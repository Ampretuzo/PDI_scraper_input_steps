package ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class ScraperGlobalbxTest {

    @Test
    public void run() throws IOException {
        ScraperGlobalbx scraperGlobalbx = new ScraperGlobalbx();
            scraperGlobalbx.scrapeUrl("http://www.globalbx.com/search.asp", null, output -> {
            if (output == null) {
                System.out.println("done");
                return;
            }
            System.out.println("Result: " + Arrays.toString(output));
        }, null);
    }

}