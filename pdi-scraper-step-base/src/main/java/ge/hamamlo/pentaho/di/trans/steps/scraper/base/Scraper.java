package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import java.io.IOException;

/**
 * This is the main interface to implement when new types of scrapers are needed, other classes need not be touched.<br>
 * Implementations are required to have a no-arg ctor!
 */
public interface Scraper {
    /**
     * Specify fields which will be generating when scraping.
     */
    FieldDef[] fields();
    /**
     * Scrape whatever necessary from given url and feed results to {@code scraperOutput::yield}.
     */
    void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput, ScraperInformation scraperInformation) throws IOException;
}
