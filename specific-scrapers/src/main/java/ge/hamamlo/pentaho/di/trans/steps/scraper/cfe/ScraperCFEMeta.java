package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import org.pentaho.di.core.annotations.Step;

@Step(
        id = "CFEScraper",
        name = "Corporate Finance in Europe Scraper",
        categoryDescription = "scrapers",
        image = "ge/hamamlo/pentaho/di/trans/steps/scraper/resources/CFE.jpg"
)
public class ScraperCFEMeta extends ScraperBaseMeta {
    public ScraperCFEMeta() {
        super();
        setScraperClass(ScraperCFE.class);
    }
}
