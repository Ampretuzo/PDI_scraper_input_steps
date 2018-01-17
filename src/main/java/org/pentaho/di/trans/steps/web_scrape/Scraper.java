package org.pentaho.di.trans.steps.web_scrape;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Scraper extends BaseStep implements StepInterface {

    private ScraperWorker scraperWorker;

    public Scraper(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        if (!super.init(smi, sdi) ) return false;
        // set up data interface
        ScraperMeta meta = (ScraperMeta) smi;
        ScraperData data = (ScraperData) sdi;

        // initialize scraperworker
        try {
            Class<? extends ScraperWorker> clazz = (Class<? extends ScraperWorker>) Class.forName("org.pentaho.di.trans.steps.web_scrape.ScraperWorkerImpl");
            Constructor<? extends ScraperWorker> ctor = clazz.getConstructor();
            scraperWorker = ctor.newInstance(new Object[] {});
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
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

        RowDataUtil.resizeArray(r, scraperData.getOutputRowInterface().size() );
        r[scraperData.getOutputRowInterface().size() - 1] = scraperWorker.scrapeUrl(url); // getAll(projectUrls);
        putRow(scraperData.getOutputRowInterface(), r);

        return true;
    }

}
