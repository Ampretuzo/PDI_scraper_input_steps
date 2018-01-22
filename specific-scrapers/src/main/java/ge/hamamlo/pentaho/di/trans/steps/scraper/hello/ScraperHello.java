package ge.hamamlo.pentaho.di.trans.steps.scraper.hello;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import org.pentaho.di.core.annotations.Step;

import java.io.IOException;

@Step(
        id = "ScraperHello",
        name = "Hello World Scraper Example",
        categoryDescription = "scrapers",
        image = "ge/hamamlo/pentaho/di/trans/steps/scraper/resources/helloworld.png"
)
public class ScraperHello extends ScraperBaseMeta implements Scraper {
    public ScraperHello() {
        super();
        setScraperClass(this.getClass() );
    }

    @Override
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperBase.ScraperOutput output) throws IOException {
        logger.logBasic("This demonstrational step returns Hello World every time scraping is requested!");
        output.yield("Hello World!");
        output.yield(null);
    }
}
