package ge.hamamlo.pentaho.di.trans.steps.scraper.ec;

import ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC;
import org.pentaho.di.core.annotations.Step;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;

@Step(
        id = "ScraperInput",
        name = "European Commision Projects Scraper",
        categoryDescription = "scrapers",
        image = "ge.hamamlo.pentaho.di.trans.steps.scraper.base.resources/European_Commission.png"
)
public class ScrapperECMeta extends ScraperBaseMeta {
    public ScrapperECMeta() {
        super();
        setScraperClass(ScraperEC.class);
    }
}
