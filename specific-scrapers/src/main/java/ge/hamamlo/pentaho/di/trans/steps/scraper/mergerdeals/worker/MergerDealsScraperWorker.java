package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.getIndexForFieldName;

public class MergerDealsScraperWorker implements Runnable {
    private final CountDownLatch notifyMainThatWeAreDone;
    private final Object[] result;
    private final String[] fields;
    private final String busyness_url;
    private final ScraperBase.LoggerForScraper logger;
    private final AtomicBoolean successfulParse;

    public MergerDealsScraperWorker(final CountDownLatch waitForAllBusynessesToFinish, final Object[] result, final String[] fields, final String busyness_url, final ScraperBase.LoggerForScraper logger, final AtomicBoolean successfulParse) {
        this.notifyMainThatWeAreDone = waitForAllBusynessesToFinish;
        this.result = result;
        this.fields = fields;
        this.busyness_url = busyness_url;
        this.logger = logger;
        this.successfulParse = successfulParse;
    }

    @Override
    public void run() {
        try {
            scrapeBusynessPage(result, busyness_url);
            successfulParse.set(true);
        } catch (IOException e) {
            logger.logBasic("Could not process page: " + busyness_url);
            successfulParse.set(false);
        } finally {
            notifyMainThatWeAreDone.countDown();
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
            // we resort to basic text processing
            String pureBody = key.toString().replaceAll("[<](/)?div[^>]*[>]", "").trim();
            // there are a lot of different things that could happen:
            if (!pureBody.contains("<br>") ) {
                /*
                 * <div id="key"><div style="background-color:#fff;padding:20px">The company has maintained consistent revenue growth from 2014 through 2016. In 2014, revenues were approximately $1.5 million and have grown to $3.2 million in 2017. The opening of new multiple locations throughout Texas and the United States is driving this growth.</div></div>
                 * this is pure text nothing can be parsed
                 */
                result[getIndexForFieldName("turnover_ori_1", fields) ] = pureBody;
            } else if (pureBody.toLowerCase().contains("average") ) {
                /*
                 * <br>
                 *  <br>Average Revenues: $66,391
                 *  <br>Average Gross Profit: $3,866
                 *  <br>Average Adjusted EBITDA*: $2,315
                 *  <br>
                 *  <br>*Earnings before interest, tax, depreciation and amortization (non-cash items)
                 */
                List<String> strings = Arrays.asList(pureBody.split(" ") );
                String averageRevenue = strings.get(strings.indexOf("Revenues:") + 1 ).replace(",", "").replace(".", "").trim() + "000";
                String averageGrossProfit = strings.get(strings.indexOf("Profit:") + 1 ).replace(",", "").replace(".", "").trim() + "000";
                String averageEbitda = strings.get(strings.indexOf("EBITDA*:") + 1).replace(",", "").replace(".", "").trim() + "000";
                result[getIndexForFieldName("adj_ebidta_year_1", fields) ] = "Average";
                result[getIndexForFieldName("turnover_year_1", fields) ] = "Average";
                result[getIndexForFieldName("gross_profit_year_1", fields) ] = "Average";
                result[getIndexForFieldName("adj_ebidta_type_1", fields) ] = "";    // there is no type information
                result[getIndexForFieldName("turnover_type_1", fields) ] = "";  // there is no type information
                result[getIndexForFieldName("gross_profit_type_1", fields) ] = "";   // there is no type information
                result[getIndexForFieldName("adj_ebidta_ori_1", fields) ] = averageEbitda;
                result[getIndexForFieldName("turnover_ori_1", fields) ] = averageRevenue;
                result[getIndexForFieldName("gross_profit_ori_1", fields) ] = averageGrossProfit;

//            } else if (pureBody.toLowerCase().contains("company") ) {
                /*
                 * Company 1 (Pipeline Construction):
                 * <br>$'000 10/31/17 2016 2015 2014
                 * <br> YTD Tax Rtns Tax Rtns Tax Rtns
                 * <br>Revenue 2,360 2,740 3,809 4,674
                 * <br>
                 * <br>Company 2 (Industrial Supply):
                 * <br>$'000 10/31/17 2016 2015 2014
                 * <br> YTD Tax Rtns Tax Rtns Tax Rtns
                 * <br>Revenue 8,318 9,301 19,892 22,074
                 */
//            } else if (pureBody.toLowerCase().contains("revenue") && pureBody.toLowerCase().contains("profit") && pureBody.toLowerCase().contains("ebidta") ) {
                /*
                 * YE Dec 31st 6/30/2017 2016 2015 2014 2013
                 * <br>$'000 Review Review Review Review Review
                 * <br>Gross Revenue 6,875 8,934 7,615 10,937 10,177
                 * <br>Gross Profit 1,982 2,701 1,983 4,210 3 ,848
                 * <br>Adj. EBITDA 684 371 -260 785 463
                 */
            } else {
                // fall back to this so that information is not lost
                result[getIndexForFieldName("turnover_ori_1", fields) ] = pureBody;
            }
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
        if (stringValue == null || stringValue.equals("") ) return null;
        try {
            return Integer.parseInt(
                    stringValue.replace(",", "").replace(".", "")
            );
        } catch (NumberFormatException nfe) {
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
}
