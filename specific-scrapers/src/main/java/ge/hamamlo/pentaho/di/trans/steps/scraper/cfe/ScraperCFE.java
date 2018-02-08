package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.*;
import ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.service.ScrapingService;

import java.io.IOException;
import java.util.List;

public class ScraperCFE implements Scraper {

    private static final String[] fields = {
            "pro_name",
            "page_url",
            "timeline_insert",
            "location",
            "ori_industry",
            "requirement_type",
            "language",
            "price_ori",
            "descrip",
            "years_in_biz_ori",
            "employee_num",
            "revenue_ori",
            "ebitda_ori",
            "ebit_ori",
            "ebitda_margin_ori",
            "sales_ori",
            "motivation_for_trade"
    };

    // utility method
    public static int getIndexForFieldName(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            String fieldNameOther = fields[i];
            if (fieldNameOther.equalsIgnoreCase(fieldName)) return i;
        }
        throw new RuntimeException("There is no such field as - " + fieldName);
    }

    @Override
    public FieldDef[] fields() {
        FieldDef[] fieldDefs = new FieldDef[fields.length];
        for (int i = 0; i < fields.length; i++) {
            FieldDef fieldDef = new FieldDef();
            fieldDef.setName(fields[i]);
            fieldDef.setFieldType(FieldDef.FieldType.STRING);
            fieldDefs[i] = fieldDef;
        }

        return fieldDefs;
    }

    @Override
    public void scrapeUrl(String url, ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput, ScraperInformation information) throws IOException {
        // logger.logBasic("Start scraping CFE " + url);
        List<Object[]> allData = scrapeWebsite(logger, scraperOutput);
        // logger.logBasic("Scraped " + allData.size() + " items");
        /*for (Object[] projectData : allData) {
            if (projectData == null) continue;
            scraperOutput.yield(projectData);
        }*/
        scraperOutput.yield(null);
        logger.logBasic("Finished!");
    }

    private List<Object[]> scrapeWebsite(ScraperBase.LoggerForScraper logger, ScraperOutput scraperOutput) {
        ScrapingService scrapingService = new ScrapingService();
        scrapingService.run(logger, scraperOutput);

        return scrapingService.getObjectList();
    }

    public static int getFieldsSize() {
        return fields.length;
    }
}
