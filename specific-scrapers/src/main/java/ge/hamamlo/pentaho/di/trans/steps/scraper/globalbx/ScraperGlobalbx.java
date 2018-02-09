package ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.*;
import ge.hamamlo.pentaho.di.trans.steps.scraper.globalbx.worker.ScraperGlobalbxWorker;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ScraperGlobalbx implements Scraper {

    private static final String HEALTH_AND_INDUSTRY_DATA_POST_STRING = "&txtindlist=Health+and+Medical%7CManufacturing&industry=10&industry=12&d=&a1=10000000&a2=&r1=&r2=&c1=&c2=&page=searchpage";

    private static final String TECHNOLOGY_DATA_POST_STRING = "&txtindlist=Technology+%96+Internet+Related&industry=17&d=&a1=5000000&a2=&r1=&r2=&c1=&c2=&page=searchpage";

    private static final String COMMON_DATA_POST_STRING = "formid=AForm&submit_form=R&searchResultsPathInfo=%2Fresults.asp&FT=1&txtBarea=&txtKeywordBasic=&A=1";

    private static final String SEARCH_URL = "http://www.globalbx.com/searchResultsRedirector.asp";

    private ScraperBase.LoggerForScraper logger;

    private static final String[] fields = {
            "pro_name",
            "page_url",
            "industry_ori_industry",
            "industry_ori_subindustry",

            "location_origin_country",
            "location_origin_province",
            "location_origin_city",

            "descrip_1",
            "descrip_2",
            "descrip_3",
            "descrip_4",

            "target_founded_time",
            "target_employee_num",
            "target_policies_offer_1",
            "target_policies_offer_2",

            "target_finance_info_gross_revenue_ori",
            "target_finance_info_cash_flow_ori",
            "target_finance_info_cash_flow_ori_type",
            "target_finance_info_inventory_ori",

            "target_premises_ownership",
            "target_premises_property_info",

            "price_ori",
            "timeline_insert",
            "project_language",
            "requirement_type",
            "motivation_for_trade"
    };

    @Override
    public FieldDef[] fields() {
        FieldDef[] allStringFieldDefs = createAllStringFieldDefs(fields);
        allStringFieldDefs[getIndexForFieldName("timeline_insert", fields)].setFieldType(FieldDef.FieldType.DATE);
        return allStringFieldDefs;
    }

    @Override
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput, ScraperInformation information) throws IOException {
        this.logger = logger;
        Connection.Response resp = Jsoup.connect(url)
                .header("Accept-Encoding", "gzip, deflate")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                .timeout(40000).execute();
        Document document = resp.parse();
        Map<String, String> cookies = resp.cookies();
        Elements locationElements = document.select("select#BizLocation option");


        ExecutorService executorService = Executors.newFixedThreadPool(1);
        CompletionService<List<Object[]>> service = new ExecutorCompletionService<>(executorService);


        for (Element locationElm : locationElements) {
            StringBuilder dataSb = new StringBuilder(COMMON_DATA_POST_STRING);
            StringBuilder dataTechSb = new StringBuilder(COMMON_DATA_POST_STRING);
            int locationId = Integer.parseInt(locationElm.attr("value"));
            if ((locationId > 0 && locationId < 52) || (locationId > 52 && locationId < 66)) {
                continue;
            }
            String locationName = locationElm.text();
            locationName = " >> " + locationName.trim().substring(3, locationName.length());

            dataSb.append("&BizLocation=").append(locationId)
                    .append("&txtloclist=").append(locationId)
                    .append("&txtCountry=").append(URLEncoder.encode(locationName, "UTF-8"));

            dataTechSb.append(dataSb);
            dataSb.append(HEALTH_AND_INDUSTRY_DATA_POST_STRING);
            dataTechSb.append(TECHNOLOGY_DATA_POST_STRING);

            CatalogFetcher catalogFetcher = new CatalogFetcher(SEARCH_URL, dataSb.toString(), cookies, logger);
            // CatalogFetcher catalogTechFetcher = new CatalogFetcher(SEARCH_URL, dataTechSb.toString(), cookies);

            service.submit(catalogFetcher);
            // service.submit(catalogTechFetcher);
        }
        executorService.shutdown();
        try {
            while (!executorService.isTerminated()) {
                Future<List<Object[]>> future = service.take();
                for (Object[] objects : future.get()) {
                    if (objects == null) {
                        continue;
                    }
                    scraperOutput.yield(objects);
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logBasic("ERROR " + e.toString());
            } else {
                System.out.println("ERROR " + e.toString());
            }
        } finally {
            scraperOutput.yield(null);
        }

    }

    public static int getIndexForFieldName(String fieldName, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            String fieldNameOther = fields[i];
            if (fieldNameOther.equalsIgnoreCase(fieldName)) return i;
        }
        throw new RuntimeException("There is no such field as - " + fieldName);
    }

    public static int getIndexForFieldName(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            String fieldNameOther = fields[i];
            if (fieldNameOther.equalsIgnoreCase(fieldName)) return i;
        }
        throw new RuntimeException("There is no such field as - " + fieldName);
    }


    public static FieldDef[] createAllStringFieldDefs(String[] fields) {
        FieldDef[] stringFieldDefs = new FieldDef[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i];
            stringFieldDefs[i] = new FieldDef();
            stringFieldDefs[i].setFieldType(FieldDef.FieldType.STRING);
            stringFieldDefs[i].setName(fieldName);
        }
        return stringFieldDefs;
    }

    private class CatalogFetcher implements Callable<List<Object[]>> {

        private String url;
        private String data;
        private Map<String, String> cookies;
        private ScraperBase.LoggerForScraper logger;


        private CatalogFetcher(String url, String data, Map<String, String> cookies, ScraperBase.LoggerForScraper logger) {
            this.url = url;
            this.data = data;
            this.cookies = cookies;
            this.logger = logger;
        }

        @Override
        public List<Object[]> call() throws Exception {
            Map<String, String> dataMap = new HashMap<>();
            Arrays.stream(data.split("&")).map(str -> str.split("=")).forEach(arr -> {
                if (arr.length == 2 && arr[0].length() > 0) {
                    try {
                        dataMap.put(arr[0], URLDecoder.decode(arr[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (arr.length == 1 && arr[0].length() > 0) {
                    dataMap.put(arr[0], "");
                }
            });

            Connection response = Jsoup.connect(url)
                    .referrer("http://www.globalbx.com/search.asp")
                    .followRedirects(true)
                    .cookies(cookies)
                    .timeout(40000)
                    .method(Connection.Method.POST)
                    .data(dataMap);
            Connection.Response resp = null;
            try {
                if (dataMap.get("industry") != null && dataMap.get("industry").equals("12")) {
                    resp = response.data("industry", "10").execute();
                } else {
                    resp = response.execute();
                }
            } catch (IOException e) {
                if (logger != null) {
                    logger.logBasic("ERROR >> " + e.toString());
                } else {
                    System.out.println("ERROR >> " + e.toString());
                }
            }
            List<String> businessUrlList = new ArrayList<>();
            int page = 1;
            if (resp != null) {
                while (true) {
                    Document catalogDocument = resp.parse();
                    Elements businessElements = catalogDocument.select("tr > td.lh a");
                    businessUrlList.addAll(businessElements.stream().map(elm -> "http://www.globalbx.com" + elm.attr("href")).collect(Collectors.toList()));
                    Elements inputElements = catalogDocument.select("form[name=ResultForm] input[type=hidden]");
                    page++;
                    if (catalogDocument.select("tr#bottom a[onclick^=GoToPage]:contains(Next)").first() == null) {
                        break;
                    } else {
                        Map<String, String> dataNextPageMap = new HashMap<>();
                        for (Element inputElm : inputElements) {
                            if (inputElm.attr("name").equals("PageIndex")) {
                                dataNextPageMap.put(inputElm.attr("name"), Integer.toString(page));
                            } else {
                                dataNextPageMap.put(inputElm.attr("name"), inputElm.attr("value"));
                            }
                        }
                        URL url = resp.url();
                        resp = Jsoup.connect(SEARCH_URL)
                                .followRedirects(true)
                                .timeout(40000)
                                .referrer(url.toString())
                                .method(Connection.Method.POST)
                                .cookies(cookies)
                                .data(dataNextPageMap).execute();
                    }
                }
            }

            ScraperGlobalbxWorker scraperGlobalbxWorker = new ScraperGlobalbxWorker(businessUrlList, logger, cookies, fields, url);
            return scraperGlobalbxWorker.scrapeBusiness();
        }
    }
}
