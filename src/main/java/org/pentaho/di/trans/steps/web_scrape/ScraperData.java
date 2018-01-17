package org.pentaho.di.trans.steps.web_scrape;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class ScraperData extends BaseStepData implements StepDataInterface {
    private RowMetaInterface outputRowInterface;

    public RowMetaInterface getOutputRowInterface() {
        return outputRowInterface;
    }

    public void setOutputRowInterface(RowMetaInterface outputRowInterface) {
        this.outputRowInterface = outputRowInterface;
    }
}
