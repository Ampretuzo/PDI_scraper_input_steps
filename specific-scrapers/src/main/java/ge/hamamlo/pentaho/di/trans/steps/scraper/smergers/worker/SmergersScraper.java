package ge.hamamlo.pentaho.di.trans.steps.scraper.smergers.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.createAllStringFieldDefs;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.getIndexForFieldName;

public class SmergersScraper implements Scraper {
    private static String[] fields = {
            "url",
            "country",
            "location",
            "pro_name",
            "price_ori",
            "trade_motivation",
            "fixed_asset",
            "sales",
            "ebitda_margin",
            "ori_industry",
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
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperBase.ScraperOutput scraperOutput) throws IOException {
        URL websiteUrl = new URL(url);
        int pageNumber = 1;
        while (true) {
            String pageQuery = "&page=" + pageNumber;
            Document doc = Jsoup.connect(url + pageQuery)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
            pageNumber++;
            if (noMoreBusynessesOnPage(doc) ) {
                scraperOutput.yield(null);
                return;
            }
            Elements listItemsElements = doc.body().getElementsByClass("listing-item-wrapper");
            for (int i = 0; i < listItemsElements.size(); i++) {
                Element listingItemWrapper = listItemsElements.get(i);
                String busynessPageRelative = ((Element) listingItemWrapper.getElementsByTag("a").get(0) ).attr("href");
                String busynessPageUrl = "http://" +  websiteUrl.getHost() + busynessPageRelative;  // it's ok to use insecure protocol
                SmergersScraperWorker smergersScraperWorker;    // TODO: this comes later
                scrapeBusyness(busynessPageUrl);
            }
        }
    }

    private void scrapeBusyness(String busynessPageUrl) throws IOException {
        Document doc = Jsoup.connect(busynessPageUrl)
                .userAgent("Mozilla")
                .timeout(3000)
                .get();
        String proName = getProjectName(doc.body() );

        String country = getCountry(doc.body() );

        String oriPrice = getOriPrice(doc.body() );

        String tradeMotivation = getTradeMotivation(doc.body() );

        String fixedAsset = getFixedAsset(doc.body() );

        String runRateSales = getRunRateSales(doc.body() );
        if (runRateSales.equalsIgnoreCase("nil") ) runRateSales = null;

        String ebidtaMargin = getEbidtaMargin(doc.body() );

        List<String> oriIndustries = getOriIndustries(doc.body() );

        String location = getLocation(doc.body() );

        String publisherType = getPublisherType(doc.body() );

        String projectStatus = getProjectStatus(doc.body() );

        String descrip = getDescrip(doc.body() );

        // print to test
        System.out.println(
                proName + "\n" +
                        busynessPageUrl + "\n" +
                        country + "\n" +
                        oriPrice + "\n" +
                        tradeMotivation + "\n" +
                        fixedAsset + "\n" +
                        runRateSales + "\n" +
                        ebidtaMargin + "\n" +
                        oriIndustries + "\n" +
                        location + "\n" +
                        publisherType + "\n" +
                        projectStatus + "\n" +
                        descrip
        );

        System.out.println("\n");
    }

    private String getDescrip(Element body) {
        return body.getElementsByClass("business-description").text();
    }

    private String getProjectStatus(Element body) {
        Element keyFactsContainer = getKeyFactsContainer(body);
        return keyFactsContainer.child(10).child(1).text();
    }

    private String getPublisherType(Element body) {
        Element keyFactsContainer = getKeyFactsContainer(body);
        return keyFactsContainer.child(9).child(1).text();
    }

    private String getLocation(Element body) {
        Element keyFactsContainer = getKeyFactsContainer(body);
        return keyFactsContainer.child(7).child(1).text();
    }

    private List<String> getOriIndustries(Element body) {
        Element keyFactsContainer = getKeyFactsContainer(body);
        return Arrays.asList(keyFactsContainer.child(6).child(1).child(0).attr("data-title").split(", ") );
    }

    private String getEbidtaMargin(Element body) {
        Element keyFactsContainer = getKeyFactsContainer(body);
        return keyFactsContainer.child(5).child(1).text();
    }

    private String getRunRateSales(Element body) {
        Element keyFacts = getKeyFactsContainer(body);
        return keyFacts.child(4).child(1).text();
    }

    private Element getKeyFactsContainer(Element body) {
        return body.getElementsByClass("business-key-facts").get(0);
    }

    private String getFixedAsset(Element body) {
        Element container = body.getElementsByClass("transaction-reason").get(1);
        String currency = container.getElementsByClass("currency-label").get(0).text();
        String askingPrice = container.getElementsByClass("asking-price").get(0).text();
        return currency + " " + askingPrice;
    }

    private String getTradeMotivation(Element body) {
        return body.getElementsByClass("transaction-reason").get(0).text();
    }

    private String getOriPrice(Element body) {
        Element container = body.getElementsByClass("transaction-one-line").get(0);
        String currency = container.getElementsByClass("currency-label").get(0).text();
        String askingPrice = container.getElementsByClass("asking-price").get(0).text();
        return currency + " " + askingPrice;
    }

    private String getCountry(Element body) {
        return body.getElementsByClass("profile-breadcrumbs").get(0).getElementsByAttributeValue("itemprop", "name").get(1).text();
    }

    private String getProjectName(Element body) {
        return body.getElementsByClass("single-line-description").get(0).text();
    }

    private boolean noMoreBusynessesOnPage(Document doc) {
        Elements listItemsElements = doc.body().getElementsByClass("listing-item-wrapper");
        return listItemsElements.size() == 0;
    }
}
