package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker.MergerDealsScraper;
import org.pentaho.di.core.annotations.Step;

@Step(
        id = "MergerDealsScraper",
        name = "MergerDeals Scraper",
        categoryDescription = "scrapers",
        image = "ge/hamamlo/pentaho/di/trans/steps/scraper/resources/merge-512.png"
)
public class MergerDealsMeta extends ScraperBaseMeta {
    public MergerDealsMeta() {
        super();
        setScraperClass(MergerDealsScraper.class);
    }
}
