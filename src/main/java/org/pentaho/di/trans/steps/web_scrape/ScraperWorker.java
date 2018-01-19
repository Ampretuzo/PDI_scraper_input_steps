package org.pentaho.di.trans.steps.web_scrape;

/**
 * This is the only interface to implement when new types of scrapers are needed, other classes need not be touched.<br>
 * Implementations are required to have a no-arg ctor!
 */
public interface ScraperWorker {
    String scrapeUrl(String url, Scraper.LoggerForScraper logger);
}
