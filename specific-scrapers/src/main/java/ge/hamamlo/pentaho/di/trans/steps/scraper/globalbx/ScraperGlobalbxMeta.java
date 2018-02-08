package ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx;


import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import org.pentaho.di.core.annotations.Step;

@Step(
        id = "ScraperInput2",
        name = "Globalbx Established Businesses Scraper",
        categoryDescription = "scrapers",
        image = "ge/hamamlo/pentaho/di/trans/steps/scraper/resources/globalbx.png"
)
public class ScraperGlobalbxMeta extends ScraperBaseMeta {
    public ScraperGlobalbxMeta() {
        super();
        setScraperClass(ScraperGlobalbx.class);
    }
}
