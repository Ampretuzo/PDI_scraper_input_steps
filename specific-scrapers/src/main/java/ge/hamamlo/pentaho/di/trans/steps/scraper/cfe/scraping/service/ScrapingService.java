package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.service;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperOutput;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.entity.Industry;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers.JsoupDocument;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers.Text;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers.Url;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.ScraperCFE.getFieldsSize;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.ScraperCFE.getIndexForFieldName;

public class ScrapingService {

    private static final String LARGE = "22002";
    private static final String MEDIUM = "22001";
    private static final String SMALL = "22000";
    private static final String TINY = "21999";

    private static List<Industry> industryList = new ArrayList<Industry>() {
        {
            add(new Industry("M&A in automotive", "Automotive", LARGE));
            add(new Industry("M&A in Chemicals", "Chemicals", LARGE));
            add(new Industry("M&A in healthcare", "Healthcare", LARGE, MEDIUM));
            add(new Industry("M&A in ICT", "IT or ICT", LARGE, MEDIUM));
            add(new Industry("M&A in manufacturing", "Manufacturing", LARGE, MEDIUM));
            add(new Industry("M&A in pharma", "Pharma", LARGE));
        }
    };

    private List<Object[]> objectList = new ArrayList<>();
    private List<String> list = new CopyOnWriteArrayList<>();
    private Industry currentIndustry;
    private ScraperBase.LoggerForScraper logger;
    private ScraperOutput scraperOutput;
//    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    public void run(ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput) {
        this.logger = logger;
        this.scraperOutput = scraperOutput;
        Document document = JsoupDocument.connect(Url.getMainUrl());
        if (document == null) {
            return;
        }

        Elements industries = document.select("div#article_content > table > tbody > tr > td > a > img");
        for (Element industryEl : industries) {
            String name = industryEl.attr("alt");
            Industry industry = getIndustry(name);
            if (industry == null) {
                continue;
            }
            industry.setLink(industryEl.parent().absUrl("href"));

            currentIndustry = industry;
            scrapeIndustry();
        }

//        executorService.shutdown();
    }

    private void scrapeIndustry() {
//        logger.logBasic("Scraping " + currentIndustry.getNormalName());
        Document document = JsoupDocument.connect(currentIndustry.getLink());
        if (document == null) {
            return;
        }
        Elements menus = document.select("div.submenu > ul > li > a");

        String forSaleLink = null;
        for (Element menu : menus) {
            if (menu.text().toLowerCase().endsWith(" for sale")) {
                forSaleLink = menu.absUrl("href");
                break;
            }
        }

        if (forSaleLink != null) {
            scrapeForSale(forSaleLink);
        }
    }

    private void scrapeForSale(String forSaleLink) {
        Document document = JsoupDocument.connect(forSaleLink);
        if (document == null) {
            return;
        }
        Elements formElements = document.select("form#filterform [name]");
        for (String size : currentIndustry.getSizeCriteria()) {
            StringBuilder searchUrl = new StringBuilder(document.baseUri() + "?");
            for (Element formElement : formElements) {
                String tagName = formElement.tagName();
                if (tagName.equalsIgnoreCase("input")) {
                    searchUrl.append(formElement.attr("name"))
                            .append("=")
                            .append(formElement.attr("value"))
                            .append("&");
                } else if (tagName.equalsIgnoreCase("select")) {
                    if (formElement.previousElementSibling().text().equalsIgnoreCase("size")) {
                        searchUrl.append(formElement.attr("name"))
                                .append("=")
                                .append(size)
                                .append("&");
                    } else {
                        Elements options = formElement.select("option");
                        for (Element option : options) {
                            if (option.hasAttr("selected")) {
                                searchUrl.append(formElement.attr("name"))
                                        .append("=")
                                        .append(option.attr("value"))
                                        .append("&");
                                break;
                            }
                        }
                    }
                }
            }

            scrapeSearchUrl(searchUrl.toString());
        }
    }

    private void scrapeSearchUrl(String searchUrl) {
        int page = 2;
        while (true) {
            Document document = JsoupDocument.connect(searchUrl);
            if (document == null) {
                break;
            }
            Elements businessSaleLinks = document.select("#article_content a");
            for (Element businessSaleLinkEl : businessSaleLinks) {
                String link = businessSaleLinkEl.attr("href");
                final String href = businessSaleLinkEl.absUrl("href");
                if (link.contains("/business-sale/")) {
                    scrapeBusinessSale(href);
                }
            }

            Element nextPage = document.select("a[rel='" + page + "']").first();
            if (nextPage == null) {
                break;
            }
            searchUrl = nextPage.absUrl("href");
            page ++;
        }
    }

    private Object[] scrapeBusinessSale(String businessSaleLink) {
        if (list.contains(businessSaleLink)) {
            return null;
        }
        list.add(businessSaleLink);
        Document document = JsoupDocument.connect(businessSaleLink);
        if (document == null) {
            return null;
        }

        Object[] objects = new Object[getFieldsSize()];

        //page url
        objects[getIndexForFieldName("page_url")] = businessSaleLink;

        //pro name
        Element nameEl = document.select("#article_content h1").first();
        if (nameEl == null) {
            return null;
        }
        String proName = Text.clean(nameEl.text());
        objects[getIndexForFieldName("pro_name")] = proName;

        //industry
        String industryType = currentIndustry.getNormalName();
        objects[getIndexForFieldName("ori_industry")] = Text.clean(industryType);

        //details
        Element detailsEl = document.select("#article_content > ul").first();
        if (detailsEl != null) {
            for (Element detailEl : detailsEl.select("> li")) {
                String key = "";
                String value = "";
                String detail = detailEl.text();
                String[] split = detail.split(":");
                if (split.length > 0) {
                    key = Text.clean(split[0]);
                }
                if (split.length > 1) {
                    value = Text.clean(split[1]);
                }

                //sales
                if (key.startsWith("Sales")) {
                    if (value.isEmpty()) {
                        value = key.replace("Sales", "").trim();
                    }
                    objects[getIndexForFieldName("sales_ori")] = value;
                }

                //ebitda margin
                else if (key.startsWith("EBITDA margin")) {
                    if (value.isEmpty()) {
                        value = key.replace("EBITDA margin", "").trim();
                    }
                    objects[getIndexForFieldName("ebitda_margin_ori")] = value;
                }

                //ebitda
                else if (key.startsWith("EBITDA")) {
                    if (value.isEmpty()) {
                        value = key.replace("EBITDA", "").trim();
                    }
                    objects[getIndexForFieldName("ebitda_ori")] = value;
                }

                //ebit
                else if (key.startsWith("EBIT")) {
                    if (value.isEmpty()) {
                        value = key.replace("EBIT", "").trim();
                    }
                    objects[getIndexForFieldName("ebit_ori")] = value;
                }

                //location
                else if (key.startsWith("Location of")) {
                    objects[getIndexForFieldName("location")] = value;
                }

                //employee
                else if (key.startsWith("Employees")) {
                    if (value.isEmpty()) {
                        value = key.replace("Employees", "").trim();
                    }
                    objects[getIndexForFieldName("employee_num")] = value;
                }

                //price
                else if (key.startsWith("Price for") || key.startsWith("Valuation demands") ||
                        key.startsWith("Estimated Company Value")) {
                    objects[getIndexForFieldName("price_ori")] = value;
                }

                //revenue
                else if (key.startsWith("Revenue")) {
                    if (value.isEmpty()) {
                        value = key.replace("Revenue", "").trim();
                    }
                    objects[getIndexForFieldName("revenue_ori")] = value;
                }

                //reason for sale
                else if (key.startsWith("Reason for sale")) {
                    if (value.isEmpty()) {
                        value = key.replace("Reason for sale", "").trim();
                    }
                    objects[getIndexForFieldName("motivation_for_trade")] = value;
                }

                // years in biz
                else if (key.startsWith("Established")) {
                    if (value.isEmpty()) {
                        value = key.replace("Established", "").trim();
                    }
                    objects[getIndexForFieldName("years_in_biz_ori")] = value;
                }
            }
        }

        //description
        Elements titles = document.select("#article_content > h2");
        if (titles.size() > 0) {
            titles.remove(0);
        }
        if (titles.size() > 0) {
            titles.remove(titles.size() - 1);
        }
        StringBuilder description = new StringBuilder("");
        for (Element title : titles) {
            description.append(title.text()).append("\n");
            Element content = title.nextElementSibling();
            if (content.tagName().equalsIgnoreCase("ul")) {
                for (Element liEl : content.children()) {
                    description.append(liEl.text()).append("\n");
                }
            } else {
                description.append(Text.clean(content.text()));
            }
            description.append("\n\n");
        }
        objects[getIndexForFieldName("descrip")] = description.toString();
        objects[getIndexForFieldName("timeline_insert")] = LocalDateTime.now();
        objects[getIndexForFieldName("requirement_type")] = "sell";
        objects[getIndexForFieldName("language")] = "en";

        scraperOutput.yield(objects);

        return objects;
    }

    private Industry getIndustry(String name) {
        for (Industry industry : industryList) {
            if (industry.getName().equals(name)) {
                return industry;
            }
        }

        return null;
    }

    public List<Object[]> getObjectList() {
        return objectList;
    }
}
