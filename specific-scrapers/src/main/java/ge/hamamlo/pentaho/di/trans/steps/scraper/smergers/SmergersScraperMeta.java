package ge.hamamlo.pentaho.di.trans.steps.scraper.smergers;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import ge.hamamlo.pentaho.di.trans.steps.scraper.smergers.worker.SmergersScraper;
import org.pentaho.di.core.annotations.Step;

@Step(
        id = "SmergersScraper",
        name = "Smergers Busyness Scraper",
        categoryDescription = "scrapers",
        image = "ge/hamamlo/pentaho/di/trans/steps/scraper/resources/smergers.png"
)
public class SmergersScraperMeta extends ScraperBaseMeta {
    public SmergersScraperMeta() {
        super();
        setScraperClass(SmergersScraper.class);
    }
}
