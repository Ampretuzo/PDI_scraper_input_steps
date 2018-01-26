package ge.hamamlo.pentaho.di.trans.steps.scraper.smergers.worker;

import org.junit.Test;

import java.util.Arrays;

/*
 * NOTE: this is not a real test! It is there just to spare Spoon deployment time by doing System.out.println() !
 */

public class SmergersScraperTest {
    @Test
    public void scrapeUrl() throws Exception {
        SmergersScraper smergersScraper = new SmergersScraper();
        smergersScraper.scrapeUrl(
                "https://www.smergers.com/businesses/businesses-for-sale/s0/c0/t2/?deal_size_gte=5000000&page=1&deal_size_lte=1000000000",
                null,
                output -> {
                    if (output == null) {
                        System.out.println("done!");
                        return;
                    }
                    System.out.println("Result: " + Arrays.toString(output) );
                }
        );
    }

}