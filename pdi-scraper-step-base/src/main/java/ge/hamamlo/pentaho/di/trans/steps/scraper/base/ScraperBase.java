package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * Used as an output interface for {@link Scraper}.
     */
    public interface ScraperOutput {
        /**
         * Feed output through this interface, pass null when done.
         */
        void yield(Object[] output);
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

        try {
            scraper = meta.instantiateScraper();
        } catch (ReflectiveOperationException e) {
            logError("Could not instantiate " + meta.getScraperClass().getSimpleName() + " instance, make sure there is such class in classpath and it has a no-arg ctor!");
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

        if (first) {
            first = false;
        }

        String url = scraperBaseMeta.getSourceUrl();

        CountDownLatch cdl = new CountDownLatch(1); // to wait for all outputs
        AtomicBoolean flagToDisableStepOutput = new AtomicBoolean(false);

        try {
            scraper.scrapeUrl(url, loggerForScraper, (Object[] output) -> {
                // don't do anything if main thread was interrupted
                if (flagToDisableStepOutput.get() ) return;
                // unlock main thread to let it exit processRow
                if (output == null) {
                    cdl.countDown();
                    return;
                }
                try {
                    putRow(scraperBaseData.getOutputRowInterface(), output);
                    incrementLinesInput();
                } catch (KettleStepException e) {
                    // *might* need to set a flag, so that we don't attempt to putRow after it failed once
                    // TODO: handling or wrapping it and throwing back
                    e.printStackTrace();
                }
            } );
        } catch (IOException e) {
            logError("There was a problem during data retrieval from " + url);
            setOutputDone();
            return false;
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            flagToDisableStepOutput.set(true);
            String message = "Interrupted while waiting for " + url;
            logError(message);
            throw new KettleException(message);
        } finally {
            setOutputDone();
        }
        return false;
    }

}
