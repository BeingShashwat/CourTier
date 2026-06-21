package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EcourtsDistrictScraper implements CourtScraper {

    private static final String BASE_URL =
            "https://services.ecourts.gov.in/ecourtindia_v6/";

    private static final String SEARCH_URL =
            BASE_URL + "?p=cnr_status/searchByCNR/";

    private final CaseHtmlParser htmlParser;
    private final ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public boolean supports(String cnrNumber) {
        if (cnrNumber == null || cnrNumber.length() < 4) {
            return false;
        }

        return !cnrNumber.substring(0, 4)
                .toUpperCase()
                .endsWith("HC");
    }

    @Override
    public Case scrape(String cnrNumber) throws Exception {

        log.info("Scraping district case: {}", cnrNumber);

        String body =
                "cino=" + cnrNumber +
                        "&ajax_req=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_URL))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/137.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Origin", "https://services.ecourts.gov.in")
                .header("Referer",
                        "https://services.ecourts.gov.in/ecourtindia_v6/")
                .header("Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;

        try {

            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

        } catch (Exception e) {

            log.error("District scraper request failed for {}", cnrNumber, e);

            throw e;
        }

        if (response.statusCode() != 200) {

            log.error(
                    "District court returned HTTP {} for CNR {}",
                    response.statusCode(),
                    cnrNumber
            );

            return null;
        }

        if (response.body() == null || response.body().isBlank()) {

            log.error(
                    "Empty response received for district case {}",
                    cnrNumber
            );

            return null;
        }

        JsonNode json;

        try {

            json = objectMapper.readTree(response.body());

        } catch (Exception e) {

            log.error(
                    "Failed to parse eCourts JSON for {}",
                    cnrNumber,
                    e
            );

            throw e;
        }

        JsonNode caseTypeNode = json.get("casetype_list");

        if (caseTypeNode == null) {

            log.error(
                    "casetype_list missing for district case {}",
                    cnrNumber
            );

            return null;
        }

        String html = caseTypeNode.asText();

        if (html.isBlank()) {

            log.warn(
                    "Blank HTML returned for district case {}",
                    cnrNumber
            );

            return null;
        }

        if (html.contains("This Case Code does not exists")
                || html.contains("Record not found")
                || html.contains("No Record")) {

            log.warn("District case not found: {}", cnrNumber);

            return null;
        }

        Case courtCase;

        try {

            courtCase = htmlParser.parse(html, cnrNumber);

        } catch (Exception e) {

            log.error(
                    "Failed to parse district court HTML for {}",
                    cnrNumber,
                    e
            );

            throw e;
        }

        if (courtCase == null) {

            log.warn(
                    "HTML parser returned null for district case {}",
                    cnrNumber
            );

            return null;
        }

        courtCase.setLastPolledAt(LocalDateTime.now());

        log.info(
                "Successfully scraped district case {}",
                cnrNumber
        );

        return courtCase;
    }
}