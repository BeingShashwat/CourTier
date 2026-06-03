package com.courtier.courtier.scraper;

import com.courtier.courtier.case_.entity.Case;

public interface CourtScraper {
    Case scrape(String cnrNumber) throws Exception;
    boolean supports(String cnrNumber);
}