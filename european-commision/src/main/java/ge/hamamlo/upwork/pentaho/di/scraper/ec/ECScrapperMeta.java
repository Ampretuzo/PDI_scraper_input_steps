package ge.hamamlo.upwork.pentaho.di.scraper.ec;

import ge.hamamlo.upwork.pentaho.di.scraper.ec.worker.ScraperWorkerImpl;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.trans.steps.web_scrape.ScraperMeta;

@Step(
        id = "ScraperInput",
        name = "placeholder",
        categoryDescription = "custom",
        image = "org/pentaho/di/trans/steps/web_scrape/resources/kafka_consumer.png"
)
public class ECScrapperMeta extends ScraperMeta {
    public ECScrapperMeta() {
        super();
        setScraperWorkerClass(ScraperWorkerImpl.class);
    }
}
