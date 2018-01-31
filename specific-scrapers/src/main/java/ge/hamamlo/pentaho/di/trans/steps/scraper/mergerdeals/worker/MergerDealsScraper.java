package ge.hamamlo.pentaho.di.trans.steps.scraper.mergerdeals.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;

import java.io.IOException;

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
            "turnover_1",
            "turnover_2",
            "turnover_3",
            "gross_profit_1",
            "gross_profit_2",
            "gross_profit_3",
            "adj_ebidta_1",
            "adj_ebidta_2",
            "adj_ebidta_3",
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
        scraperOutput.yield(null);
    }
}
