package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import java.io.IOException;

public class ScraperBase extends BaseStep implements StepInterface {
    public static final int MAX_CONNS_TO_SINGE_SERVER = 32;   // not to be rude, limit simultaneous connectins to server on this #
    /**
     * Used to enable {@link Scraper} to log when it needs to.
     */
    public class LoggerForScraper {
        public void logMinimal(final String toLog) {
            ScraperBase.this.logMinimal(toLog);
        }

        public void logBasic(final String toLog) {
            ScraperBase.this.logBasic(toLog);
        }
    }

    private Scraper scraper;
    private LoggerForScraper loggerForScraper;

    public ScraperBase(final StepMeta stepMeta, final StepDataInterface stepDataInterface, int copyNr, final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
        this.loggerForScraper = new LoggerForScraper();
    }

    @Override
    public boolean init(final StepMetaInterface smi, final StepDataInterface sdi) {
        if (!super.init(smi, sdi) ) return false;
        // set up data interface
        ScraperBaseMeta meta = (ScraperBaseMeta) smi;
        ScraperBaseData data = (ScraperBaseData) sdi;

        // initialize scraperworker
        try {
            Class<? extends Scraper> clazz = meta.getScraperClass();
            // Use the no-arg ctor
            scraper = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            logError("Could not instantiate " + meta.getScraperClass().getSimpleName() + " instance, make sure there is such class in classpath and it has a no-arg ctor!");
            e.printStackTrace();
            return false;
        }

        data.setOutputRowInterface(new RowMeta() );  // build on empty row meta
        try {
            meta.getFields( data.getOutputRowInterface(), getStepname(), null, null, this, getRepository(), getMetaStore() );
        } catch (KettleStepException e) {
            logError("Failed to initialize output row structure...");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        ScraperBaseData scraperBaseData = (ScraperBaseData) sdi;
        ScraperBaseMeta scraperBaseMeta = (ScraperBaseMeta) smi;

        Object[] r = new Object[] {};

        if (first) {
            first = false;
        }

//        String url = "https://ec.europa.eu/eipp/desktop/en/list-view.html";
        String url = scraperBaseMeta.getSourceUrl();

        r = RowDataUtil.resizeArray(r, scraperBaseData.getOutputRowInterface().size() );    // size could be hard coded as 1
        try {
            r[scraperBaseData.getOutputRowInterface().size() - 1] = scraper.scrapeUrl(url, loggerForScraper); // index could be hard coded as 0
        } catch (IOException e) {
            logError("There was a problem during data retrieval from " + url);
            e.printStackTrace();
            setOutputDone();
            return false;
        }
        putRow(scraperBaseData.getOutputRowInterface(), r);

        incrementLinesInput();
        setOutputDone();

        return false;
    }

}