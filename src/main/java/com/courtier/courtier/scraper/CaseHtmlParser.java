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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CaseHtmlParser {

    // eCourts uses formats like "29-08-2024" and "01st October 2024"
    private static final DateTimeFormatter DASH_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter ORDINAL_FORMAT =
            DateTimeFormatter.ofPattern("d['st']['nd']['rd']['th'] MMMM yyyy", Locale.ENGLISH);

    public Case parse(String html, String cnrNumber) {
        Document doc = Jsoup.parse(html);

        Case courtCase = new Case();
        courtCase.setCnrNumber(cnrNumber);
        courtCase.setStatus(Case.CaseStatus.PENDING); // default, override if disposed

        parseCaseDetails(doc, courtCase);
        parseCaseStatus(doc, courtCase);
        parsePetitionerRespondent(doc, courtCase);

        List<CaseAct> acts = parseActs(doc, courtCase);
        courtCase.setActs(acts);

        List<HearingHistory> hearings = parseHearingHistory(doc, courtCase);
        courtCase.setHearingHistory(hearings);

        extractStatelessIdentifiers(doc, courtCase);

        return courtCase;
    }

    private void parseCaseDetails(Document doc, Case courtCase) {
        Element table = doc.selectFirst("table.case_details_table");
        if (table == null) return;

        for (Element row : table.select("tr")) {
            Elements ths = row.select("th");
            Elements tds = row.select("td");
            if (ths.isEmpty() || tds.isEmpty()) continue;

            Element th = ths.first();
            Element td = tds.first();

            if (th == null || td == null) {
                continue;
            }

            String label = th.text().trim();
            String value = td.text().trim();

            switch (label) {
                case "Case Type" -> courtCase.setCaseType(value);
                case "Filing Number" -> courtCase.setFilingNumber(value.replace("\u00a0", "").trim());
                case "Filing Date" -> courtCase.setFilingDate(parseDashDate(value.trim()));
                case "Registration Number" -> courtCase.setRegistrationNumber(value.trim());
                case "Registration Date" -> courtCase.setRegistrationDate(parseDashDate(value.trim()));
            }
        }

        // court name from heading
        Element heading = doc.selectFirst("h2#chHeading");
        if (heading != null) {
            courtCase.setCourtName(heading.text().trim());
        }
    }

    private void parseCaseStatus(Document doc, Case courtCase) {
        Element table = doc.selectFirst("table.case_status_table");

//        log.error("CASE STATUS TABLE FOUND = {}", table != null);

        if (table == null) return;

        for (Element row : table.select("tr")) {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");
            if (th == null || td == null) continue;

            String label = th.text();

            String value = td.text();

            if (label == null || value == null) {
                continue;
            }

            label = label.trim();
            value = value.trim();

            switch (label) {
                case "Next Hearing Date" -> courtCase.setNextHearingDate(parseOrdinalDate(value));
                case "Case Stage" -> courtCase.setCaseStage(value);
                case "Court Number and Judge" -> {
                    // "10-A.D.J., Court No.-3" — split on first hyphen
                    String[] parts = value.split("-", 2);
                    if (parts.length == 2) {
                        courtCase.setCourtNumber(parts[0].trim());
                        courtCase.setJudgeName(parts[1].trim());
                    } else {
                        courtCase.setJudgeName(value);
                    }
                }
            }

            // detect disposed cases
            if (value.toLowerCase().contains("disposed")) {
                courtCase.setStatus(Case.CaseStatus.DISPOSED);
            }
        }
    }

    private void parsePetitionerRespondent(Document doc, Case courtCase) {
        Element petList = doc.selectFirst("ul.Petitioner_Advocate_table");
        if (petList != null) {
            List<String> names = new ArrayList<>();
            for (Element li : petList.select("li")) {
                String text = li.ownText();

                if (text != null) {
                    text = text.trim();

                    if (!text.isEmpty()) {
                        names.add(text);
                    }
                }
            }
            courtCase.setPetitionerName(String.join("; ", names));
        }

        Element respList = doc.selectFirst("ul.Respondent_Advocate_table");
        if (respList != null) {
            List<String> names = new ArrayList<>();
            for (Element li : respList.select("li")) {
                String text = li.ownText();

                if (text != null) {
                    text = text.trim();

                    if (!text.isEmpty()) {
                        names.add(text);
                    }
                }
            }
            courtCase.setRespondentName(String.join("; ", names));
        }
    }

    private List<CaseAct> parseActs(Document doc, Case courtCase) {
        List<CaseAct> acts = new ArrayList<>();
        Element table = doc.selectFirst("table.acts_table");
        if (table == null) return acts;

        // skip header row
        Elements rows = table.select("tr");
        for (int i = 1; i < rows.size(); i++) {
            Elements tds = rows.get(i).select("td");
            if (tds.size() < 2) continue;
            Element actCell = tds.get(0);
            Element sectionCell = tds.get(1);

            if (actCell == null || sectionCell == null) {
                continue;
            }

            String actName = actCell.text().trim();
            String section = sectionCell.text().trim();
            if (!actName.isEmpty()) {
                acts.add(CaseAct.builder()
                        .courtCase(courtCase)
                        .actName(actName)
                        .section(section)
                        .build());
            }
        }
        return acts;
    }

    private List<HearingHistory> parseHearingHistory(Document doc, Case courtCase) {
        List<HearingHistory> hearings = new ArrayList<>();
        Element table = doc.selectFirst("table.history_table");
        if (table == null) return hearings;

        for (Element row : table.select("tbody tr")) {
            Elements tds = row.select("td");
            if (tds.size() < 4) continue;

            Element judgeCell = tds.get(0);
            Element hearingCell = tds.get(1);
            Element nextCell = tds.get(2);
            Element purposeCell = tds.get(3);

            if (judgeCell == null || hearingCell == null ||
                    nextCell == null || purposeCell == null) {
                continue;
            }

            String judgeName = judgeCell.text().trim();
            String hearingDateStr = hearingCell.text().trim();
            String nextDateStr = nextCell.text().trim();
            String purpose = purposeCell.text().trim();

            LocalDate hearingDate = parseDashDate(hearingDateStr);
            if (hearingDate != null) {
                hearings.add(HearingHistory.builder()
                        .courtCase(courtCase)
                        .hearingDate(hearingDate)
                        .purpose(purpose)
                        .judgeName(judgeName)
                        .build());

                // track last hearing date
                if (courtCase.getLastHearingDate() == null ||
                        hearingDate.isAfter(courtCase.getLastHearingDate())) {
                    courtCase.setLastHearingDate(hearingDate);
                }
            }
        }
        return hearings;
    }

    // parses "29-08-2024"
    private LocalDate parseDashDate(String value) {
        if (value == null || value.isBlank() || value.equals("-")) return null;
        try {
            return LocalDate.parse(value.trim(), DASH_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse dash date: '{}'", value);
            return null;
        }
    }

    // parses "01st October 2024", "20th July 2026"
    private LocalDate parseOrdinalDate(String value) {
        if (value == null || value.isBlank() || value.equals("---")) return null;
        try {
            // strip ordinal suffix: "01st" → "01", "20th" → "20"
            String cleaned = value.replaceAll("(\\d+)(st|nd|rd|th)", "$1");
            return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException e) {
            log.warn("Could not parse ordinal date: '{}'", value);
            return null;
        }
    }

    // Call this inside the main `parse()` method
    public void extractStatelessIdentifiers(Document doc, Case courtCase) {
        // Grab the raw HTML string
        String html = doc.outerHtml();

        // Regex to find any of the common eCourts data functions and capture everything inside the parentheses
        Pattern functionPattern = Pattern.compile("(?:viewHistory|viewBusiness|viewDailyOrder)\\s*\\(([^)]+)\\)");
        Matcher functionMatcher = functionPattern.matcher(html);

        if (functionMatcher.find()) {
            String argsString = functionMatcher.group(1);

            // Regex to safely extract all arguments wrapped in single quotes: 'arg1', 'arg2'
            Pattern argPattern = Pattern.compile("'([^']*)'");
            Matcher argMatcher = argPattern.matcher(argsString);

            List<String> args = new ArrayList<>();
            while (argMatcher.find()) {
                args.add(argMatcher.group(1));
            }

            // eCourts generally passes 11 arguments to these functions:
            // 0: court_code, 1: dist_code, 2: nextdate, 3: cnr, 4: state_code, 5: status,
            // 6: business_date, 7: court_no, 8: national_code, 9: search_type, 10: srno
            if (args.size() >= 11) {
                courtCase.setCourtCode(args.get(0));
                courtCase.setDistCode(args.get(1));
                courtCase.setCaseNumber1(args.get(3)); // This is the internal case number format
                courtCase.setStateCode(args.get(4));
                courtCase.setBusinessDate(args.get(6));
                courtCase.setCourtNo(args.get(7));
                courtCase.setNationalCourtCode(args.get(8));
                courtCase.setSrNo(args.get(10));

                log.info("Extracted stateless polling identifiers for CNR: {}", courtCase.getCnrNumber());
                return; // Success!
            }
        }

        log.warn("Could not find stateless identifiers in HTML for CNR: {}. Polling will be disabled.", courtCase.getCnrNumber());
    }
}