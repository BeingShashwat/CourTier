package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllahabadHCScraper implements CourtScraper {

    private static final String BASE_URL =
            "https://hclko.allahabadhighcourt.in/status/index.php/";
    private static final String SEARCH_URL = BASE_URL + "get_CaseInfo";
    private static final String DETAILS_URL = BASE_URL + "get_CaseDetails";
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // maps case type string prefix to numeric ID
    // add more as discovered
    private static final java.util.Map<String, String> CASE_TYPE_MAP =
            java.util.Map.of(
                    "WPIL", "92",
                    "WP", "1",
                    "CR", "3"
            );

    private final AllahabadHCHtmlParser htmlParser;

    private static class ThreadLocalCookieHandler extends java.net.CookieHandler {
        private final ThreadLocal<java.net.CookieManager> manager = ThreadLocal.withInitial(() -> {
            java.net.CookieManager cm = new java.net.CookieManager();
            cm.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
            return cm;
        });

        @Override
        public java.util.Map<String, java.util.List<String>> get(java.net.URI uri, java.util.Map<String, java.util.List<String>> requestHeaders) throws java.io.IOException {
            return manager.get().get(uri, requestHeaders);
        }

        @Override
        public void put(java.net.URI uri, java.util.Map<String, java.util.List<String>> responseHeaders) throws java.io.IOException {
            manager.get().put(uri, responseHeaders);
        }

        public void clear() {
            manager.remove();
        }
    }

    private final ThreadLocalCookieHandler cookieHandler = new ThreadLocalCookieHandler();
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(cookieHandler)
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public boolean supports(String cnrNumber) {
        if (cnrNumber == null || cnrNumber.length() < 4) return false;
        return cnrNumber.toUpperCase().startsWith("UPHC");
    }

    @Override
    public Case scrape(String cnrNumber) throws Exception {
        // direct poll using cino — no captcha needed
        log.info("Polling Allahabad HC for CNR: {}", cnrNumber);
        try {
            return fetchDetails(this.client, cnrNumber);
        } finally {
            cookieHandler.clear();
        }
    }

    /**
     * Onboarding — search by case number when user doesn't know CNR.
     * Captcha is client-side only so we send any value.
     */
    public Case onboard(String caseType, String caseNo,
                        String caseYear) throws Exception {
        try {
            String typeId = CASE_TYPE_MAP.getOrDefault(
                    caseType.toUpperCase(), "92");

            String body = "case_type=" + typeId
                    + "&case_no=" + caseNo
                    + "&case_year=" + caseYear
                    + "&captchacode=1234"; // server never validates

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEARCH_URL))
                    .header("Content-Type",
                            "application/x-www-form-urlencoded")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://hclko.allahabadhighcourt.in/")
                    .header("Origin", "https://hclko.allahabadhighcourt.in")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = this.client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            String cnr = extractCnrFromSearchResponse(response.body());
            if (cnr == null) {
                return null; // case not found
            }

            log.info("Extracted CNR from search: {}", cnr);
            return fetchDetails(this.client, cnr);
        } finally {
            cookieHandler.clear();
        }
    }

    private Case fetchDetails(HttpClient client,
                              String cnrNumber) throws Exception {

        // Step 1 — visit page to get ci_session cookie
        HttpRequest pageRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://hclko.allahabadhighcourt.in/status/index.php/case-number"))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", "https://hclko.allahabadhighcourt.in/")
                .GET()
                .build();
        HttpResponse<String> pageResponse = client.send(
                pageRequest, HttpResponse.BodyHandlers.ofString());

        // Step 2 — now call get_CaseDetails with correct payload
        // payload matches exactly what browser sends
        String body = "cino=" + cnrNumber + "&source=&iemi=undefined";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DETAILS_URL))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://hclko.allahabadhighcourt.in/status/index.php/case-number")
                .header("Origin", "https://hclko.allahabadhighcourt.in")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.body().isBlank()) {
            log.warn("Empty response from get_CaseDetails");
            return null;
        }

        if (response.body().contains("No Record Found") ||
                response.body().contains("no record") ||
                response.body().contains("Invalid")) {
            log.warn("No record found: {}",
                    response.body().substring(0,
                            Math.min(200, response.body().length())));
            return null;
        }

        Case courtCase = htmlParser.parse(response.body(), cnrNumber);
        courtCase.setLastPolledAt(LocalDateTime.now());
        return courtCase;
    }

    private String extractCnrFromSearchResponse(String html) {
        // extract from: onclick=viewCaseData('UPHC020665842025' ,'' )
        Pattern pattern = Pattern.compile(
                "viewCaseData\\('([A-Z0-9]+)'");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("Could not extract CNR from search response");
        return null;
    }


}