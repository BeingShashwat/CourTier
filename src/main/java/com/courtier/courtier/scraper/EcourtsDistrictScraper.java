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

        log.info(
                "Executing stateless searchByCNR scrape for CNR: {}",
                cnrNumber
        );



        String body =
                "cino=" + cnrNumber +
                        "&ajax_req=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_URL))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                )
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                this.client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            log.error(
                    "eCourts returned HTTP {} for {}",
                    response.statusCode(),
                    cnrNumber
            );
            return null;
        }

        if (response.body() == null || response.body().isBlank()) {
            log.error(
                    "Empty response received for {}",
                    cnrNumber
            );
            return null;
        }

        log.error("Raw JSON response: {}", response.body());

        JsonNode json = objectMapper.readTree(response.body());

        JsonNode caseTypeNode = json.get("casetype_list");

        if (caseTypeNode == null) {
            log.error(
                    "casetype_list missing in response for {}",
                    cnrNumber
            );
            return null;
        }

        String html = caseTypeNode.asText();

        log.info("HTML length = {}", html.length());

        if (html == null || html.isBlank()) {
            log.warn(
                    "Blank casetype_list returned for {}",
                    cnrNumber
            );
            return null;
        }

        if (html.contains("This Case Code does not exists")
                || html.contains("Record not found")
                || html.contains("No Record")) {

            log.warn(
                    "Case not found for CNR {}",
                    cnrNumber
            );

            return null;
        }

        Case courtCase = htmlParser.parse(html, cnrNumber);

        if (courtCase == null) {
            log.warn(
                    "Parser returned null for {}",
                    cnrNumber
            );
            return null;
        }

        courtCase.setLastPolledAt(LocalDateTime.now());

        log.info(
                "Successfully scraped CNR {} using searchByCNR",
                cnrNumber
        );

        return courtCase;
    }
}