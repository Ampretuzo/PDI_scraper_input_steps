package ge.hamamlo.pentaho.di.trans.steps.scraper.smergers.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;

import java.io.IOException;
import java.util.Date;

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
        for (int i = 0; i < 10; i++) {
            Object[] test = new Object[fields.length];
            for (int j = 0; j < test.length; j++) {
                test[i] = "test" + i;
            }
            test[getIndexForFieldName("timeline_insert", fields) ] = new Date();
            scraperOutput.yield(test);
        }
        scraperOutput.yield(null);
    }
}
