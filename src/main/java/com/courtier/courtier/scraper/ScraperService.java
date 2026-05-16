package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperService {

    private final EcourtsClient ecourtsClient;
    private final CaseHtmlParser caseHtmlParser;
    private final CaseRepository caseRepository;

    @Transactional
    public boolean scrapeAndUpdate(String cnrNumber) {
        Case courtCase = caseRepository.findByCnrNumber(cnrNumber).orElse(null);
        if (courtCase == null) {
            return false;
        }

        try {
            Document doc = ecourtsClient.fetchCaseBycnr(cnrNumber);

            if (!caseHtmlParser.isValidCaseDocument(doc)) {
                log.warn("Invalid or unrecognized response for CNR: {}. " +
                        "Captcha may have failed.", cnrNumber);
                return false;
            }

            caseHtmlParser.populateCase(courtCase, doc);
            courtCase.setLastPolledAt(LocalDateTime.now());
            caseRepository.save(courtCase);

            log.info("Successfully scraped and updated CNR: {}", cnrNumber);
            return true;
        } catch (Exception e) {
            log.error("Scraping failed for CNR: {} — {}", cnrNumber, e.getMessage(), e);
            return false;
        }
    }
}
