package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.common.exception.CourtierException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScraperRouter {

    private final List<CourtScraper> scrapers;

    public Case scrape(String cnrNumber) throws Exception {
        return scrapers.stream()
                .filter(s -> s.supports(cnrNumber))
                .findFirst()
                .orElseThrow(() -> new CourtierException.BadRequest(
                        "No scraper available for CNR: " + cnrNumber))
                .scrape(cnrNumber);
    }
}