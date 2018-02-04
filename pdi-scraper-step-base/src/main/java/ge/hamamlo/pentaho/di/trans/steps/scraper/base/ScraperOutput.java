package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

/**
 * Used as an output interface for {@link Scraper}.
 */
public interface ScraperOutput {
    /**
     * Feed output through this interface, pass null when done.
     */
    void yield(Object[] output);
}
