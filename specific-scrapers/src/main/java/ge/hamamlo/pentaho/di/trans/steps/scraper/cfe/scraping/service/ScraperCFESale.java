package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.service;

import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.entity.Industry;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers.JsoupDocument;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers.Text;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.ScraperCFE.getFieldsSize;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.ScraperCFE.getIndexForFieldName;

public class ScraperCFESale implements Callable<Object[] > {
    private final String busynessSaleLink;
    private final List<String> list;
    private final Industry currentIndustry;

    public ScraperCFESale(String busynessSaleLink, final List<String> list, final Industry currentIndustry) {
        this.busynessSaleLink = busynessSaleLink;
        this.list = list;
        this.currentIndustry = currentIndustry;
    }

    @Override
    public Object[] call() throws Exception {
        return scrapeBusinessSale(busynessSaleLink);
    }

    private Object[] scrapeBusinessSale(String businessSaleLink) {
//        List<String> list = new CopyOnWriteArrayList<>();
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

        return objects;
    }

}
