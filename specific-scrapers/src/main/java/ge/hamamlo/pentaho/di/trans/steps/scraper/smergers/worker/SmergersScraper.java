package ge.hamamlo.pentaho.di.trans.steps.scraper.smergers.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.createAllStringFieldDefs;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.getIndexForFieldName;

/**
 * does not support skipping already processed urls
 */
public class SmergersScraper implements Scraper {
    static String[] fields = {
            "url",
            "country",
            "location",
            "pro_name",
            "price_ori",
            "trade_motivation",
            "fixed_asset",
            "sales",
            "ebitda_margin",
            "ori_industry_1",
            "ori_industry_2",
            "ori_industry_3",
            "ori_industry_4",
            "ori_industry_5",
            "publisher_type",
            "project_status",
            "descrip",
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
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput, ScraperInformation scraperInformation) throws IOException {
        URL websiteUrl = new URL(url);
        // setting dollar as session currency
        String sessionId = Jsoup.connect("http://" + websiteUrl.getHost() + "/ajax/set_currency/?id=8")
                .method(Connection.Method.GET)
                .execute()
                .cookie("sessionid");
        int pageNumber = 1;
        while (true) {
            // download the page
            String pageQuery = "&page=" + pageNumber;
            Document doc = Jsoup.connect(url + pageQuery)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .cookie("sessionid", sessionId)
                    .get();
            pageNumber++;

            // if this is the last page - stop
            if (noMoreBusynessesOnPage(doc) ) {
                scraperOutput.yield(null);
                return;
            }

            int nBusynesses = getNumberOfBusynessesOnPage(doc.body() );
            CountDownLatch untilAllBusynessesDone = new CountDownLatch(nBusynesses);
            List<Object[] > results = new ArrayList<>();

            for (int i = 0; i < nBusynesses; i++) {
                Object[] result = new Object[fields.length];
                results.add(result);
                SmergersScraperWorker smergersScraperWorker = new SmergersScraperWorker(
                        result,
                        getBusynessPageUrl(doc.body(), i, websiteUrl.getHost() ),
                        untilAllBusynessesDone,
                        logger,
                        sessionId
                );
                Thread thread = new Thread(smergersScraperWorker);
                thread.start();
            }
            try {
                untilAllBusynessesDone.await();
            } catch (InterruptedException e) {
                // just ignore and move on to the next page
                logger.logBasic("Interrupted when waiting for page no. " + pageNumber);
                continue;
            }
            for (Object[] result : results) {
                if (result == null) continue;
                scraperOutput.yield(result);
            }
        }
    }

    private int getNumberOfBusynessesOnPage(Element body) {
        return body.getElementsByClass("listing-item-wrapper").size();
    }

    private String getBusynessPageUrl(Element body, int idx, String host) {
        Element listingItemWrapper = body.getElementsByClass("listing-item-wrapper").get(idx);
        String busynessPageRelative = ((Element) listingItemWrapper.getElementsByTag("a").get(0) ).attr("href");
        // it's ok to use insecure protocol
        return "http://" +  host + busynessPageRelative;
    }

    private boolean noMoreBusynessesOnPage(Document doc) {
        Elements listItemsElements = doc.body().getElementsByClass("listing-item-wrapper");
        return listItemsElements.size() == 0;
    }
}
