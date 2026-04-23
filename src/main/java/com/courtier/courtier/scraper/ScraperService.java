package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperService {

    private final EcourtsClient ecourtsClient;
    private final CaseRepository caseRepository;

    @Transactional
    public boolean scrapeAndUpdate(String cnrNumber) {
        Case courtCase = caseRepository.findByCnrNumber(cnrNumber).orElse(null);
        if (courtCase == null) {
            return false;
        }

        try {
            Document doc = ecourtsClient.fetchCaseBycnr(cnrNumber);
            log.info("Fetched case page for {} with title {}", cnrNumber, doc.title());
            return true;
        } catch (Exception e) {
            log.error("Scraping failed for {}", cnrNumber, e);
            return false;
        }
    }
}
