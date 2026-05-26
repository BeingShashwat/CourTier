package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.CaseAct;
import com.courtier.courtier.case_.entity.HearingHistory;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CaseHtmlParser {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public boolean isValidCaseDocument(Document doc) {
        // eCourts returns error text if CNR not found or captcha wrong
        String body = doc.text();
        return !body.contains("INVALID CNR")
                && !body.contains("Case Not Found")
                && !body.contains("Invalid Captcha")
                && doc.selectFirst("table.case_details_table") != null;
    }

    public void populateCase(Case courtCase, Document doc) {
        parseCaseDetails(courtCase, doc);
        parseParties(courtCase, doc);
        parseActs(courtCase, doc);
        parseHearingHistory(courtCase, doc);
        courtCase.setStatus(deriveStatus(doc));
    }

    private void parseCaseDetails(Case c, Document doc) {
        // eCourts case details are in a table with class 'case_details_table'
        Element table = doc.selectFirst("table.case_details_table");
        if (table == null) return;

        Elements rows = table.select("tr");
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 2) continue;

            String label = cells.get(0).text().trim().toLowerCase();
            String value = cells.get(1).text().trim();

            switch (label) {
                case "case type"            -> c.setCaseType(value);
                case "filing number"        -> c.setFilingNumber(value);
                case "filing date"          -> c.setFilingDate(parseDate(value));
                case "registration number"  -> c.setRegistrationNumber(value);
                case "registration date"    -> c.setRegistrationDate(parseDate(value));
                case "cnr number"           -> {}  // already set
                case "court number and judge" -> {
                    c.setCourtNumber(value);
                    c.setJudgeName(value);
                }
                case "court name"           -> c.setCourtName(value);
                case "case stage"           -> c.setCaseStage(value);
                case "next hearing date"    -> c.setNextHearingDate(parseDate(value));
            }
        }
    }

    private void parseParties(Case c, Document doc) {
        // petitioner / respondent in separate table
        Element partiesTable = doc.selectFirst("table.Petitioner_Advocate_table");
        if (partiesTable == null) return;

        Elements rows = partiesTable.select("tr");
        if (rows.size() >= 1) {
            c.setPetitionerName(rows.get(0).select("td").text().trim());
        }

        Element respTable = doc.selectFirst("table.Respondent_Advocate_table");
        if (respTable != null) {
            Elements respRows = respTable.select("tr");
            if (!respRows.isEmpty()) {
                c.setRespondentName(respRows.get(0).select("td").text().trim());
            }
        }
    }

    private void parseActs(Case c, Document doc) {
        Element actsTable = doc.selectFirst("table.acts_table");
        if (actsTable == null) return;

        List<CaseAct> acts = new ArrayList<>();
        for (Element row : actsTable.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) continue;

            CaseAct act = new CaseAct();
            act.setActName(cells.get(0).text().trim());
            act.setSection(cells.get(1).text().trim());
            act.setCourtCase(c);
            acts.add(act);
        }
        c.getActs().clear();
        c.getActs().addAll(acts);
    }

    private void parseHearingHistory(Case c, Document doc) {
        Element histTable = doc.selectFirst("table.history_table");
        if (histTable == null) return;

        List<HearingHistory> history = new ArrayList<>();
        LocalDate lastHearing = null;

        for (Element row : histTable.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 3) continue;

            HearingHistory h = new HearingHistory();
            h.setHearingDate(parseDate(cells.get(0).text().trim()));
            h.setPurpose(cells.get(1).text().trim());
            h.setJudgeName(cells.get(2).text().trim());
            h.setCourtCase(c);
            history.add(h);

            if (h.getHearingDate() != null) {
                if (lastHearing == null || h.getHearingDate().isAfter(lastHearing)) {
                    lastHearing = h.getHearingDate();
                }
            }
        }

        c.getHearingHistory().clear();
        c.getHearingHistory().addAll(history);
        c.setLastHearingDate(lastHearing);
    }

    private Case.CaseStatus deriveStatus(Document doc) {
        String text = doc.text().toLowerCase();
        if (text.contains("disposed")) return Case.CaseStatus.DISPOSED;
        if (text.contains("transferred")) return Case.CaseStatus.TRANSFERRED;
        return Case.CaseStatus.PENDING;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date: '{}'", raw);
            return null;
        }
    }
}