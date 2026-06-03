package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.CaseAct;
import com.courtier.courtier.case_.entity.HearingHistory;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class AllahabadHCHtmlParser {

    private static final DateTimeFormatter DASH_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Case parse(String html, String cnrNumber) {
        Document doc = Jsoup.parse(html);

        Case courtCase = new Case();
        courtCase.setCnrNumber(cnrNumber);
        courtCase.setCourtName("Allahabad High Court");
        courtCase.setStatus(Case.CaseStatus.PENDING);

        // status — check for DISPOSED in heading
        Element statusEl = doc.selectFirst("h3.text-success, h3.text-danger");
        if (statusEl != null) {
            String statusText = statusEl.text().trim().toUpperCase();
            if (statusText.contains("DISPOSED")) {
                courtCase.setStatus(Case.CaseStatus.DISPOSED);
            }
        }

        // case type from heading
        Element caseHeading = doc.selectFirst("h3.text-center strong");
        if (caseHeading != null) {
            courtCase.setCaseType(caseHeading.text().trim());
        }

        // table-case — filing number, filing date, CNR, registration date
        Element caseTable = doc.selectFirst("table.table-case");
        if (caseTable != null) {
            for (Element row : caseTable.select("tr")) {
                Element th = row.selectFirst("th");
                Elements tds = row.select("td");
                if (th == null || tds.isEmpty()) continue;

                String label = th.text().trim();
                switch (label) {
                    case "Filing No." -> {
                        courtCase.setFilingNumber(tds.get(0).text().trim());
                        if (tds.size() > 1) {
                            String filingDateText = tds.get(1).text()
                                    .replace("Filing Date :", "").trim();
                            courtCase.setFilingDate(parseDashDate(filingDateText));
                        }
                    }
                    case "CNR" -> {
                        if (tds.size() > 1) {
                            String regDateText = tds.get(1).text()
                                    .replace("Date of Registration :", "").trim();
                            courtCase.setRegistrationDate(parseDashDate(regDateText));
                        }
                    }
                }
            }
        }

        // table-red — case status details
        Element statusTable = doc.selectFirst("table.table-red");
        if (statusTable != null) {
            for (Element row : statusTable.select("tr")) {
                Element th = row.selectFirst("th");
                Element td = row.selectFirst("td");
                if (th == null || td == null) continue;

                String label = th.text().trim();
                String value = td.text().trim();

                switch (label) {
                    case "Next Hearing Date" -> courtCase.setNextHearingDate(
                            parseOrdinalDate(value));
                    case "Stage of Case" -> courtCase.setCaseStage(value);
                    case "Coram" -> courtCase.setJudgeName(value);
                    case "Bench Type" -> courtCase.setCourtNumber(value);
                }
            }
        }

        // petitioner / respondent — table-adv
        Element advTable = doc.selectFirst("table.table-adv tbody tr");
        if (advTable != null) {
            Elements cells = advTable.select("td");
            if (cells.size() >= 2) {
                courtCase.setPetitionerName(
                        extractPartyNames(cells.get(0)));
                courtCase.setRespondentName(
                        extractPartyNames(cells.get(1)));
            }
        }

        // acts — table-acts
        List<CaseAct> acts = new ArrayList<>();
        Element actsTable = doc.selectFirst("table.table-acts");
        if (actsTable != null) {
            Elements rows = actsTable.select("tr");
            for (int i = 1; i < rows.size(); i++) {
                Elements tds = rows.get(i).select("td");
                if (tds.size() < 2) continue;
                String actName = tds.get(0).text().trim();
                String section = tds.get(1).text().trim();
                if (!actName.isEmpty() && !actName.equals("Mandamus/Act not Mentioned")) {
                    acts.add(CaseAct.builder()
                            .courtCase(courtCase)
                            .actName(actName)
                            .section(section)
                            .build());
                }
            }
        }
        courtCase.setActs(acts);

        // listing history — table-hist (maps to HearingHistory)
        List<HearingHistory> hearings = new ArrayList<>();
        Element histTable = doc.selectFirst("table.table-hist");
        if (histTable != null) {
            for (Element row : histTable.select("tbody tr")) {
                Elements tds = row.select("td");
                if (tds.size() < 4) continue;

                String purpose = tds.get(0).text().trim();
                String judgeName = tds.get(1).text()
                        .replaceAll("Bench ID : \\d+", "").trim();
                String dateStr = tds.get(3).text().trim();
                String shortOrder = tds.size() > 4 ? tds.get(4).text().trim() : "";

                if (!shortOrder.isEmpty()) {
                    purpose = purpose + " — " + shortOrder;
                }

                LocalDate hearingDate = parseDashDate(dateStr);
                if (hearingDate != null) {
                    hearings.add(HearingHistory.builder()
                            .courtCase(courtCase)
                            .hearingDate(hearingDate)
                            .purpose(purpose)
                            .judgeName(judgeName)
                            .build());

                    if (courtCase.getLastHearingDate() == null ||
                            hearingDate.isAfter(courtCase.getLastHearingDate())) {
                        courtCase.setLastHearingDate(hearingDate);
                    }
                }
            }
        }
        courtCase.setHearingHistory(hearings);

        return courtCase;
    }

    private String extractPartyNames(Element cell) {
        // get text but stop before "Advocate -"
        String fullText = cell.text();
        int advocateIdx = fullText.indexOf("Advocate -");
        if (advocateIdx > 0) {
            fullText = fullText.substring(0, advocateIdx).trim();
        }
        return fullText.replaceAll("\\d+\\.", "").trim();
    }

    private LocalDate parseDashDate(String value) {
        if (value == null || value.isBlank() || value.equals("---")
                || value.equals("-")) return null;
        try {
            return LocalDate.parse(value.trim(), DASH_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date: '{}'", value);
            return null;
        }
    }

    private LocalDate parseOrdinalDate(String value) {
        if (value == null || value.isBlank() || value.equals("---")) return null;
        try {
            String cleaned = value.replaceAll("(\\d+)(st|nd|rd|th)", "$1");
            return LocalDate.parse(cleaned.trim(),
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException e) {
            log.warn("Could not parse ordinal date: '{}'", value);
            return null;
        }
    }
}