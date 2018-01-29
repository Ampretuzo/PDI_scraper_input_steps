package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import ge.hamamlo.pentaho.di.ui.trans.steps.scraper.base.ScraperDialog;
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
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef.*;
import static ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef.FieldType.*;
import static org.pentaho.di.core.row.ValueMetaInterface.*;

public class ScraperBaseMeta extends BaseStepMeta implements StepMetaInterface {
    private String sourceUrl;
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
    }

    @Override
    public Object clone() {
        // For the user it does not really make sense to duplicate this step...
        ScraperBaseMeta newScraperBaseMeta = (ScraperBaseMeta) super.clone();
        // Strings are immutable, it's safe to set same
        newScraperBaseMeta.setSourceUrl(this.sourceUrl);
        return newScraperBaseMeta;
    }

    @Override
    public String getXML() throws KettleException {
        StringBuilder retval = new StringBuilder(300);
        retval.append("    " + XMLHandler.addTagValue("sourceUrl", sourceUrl) );
        return retval.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        super.loadXML(stepnode, databases, metaStore);
        this.sourceUrl = XMLHandler.getTagValue(stepnode, "sourceUrl");
    }

    public Scraper instantiateScraper() throws ReflectiveOperationException {
        Class<? extends Scraper> clazz = getScraperClass();
        // Use the no-arg ctor
        return clazz.getConstructor().newInstance();
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
        Scraper scraper = null;
        try {
            scraper = instantiateScraper();
        } catch (ReflectiveOperationException e) {
            logError("Could not instantiate " + getScraperClass().getSimpleName() + " instance, make sure there is such class in classpath and it has a no-arg ctor!");
            throw new KettleStepException(e);
        }
        FieldDef[] fieldDefs = scraper.fields();
        for (FieldDef fieldDef : fieldDefs) {
            try {
                int type = getFieldType(fieldDef);
                ValueMetaInterface outputField = ValueMetaFactory.createValueMeta(type);
                outputField.setName(fieldDef.getName() );
                inputRowMeta.addValueMeta(outputField);
            } catch (KettlePluginException e) {
                throw new KettleStepException(e);
            }
        }
    }

    private int getFieldType(FieldDef fieldDef) {
        int type = -1;
        switch (fieldDef.getFieldType() ) {
            case STRING:
                type = TYPE_STRING;
                break;
            case NUMBER:
                type = TYPE_NUMBER;
                break;
            case BOOLEAN:
                type = TYPE_BOOLEAN;
                break;
            case DATE:
                type = TYPE_DATE;
                break;
            default: throw new RuntimeException("Unhandled field type!");
        }
        return type;
    }

    // *****************************************************************************************************************
    // getters and setters below

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
