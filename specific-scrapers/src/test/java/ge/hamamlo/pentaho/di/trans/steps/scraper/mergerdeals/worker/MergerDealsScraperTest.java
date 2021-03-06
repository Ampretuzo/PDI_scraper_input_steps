package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperInformationDisabled;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class MergerDealsScraperTest {
    @Test
    public void main() throws IOException {
        Scraper scraper = new MergerDealsScraper();
        scraper.scrapeUrl("http://www.mergerdeals.com/listings/Turnover/10m-25m/25m-50m/50m-plus/",
                null,
                output -> {
                    if (output == null) {
                        System.out.println("done!");
                        return;
                    }
                    System.out.println(Arrays.toString(output));
                },
                new ScraperInformationDisabled()
        );
    }

}