package org.pentaho.di.trans.steps.web_scrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import java.io.IOException;

public class Scraper extends BaseStep implements StepInterface {

    public Scraper(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        if (!super.init(smi, sdi) ) return false;
        // set up data interface
        ScraperMeta meta = (ScraperMeta) smi;
        ScraperData data = (ScraperData) sdi;
        /*RowMetaInterface inputRowMeta = getInputRowMeta();
        try {
            // inputRowMeta is not initialized
            meta.getFields( inputRowMeta, getStepname(), null, null, this, getRepository(), getMetaStore() );
        } catch (KettleStepException e) {
            e.printStackTrace();
            return false;
        }
        data.setOutputRowInterface(inputRowMeta);*/
        return true;
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        ScraperData scraperData = (ScraperData) sdi;
        ScraperMeta scraperMeta = (ScraperMeta) smi;

        Object[] r = null;
        r = getRow();

        if ( r == null ) { // no more rows to be expected from the previous step(s)
            setOutputDone();
            return false;
        }

        if (first) {
            first = false;
            scraperData.setOutputRowInterface(getInputRowMeta().clone() );
            scraperData.getOutputRowInterface().addValueMeta(scraperMeta.getOutputFieldMetaInterface() );
        }

        String urlFieldName = scraperMeta.getUrlFieldName();
        String url = getInputRowMeta().getString(r, getInputRowMeta().indexOfValue(urlFieldName) );
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
        } catch (IOException e) {
            logBasic("Could not connect to " + url);
            return false;
        }

        String[] words = doc.text().split(" ");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            Object[] rc = getInputRowMeta().cloneRow(r);
            RowDataUtil.resizeArray(rc, scraperData.getOutputRowInterface().size() );
            rc[scraperData.getOutputRowInterface().size() - 1] = word;
            putRow(scraperData.getOutputRowInterface(), rc);
        }

//        RowDataUtil.resizeArray(r, scraperData.getOutputRowInterface().size() );
//        r[scraperData.getOutputRowInterface().size() - 1] = "Hello World!";
//        putRow(scraperData.getOutputRowInterface(), r);

        return true;
    }


}
