package com.courtier.courtier.scraper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EcourtsClient {

    private static final String BASE_URL = "https://services.ecourts.gov.in/ecourtindia_v6/";
    private static final String CASE_URL = BASE_URL + "?p=home/viewCase&state_code=null&dist_code=null&court_code=null";

    public Document fetchCaseBycnr(String cnrNumber) throws Exception {
        log.debug("Trying simple case fetch for {}", cnrNumber);
        return Jsoup.connect(CASE_URL)
                .data("cino", cnrNumber)
                .post();
    }
}
