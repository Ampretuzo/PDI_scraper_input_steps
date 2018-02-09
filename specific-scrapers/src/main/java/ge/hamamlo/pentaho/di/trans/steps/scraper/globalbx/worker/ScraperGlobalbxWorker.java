package ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx.ScraperGlobalbx.getIndexForFieldName;

public class ScraperGlobalbxWorker {
    private List<String> urlList;
    private ScraperBase.LoggerForScraper logger;
    private Map<String, String> cookies;
    private String[] fields;
    private String referrer;

    private class ScraperGlobalbxWorkersWorker implements Callable<Object[] > {
        private final String url;

        ScraperGlobalbxWorkersWorker(String url) {
            this.url = url;
        }

        @Override
        public Object[] call() throws Exception {
            System.out.println(Thread.currentThread().getName() + " Fatching page: " + url);
            Object[] result = new Object[fields.length];
            try {
                //set url
                result[getIndexForFieldName("page_url", fields)] = url;
                Document document = Jsoup.connect(url)
                        .cookies(cookies)
                        .timeout(40000)
                        .referrer(referrer)
                        .execute()
                        .parse();

                Element mainFormElm = document.select("form[name=AForm]").first();
                if (mainFormElm != null) {

                    //set name
                    Element nameElm = mainFormElm.select("table > tbody > tr > td > font > b").first();
                    if (nameElm != null) {
                        result[getIndexForFieldName("pro_name", fields)] = nameElm.text();
                    }


                    //set industry
                    Element industryElm = mainFormElm.select("b:contains(Industry:)").first();
                    if (industryElm != null) {
                        Matcher m = Pattern.compile("Industry:(.*?)>(.*)").matcher(industryElm.text());
                        if (m.find()) {
                            result[getIndexForFieldName("industry_ori_industry", fields)] = m.group(1).trim();
                            result[getIndexForFieldName("industry_ori_subindustry", fields)] = m.group(2).trim();
                        }
                    }

                    //set location
                    Element locationElm = mainFormElm.select("b:contains(Industry:)").first();
                    if (locationElm != null) {
                        Matcher m = Pattern.compile("Location:(.*?)Industry:").matcher(locationElm.text().trim());
                        if (m.find()) {
                            if (m.group(1).contains("->")) {
                                String[] locationArr = m.group(1).split("->");
                                if (locationArr.length == 2) {
                                    result[getIndexForFieldName("location_origin_country", fields)] = locationArr[0].trim();
                                    result[getIndexForFieldName("location_origin_province", fields)] = locationArr[1].trim();
                                } else if (locationArr.length == 3) {
                                    result[getIndexForFieldName("location_origin_country", fields)] = locationArr[0].trim();
                                    result[getIndexForFieldName("location_origin_province", fields)] = locationArr[1].trim();
                                    result[getIndexForFieldName("location_origin_city", fields)] = locationArr[2].trim();
                                }
                            } else {
                                result[getIndexForFieldName("location_origin_country", fields)] = m.group(1).trim();
                            }
                        }
                    }

                    //set description
                    setArrayFieldsWithOffset(result, getDescription(mainFormElm), 1, "descrip_");


                    //target founded time
                    result[getIndexForFieldName("target_founded_time", fields)] = getData("Year Established", mainFormElm, false);

                    //employee numbers
                    result[getIndexForFieldName("target_employee_num", fields)] = getData("Number of Employees", mainFormElm, false);

                    //policies offer
                    result[getIndexForFieldName("target_policies_offer_1", fields)] = getData("Seller Financing", mainFormElm, true);
                    result[getIndexForFieldName("target_policies_offer_2", fields)] = getData("Management Training and Support", mainFormElm, true);

                    //finance info
                    result[getIndexForFieldName("target_finance_info_gross_revenue_ori", fields)] = getData("Gross Revenues", mainFormElm, false);
                    result[getIndexForFieldName("target_finance_info_cash_flow_ori", fields)] = getData("Cash Flow", mainFormElm, false);
                    result[getIndexForFieldName("target_finance_info_cash_flow_ori_type", fields)] = getData("Cash Flow Type", mainFormElm, false);
                    result[getIndexForFieldName("target_finance_info_inventory_ori", fields)] = getData("Inventory", mainFormElm, false);

                    //premises
                    result[getIndexForFieldName("target_premises_ownership", fields)] = getData("Current Real Estate", mainFormElm, false);
                    result[getIndexForFieldName("target_premises_property_info", fields)] = getData("Property:", mainFormElm, false);

                    //price
                    result[getIndexForFieldName("price_ori", fields)] = getData("Asking Price Range", mainFormElm, false);

                    //timeline
                    result[getIndexForFieldName("timeline_insert", fields) ] = new Date();

                    result[getIndexForFieldName("project_language", fields) ] = "en";
                    result[getIndexForFieldName("requirement_type", fields) ] = "sell";
                    result[getIndexForFieldName("motivation_for_trade", fields)] = getData("Reason For Selling", mainFormElm, false);

                }
            } catch (IOException e) {
                String message = Thread.currentThread().getName() + " Fetching page from " + url + " error! " + e.toString();
                if (logger == null) {
                    System.out.println(message);
                } else {
                    logger.logBasic(message);
                }
                return null;    // to differentiate from valid result
            }
            return result;
        }
    }

    public ScraperGlobalbxWorker(List<String> urlList, ScraperBase.LoggerForScraper logger, Map<String, String> cookies, String[] fields, String referrer) {
        this.referrer = referrer;
        this.urlList = urlList;
        this.logger = logger;
        this.cookies = cookies;
        this.fields = fields;
    }


    public List<Object[]> scrapeBusiness() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);  // don't choose more because server refuses to answer concurrent requests
        List<ScraperGlobalbxWorkersWorker> workersWorkers = new ArrayList<>();
        for (int i = 0; i < urlList.size(); i++) {
            workersWorkers.add(new ScraperGlobalbxWorkersWorker(urlList.get(i) ) );
        }
        try {
            List<Future<Object[]>> futures = executorService.invokeAll(workersWorkers);
            List<Object[]> resultList = new ArrayList<>();
            for (Future<Object[] > futureResult : futures) {
                Object[] result = futureResult.get();
                if (result != null) resultList.add(result);
            }
            executorService.shutdown();
            return resultList;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getData(String textInElm, Element mainFormElm, boolean getAll) {
        String text = "";
        Element elm = mainFormElm.select("div#businessdetails font:contains(" + textInElm + ")").first();
        if (elm != null) {
            if (getAll) {
                text = elm.text().trim();
            } else {
                text = elm.ownText().trim();
            }
        }
        return text;
    }

    private void setArrayFieldsWithOffset(Object[] result, List<String> values, int offset, String fieldNameBase) {
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String fieldName = fieldNameBase + (i + offset);
            result[getIndexForFieldName(fieldName)] = value;
        }
    }

    private List<String> getDescription(Element mainElm) {
        List<String> descriptionList = new ArrayList<>();
        Element descriptionElm = mainElm.select("div.KonaBody").first();
        if (descriptionElm != null) {
            descriptionList.add(descriptionElm.text());
        }

        Element d2 = mainElm.select("div#businessdetails font:contains(Seller Financing)").first();
        if (d2 != null) {
            descriptionList.add(d2.text());
        }

        Element d3 = mainElm.select("div#businessdetails font:contains(Relocatable)").first();
        if (d3 != null) {
            descriptionList.add(d3.text());
        }

        Element d4 = mainElm.select("div#businessdetails font:contains(Franchise)").first();
        if (d4 != null) {
            descriptionList.add(d4.text());
        }
        return descriptionList;
    }

}
