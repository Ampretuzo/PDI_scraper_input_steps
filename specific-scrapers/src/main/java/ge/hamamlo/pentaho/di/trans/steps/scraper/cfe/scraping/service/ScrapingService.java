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
        logger.logBasic("Scraping " + currentIndustry.getNormalName());
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
            List<ScraperCFESale> busynessScrapers = new ArrayList<>();
            for (Element businessSaleLinkEl : businessSaleLinks) {
                String link = businessSaleLinkEl.attr("href");
                final String href = businessSaleLinkEl.absUrl("href");
                if (link.contains("/business-sale/")) {
                    busynessScrapers.add(new ScraperCFESale(href, list, currentIndustry) );
                }
            }
            try {
                ExecutorService executorService = Executors.newFixedThreadPool(32);
                List<Future<Object[] > > futureResults = executorService.invokeAll(busynessScrapers);
                for (Future<Object[] > futureResult : futureResults) {
                    Object[] result = futureResult.get();
                    if (result != null) scraperOutput.yield(result);
                }
                executorService.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();    // TODO: handle
            }

            Element nextPage = document.select("a[rel='" + page + "']").first();
            if (nextPage == null) {
                break;
            }
            searchUrl = nextPage.absUrl("href");
            page ++;
        }
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
