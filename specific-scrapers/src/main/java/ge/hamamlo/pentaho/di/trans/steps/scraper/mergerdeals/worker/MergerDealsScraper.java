package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

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
        URL websiteUrl = new URL(url);
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
            if (noMoreBusynessesOnPage(doc) ) {
                scraperOutput.yield(null);
                System.out.println("done at page #: " + --pageNumber);
                return;
            }

            Element listingsResults = doc.getElementById("listings_results");
            for (int i = 1; i < listingsResults.children().size(); i++) {
                // first, take care of the fields that are right on the page
                Element section = listingsResults.children().get(i);
                String busyness_url = section.child(0).attr("href");
                String pro_name = section.getElementsByTag("h1").get(0).ownText();
                String oriIndustry = section.getElementsByTag("h1").get(0).child(0).text();
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


                scrapeBusynessPage(result, busyness_url);

                scraperOutput.yield(result);
            }

        }
    }

    private void scrapeBusynessPage(Object[] result, String busyness_url) throws IOException {
        Document doc = Jsoup.connect(busyness_url)
                .userAgent("Mozilla")
                .timeout(100000) // this website is very slow, hence longer timeout
                .get();

        Element list = doc.body().getElementById("list");
        String descrip1 = list.getElementsByTag("p").get(0).ownText();
        Element columns = list.getElementById("columns");
        String descrip2 = columns.getElementsByClass("col1").get(0).text();
        String descrip3 = columns.getElementsByClass("col2").get(0).text();
        String descrip4 = columns.getElementsByClass("col3").get(0).text();

        Element key = doc.body().getElementById("key");
        String descrip5 = null;
        if (key.children().size() == 2) {
            descrip5 = key.child(1).text();  // footnote div
        }

        result[getIndexForFieldName("descrip_1", fields) ] = descrip1;
        result[getIndexForFieldName("descrip_2", fields) ] = descrip2;
        result[getIndexForFieldName("descrip_3", fields) ] = descrip3;
        result[getIndexForFieldName("descrip_4", fields) ] = descrip4;
        result[getIndexForFieldName("descrip_5", fields) ] = descrip5;

        // there might be a buggy table, hence if statement to handle those differently.
        // see: http://www.mergerdeals.com/listings/VEN060/
        if (key.child(0).tagName().equalsIgnoreCase("table") ) {
            Element table = key.child(0).child(0);
            parseTurnover(result, table);
            parseGrossProfit(result, table);
            parseAdjEbidta(result, table);
        } else {
            // TODO: this is a special case which happens e.g. when two companies are for sale simultaneously.
            // they will be presented by a text
            result[getIndexForFieldName("turnover_ori_1", fields) ] = key.text();   // fall back to this
        }
    }

    private void parseAdjEbidta(Object[] result, Element table) {
        for (int i = 0; i < table.child(0).children().size() - 1; i++) {
            String year = getYear(table, i);
            String type = getType(table, i);
            String currency = getCurrency(table, i);
            Integer adjEbidtaAmount = getValue(table, i, 3);
            result[getIndexForFieldName("adj_ebidta_year_" + (i + 1), fields) ] = year;
            result[getIndexForFieldName("adj_ebidta_type_" + (i + 1), fields) ] = type;
            result[getIndexForFieldName("adj_ebidta_ori_" + (i + 1), fields) ] =
                    adjEbidtaAmount != null ? currency + (adjEbidtaAmount * 1000) : getValueString(table, i, 3);
        }
    }

    private String getValueString(Element table, int i, int j) {
        String stringValue = table.child(j).child(i + 1).ownText();
        return stringValue;
    }

    private void parseGrossProfit(Object[] result, Element table) {
        for (int i = 0; i < table.child(0).children().size() - 1; i++) {
            String year = getYear(table, i);
            String type = getType(table, i);
            String currency = getCurrency(table, i);
            Integer grossProfitAmount = getValue(table, i, 2);
            result[getIndexForFieldName("gross_profit_year_" + (i + 1), fields) ] = year;
            result[getIndexForFieldName("gross_profit_type_" + (i + 1), fields) ] = type;
            result[getIndexForFieldName("gross_profit_ori_" + (i + 1), fields) ] =
                    grossProfitAmount != null ? currency + (grossProfitAmount * 1000) : getValueString(table, i, 2);
        }
    }

    private void parseTurnover(Object[] result, Element table) {
        for (int i = 0; i < table.child(0).children().size() - 1; i++) {
            String year = getYear(table, i);
            String type = getType(table, i);
            String currency = getCurrency(table, i);
            Integer turnoverAmount = getValue(table, i, 1);
            result[getIndexForFieldName("turnover_year_" + (i + 1), fields) ] = year;
            result[getIndexForFieldName("turnover_type_" + (i + 1), fields) ] = type;
            result[getIndexForFieldName("turnover_ori_" + (i + 1), fields) ] =
                    turnoverAmount != null ? currency + (turnoverAmount * 1000) : getValueString(table, i, 1);
        }
    }

    private Integer getValue(Element table, int i, int j) {
        String stringValue = getValueString(table, i, j);
        try {
            NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.FRANCE);
            integerInstance.setParseIntegerOnly(true);
            return integerInstance.parse(stringValue).intValue();
        } catch (ParseException e) {
            return null;
        }
    }

    private String getCurrency(Element table, int i) {
        String firstChar = table.child(0).child(i + 1).child(2).ownText().substring(0, 1);
        if (firstChar.equalsIgnoreCase("'") ) firstChar = "$";
        return firstChar;
    }

    private String getType(Element table, int i) {
        return table.child(0).child(i + 1).child(1).ownText();
    }

    private String getYear(Element table, int i) {
        return table.child(0).child(i + 1).child(0).ownText();
    }

    private boolean noMoreBusynessesOnPage(Document doc) {
        // only paging numbers left
        return doc.getElementById("listings_results").children().size() == 1;
    }
}
