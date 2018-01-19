package org.pentaho.di.trans.steps.web_scrape;

public interface ScraperWorker {
    String scrapeUrl(String url, Scraper.LoggerForScraper logger);
}
