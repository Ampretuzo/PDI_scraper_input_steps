package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import ge.hamamlo.pentaho.di.ui.trans.steps.scraper.base.ScraperDialog;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

import static org.pentaho.di.core.row.ValueMetaInterface.TYPE_STRING;

public class ScraperBaseMeta extends BaseStepMeta implements StepMetaInterface {
    public static final String DEFAULT_OUTPUT_FIELD_NAME = "scrapeResult";

    private String sourceUrl;
    private String outputFieldName;
    private Class<? extends Scraper> scraperClass;

    public ScraperBaseMeta() {
        super();
    }

    /**
     * Override this method to set {@code scraperClass} at the startup.
     */
    @Override
    public void setDefault() {
        sourceUrl = null;
        outputFieldName = DEFAULT_OUTPUT_FIELD_NAME;
    }

    @Override
    public Object clone() {
        // For the user it does not really make sense to duplicate this step...
        ScraperBaseMeta newScraperBaseMeta = (ScraperBaseMeta) super.clone();
        // Strings are immutable, it's safe to set same
        newScraperBaseMeta.setSourceUrl(this.sourceUrl);
        newScraperBaseMeta.setOutputFieldName(this.outputFieldName);
        return newScraperBaseMeta;
    }

    @Override
    public String getXML() throws KettleException {
        StringBuilder retval = new StringBuilder(300);
        retval  .append("    <sourceUrl>")
                .append(sourceUrl)
                .append("</sourceUrl>")
                .append(Const.CR);
        retval  .append("    <outputFieldName>")
                .append(outputFieldName)
                .append("</outputFieldName>")
                .append(Const.CR);
        return retval.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        super.loadXML(stepnode, databases, metaStore);
        this.sourceUrl = XMLHandler.getTagValue(stepnode, "sourceUrl");
        this.outputFieldName = XMLHandler.getTagValue(stepnode, "outputFieldName");
    }

    @Override
    public String getDialogClassName() {
        return ScraperDialog.class.getName();
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        return new ScraperBase(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new ScraperBaseData();
    }

    @Override
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
                          VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
        super.getFields(inputRowMeta, name, info, nextStep, space, repository, metaStore);
        try {
            ValueMetaInterface outputField = ValueMetaFactory.createValueMeta(TYPE_STRING);
            outputField.setName(outputFieldName);
            inputRowMeta.addValueMeta(outputField);
        } catch (KettlePluginException e) {
            throw new KettleStepException( e );
        }

    }

    // *****************************************************************************************************************
    // getters and setters below

    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String outputFieldName) {
        this.outputFieldName = outputFieldName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Class<? extends Scraper> getScraperClass() {
        return scraperClass;
    }

    public void setScraperClass(Class<? extends Scraper> scraperClass) {
        this.scraperClass = scraperClass;
    }
}
