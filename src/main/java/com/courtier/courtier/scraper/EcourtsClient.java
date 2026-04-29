package com.courtier.courtier.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EcourtsClient {

    private static final String BASE_URL = "https://services.ecourts.gov.in/ecourtindia_v6/";
    private static final String CAPTCHA_URL = BASE_URL + "vendor/securimage/securimage_show.php";
    private static final String CASE_URL = BASE_URL + "?p=home/viewCase&state_code=null&dist_code=null&court_code=null";

    private static final int TIMEOUT_MS = 15_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36";

    private final CaptchaSolver captchaSolver;

    public Document fetchCaseBycnr(String cnrNumber) throws Exception {
        Map<String, String> cookies = new HashMap<>();

        Connection.Response sessionResp = Jsoup.connect(BASE_URL)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        cookies.putAll(sessionResp.cookies());
        log.debug("Session established, cookies: {}", cookies.keySet());

        Connection.Response captchaResp = Jsoup.connect(CAPTCHA_URL)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .cookies(cookies)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .execute();

        cookies.putAll(captchaResp.cookies());

        byte[] captchaBytes = captchaResp.bodyAsBytes();
        String captchaSolution = captchaSolver.solve(new ByteArrayInputStream(captchaBytes));
        log.debug("Captcha solved: '{}'", captchaSolution);

        Connection.Response caseResp = Jsoup.connect(CASE_URL)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .cookies(cookies)
                .method(Connection.Method.POST)
                .data("cino", cnrNumber)
                .data("captcha_code", captchaSolution)
                .data("ajax_req", "true")
                .execute();

        Document doc = caseResp.parse();
        log.debug("Case page fetched for CNR: {}", cnrNumber);
        return doc;
    }
}
