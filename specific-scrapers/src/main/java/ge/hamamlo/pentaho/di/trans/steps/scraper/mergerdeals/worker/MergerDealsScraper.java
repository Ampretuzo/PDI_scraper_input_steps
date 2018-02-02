package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.createAllStringFieldDefs;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.getIndexForFieldName;

public class MergerDealsScraper implements Scraper {
    static String[] fields = {
            "pro_name",
            "url",
            "ori_industry_1",
            "location",
            "descrip_1",
            "descrip_2",
            "descrip_3",
            "descrip_4",
            "descrip_5",
            "descrip_6",

            "turnover_ori_1",
            "turnover_ori_2",
            "turnover_ori_3",
            "turnover_ori_4",
            "turnover_type_1",
            "turnover_type_2",
            "turnover_type_3",
            "turnover_type_4",
            "turnover_year_1",
            "turnover_year_2",
            "turnover_year_3",
            "turnover_year_4",

            "gross_profit_type_1",
            "gross_profit_type_2",
            "gross_profit_type_3",
            "gross_profit_type_4",
            "gross_profit_ori_1",
            "gross_profit_ori_2",
            "gross_profit_ori_3",
            "gross_profit_ori_4",
            "gross_profit_year_1",
            "gross_profit_year_2",
            "gross_profit_year_3",
            "gross_profit_year_4",

            "adj_ebidta_type_1",
            "adj_ebidta_type_2",
            "adj_ebidta_type_3",
            "adj_ebidta_type_4",
            "adj_ebidta_ori_1",
            "adj_ebidta_ori_2",
            "adj_ebidta_ori_3",
            "adj_ebidta_ori_4",
            "adj_ebidta_year_1",
            "adj_ebidta_year_2",
            "adj_ebidta_year_3",
            "adj_ebidta_year_4",

            "timeline_insert",
            "requirement_type",
            "language"
    };

    @Override
    public FieldDef[] fields() {
        FieldDef[] allStringFieldDefs = createAllStringFieldDefs(fields);
        allStringFieldDefs[getIndexForFieldName("timeline_insert", fields) ].setFieldType(FieldDef.FieldType.DATE);
        return allStringFieldDefs;
    }

    @Override
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperBase.ScraperOutput scraperOutput) throws IOException {
        int pageNumber = 1;
        while (true) {
            System.out.println("doing page #: " + pageNumber);
            // download the page
            String pagePath = "/page" + pageNumber;
            Document doc = Jsoup.connect(url + pagePath)
                    .userAgent("Mozilla")
                    .timeout(100000) // this website is very slow, hence longer timeout
                    .get();
            pageNumber++;

            // if this is the last page - stop
            if (noMoreBusynessesOnPage(doc) /*|| pageNumber == 3 *//*limit page number for testing*/ ) {
                scraperOutput.yield(null);
                System.out.println("done at page #: " + --pageNumber);
                return;
            }

            Element listingsResults = doc.getElementById("listings_results");
            int nBusynesses = listingsResults.children().size() - 1;
            CountDownLatch waitForAllBusynessesToFinish = new CountDownLatch(nBusynesses);
            List<Object[]> resultsOnPage = new ArrayList<> ();
            List<AtomicBoolean> successful = new ArrayList<>(); // worker thread will set this to false if there was a problem processing the page
            for (int i = 1; i < nBusynesses + 1; i++) {
                // first, take care of the fields that are right on the listings page
                Element section = listingsResults.children().get(i);
                String busyness_url = section.child(0).attr("href");
                String pro_name = section.getElementsByTag("h1").get(0).ownText();
                // ori industry might not be present:
                String oriIndustry = null;
                if (section.getElementsByTag("h1").get(0).children().size() > 0) {
                    oriIndustry = section.getElementsByTag("h1").get(0).child(0).text();
                }
                String location = section.getElementById("location").text();

                Object[] result = new Object[fields.length];
                result[getIndexForFieldName("url", fields) ] = busyness_url;
                result[getIndexForFieldName("pro_name", fields) ] = pro_name;
                result[getIndexForFieldName("ori_industry_1", fields) ] = oriIndustry;
                result[getIndexForFieldName("location", fields) ] = location;

                // constants
                result[getIndexForFieldName("timeline_insert", fields) ] = new Date();
                result[getIndexForFieldName("requirement_type", fields) ] = "sell";
                result[getIndexForFieldName("language", fields) ] = "en";

                resultsOnPage.add(result);
                AtomicBoolean successfulParse = new AtomicBoolean(false);
                successful.add(successfulParse);

                MergerDealsScraperWorker worker = new MergerDealsScraperWorker(waitForAllBusynessesToFinish, result, fields, busyness_url, logger, successfulParse);
                new Thread(worker).start();
            }

            try {
                waitForAllBusynessesToFinish.await();
            } catch (InterruptedException e) {
                logger.logBasic("Interrupted while waiting for results of page #: " + pageNumber);
                scraperOutput.yield(null);
            }

            for (int i = 0; i < resultsOnPage.size(); i++) {
                if (!successful.get(i).get() ) continue;
                scraperOutput.yield(resultsOnPage.get(i) );
            }

        }
    }

    private boolean noMoreBusynessesOnPage(Document doc) {
        // only paging numbers left
        return doc.getElementById("listings_results").children().size() == 1;
    }
}
