package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import java.io.IOException;

/**
 * This is the main interface to implement when new types of scrapers are needed, other classes need not be touched.<br>
 * Implementations are required to have a no-arg ctor!
 */
public interface Scraper {
    void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperBase.ScraperOutput scraperOutput) throws IOException;
}
