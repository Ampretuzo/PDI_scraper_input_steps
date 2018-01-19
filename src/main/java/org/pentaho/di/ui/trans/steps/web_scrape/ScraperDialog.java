package org.pentaho.di.ui.trans.steps.web_scrape;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.web_scrape.ScraperMeta;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class ScraperDialog extends BaseStepDialog implements StepDialogInterface {
    private final ScraperMeta scraperMeta;
    private final String initialStepName;
    private final String initialSourceUrl;
    private final String initialOutputFieldName;

    // widgets
//    private CCombo urlFieldCombo;
    private TextVar sourceUrlTextfield;
    private TextVar outputFieldInput;

    // Handlers
    private class ValueChangeModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            scraperMeta.setChanged(true);
        }
    }

    // NOTE: for some reason, this is the constructor PDI actually uses, not the BaseStepDialog ones...

    public ScraperDialog(Shell parent, Object in, TransMeta transMeta, String stepname) {
        this(parent, (BaseStepMeta) in, transMeta, stepname);
    }
    public ScraperDialog(Shell parent, BaseStepMeta baseStepMeta, TransMeta transMeta, String stepname) {
        super(parent, baseStepMeta, transMeta, stepname);
        this.scraperMeta = (ScraperMeta) baseStepMeta;
        this.initialStepName = stepname;
        this.initialSourceUrl = this.scraperMeta.getSourceUrl();
        this.initialOutputFieldName = this.scraperMeta.getOutputFieldName();
    }

    @Override
    public String open() {
        // NOTE: Dialog class was lifted from XsdValidatorDialog
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
        props.setLook(shell);
        setShellImage(shell, scraperMeta);

        changed = scraperMeta.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText("Web Scraper");

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // /////////////////
        // Filename line ///
        // /////////////////

        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText("Step name");
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);
        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(new ValueChangeModifyListener() );
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment(middle, 0);
        fdStepname.top = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

        // ////////////////////////
        // START OF GENERAL TAB ///
        // ////////////////////////

        CTabItem wGeneralTab = new CTabItem(wTabFolder, SWT.NONE);
        wGeneralTab.setText("Configuration");

        Composite wGeneralComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wGeneralComp);

        FormLayout generalLayout = new FormLayout();
        generalLayout.marginWidth = 3;
        generalLayout.marginHeight = 3;
        wGeneralComp.setLayout(generalLayout);

        // ////////////////////////////////////////
        // START OF Url and Result fields GROUP ///
        // ////////////////////////////////////////

        Group urlAndRes = new Group(wGeneralComp, SWT.SHADOW_NONE);
        props.setLook(urlAndRes);
        urlAndRes.setText("Url and result fields");

        FormLayout urlAndResFormLayout = new FormLayout();
        urlAndResFormLayout.marginWidth = 10;
        urlAndResFormLayout.marginHeight = 10;
        urlAndRes.setLayout(urlAndResFormLayout);

        // url field from prev step
        Label urlFieldLabel = new Label(urlAndRes, SWT.RIGHT);
        urlFieldLabel.setText("Url field");
        props.setLook(urlFieldLabel);
        FormData urlFieldLabelFormData = new FormData();
        urlFieldLabelFormData.left = new FormAttachment(0, 0);
        urlFieldLabelFormData.top = new FormAttachment(wStepname, margin);
        urlFieldLabelFormData.right = new FormAttachment(middle, -margin);
        urlFieldLabel.setLayoutData(urlFieldLabelFormData);

        sourceUrlTextfield = new TextVar( transMeta, urlAndRes, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( sourceUrlTextfield );
        sourceUrlTextfield.addModifyListener(new ValueChangeModifyListener() );
        FormData sourceUrlTextfieldFormData = new FormData();
        sourceUrlTextfieldFormData.left = new FormAttachment( middle, margin );
        sourceUrlTextfieldFormData.right = new FormAttachment( 100, -margin );
        sourceUrlTextfieldFormData.top = new FormAttachment( wStepname, margin );
        sourceUrlTextfield.setLayoutData( sourceUrlTextfieldFormData );

        /*
         * output field
         */
        Label outputFieldInputLabel = new Label( urlAndRes, SWT.RIGHT );
        outputFieldInputLabel.setText( "Output field name" );
        props.setLook( outputFieldInputLabel );
        FormData outputFieldInputLabelFormData = new FormData();
        outputFieldInputLabelFormData.left = new FormAttachment( 0, 0 );
        outputFieldInputLabelFormData.top = new FormAttachment( sourceUrlTextfield, margin );
        outputFieldInputLabelFormData.right = new FormAttachment( middle, -margin );
        outputFieldInputLabel.setLayoutData( outputFieldInputLabelFormData );

        outputFieldInput = new TextVar( transMeta, urlAndRes, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( outputFieldInput );
        outputFieldInput.addModifyListener(new ValueChangeModifyListener() );
        FormData outputFieldInputFormData = new FormData();
        outputFieldInputFormData.left = new FormAttachment( middle, margin );
        outputFieldInputFormData.right = new FormAttachment( 100, -margin );
        outputFieldInputFormData.top = new FormAttachment( sourceUrlTextfield, margin );
        outputFieldInput.setLayoutData( outputFieldInputFormData );

        // ///////////////////////////////////////////////////////////
        // / END OF Url and Result fields GROUP
        // ///////////////////////////////////////////////////////////

        FormData fdGeneralComp = new FormData();
        fdGeneralComp.left = new FormAttachment(0, 0);
        fdGeneralComp.top = new FormAttachment(0, 0);
        fdGeneralComp.right = new FormAttachment(100, 0);
        fdGeneralComp.bottom = new FormAttachment(100, 0);
        wGeneralComp.setLayoutData(fdGeneralComp);

        wGeneralComp.layout();
        wGeneralTab.setControl(wGeneralComp);
        props.setLook(wGeneralComp);

        // ///////////////////////////////////////////////////////////
        // / END OF GENERAL TAB
        // ///////////////////////////////////////////////////////////

        // ///////////////////////////////////////////////////////////
        // / END OF GENERAL TAB
        // ///////////////////////////////////////////////////////////

        FormData fdTabFolder = new FormData();
        fdTabFolder.left = new FormAttachment(0, 0);
        fdTabFolder.top = new FormAttachment(wStepname, margin);
        fdTabFolder.right = new FormAttachment(100, 0);
        fdTabFolder.bottom = new FormAttachment(100, -50);
        wTabFolder.setLayoutData(fdTabFolder);

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText("OK");

        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText("Cancel");

        setButtonPositions(new Button[]{wOK, wCancel}, margin, wTabFolder);

        // Add listeners
        lsCancel = new Listener() {
            public void handleEvent(Event e) {
                cancel();
            }
        };

        lsOK = new Listener() {
            public void handleEvent(Event e) {
                ok();
            }
        };

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);

        wTabFolder.setSelection(0);

        // Set the shell size, based upon previous time...
        setSize();

        getData();
        scraperMeta.setChanged(backupChanged);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return stepname;
    }

    private void getData() {
        if (scraperMeta.getSourceUrl() != null) {
            sourceUrlTextfield.setText(/*scraperMeta.getSourceUrl()*/ "!hard coded at the moment!");
        }
        if (scraperMeta.getOutputFieldName() != null) {
            outputFieldInput.setText(scraperMeta.getOutputFieldName() );
        }
        wStepname.selectAll();
        wStepname.setFocus();
    }

    private void cancel() {
        stepname = null;
        scraperMeta.setChanged(backupChanged);
        dispose();
    }

    private void ok() {
        stepname = wStepname.getText();
        scraperMeta.setSourceUrl(sourceUrlTextfield.getText() );
        scraperMeta.setOutputFieldName(outputFieldInput.getText() );
        dispose();
    }


    // below 3 methods might come in handy for fancy changed/unchanged handling.
    private boolean urlFieldChanged() {
        return !sourceUrlTextfield.getText().equals(initialSourceUrl);
    }

    private boolean outputFieldNameChanged() {
        return !outputFieldInput.getText().equals(initialOutputFieldName);
    }

    private boolean stepNameChanged() {
        return !stepname.equals(initialStepName);
    }
}
