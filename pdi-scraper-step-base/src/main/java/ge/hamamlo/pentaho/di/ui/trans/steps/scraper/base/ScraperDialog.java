package ge.hamamlo.pentaho.di.ui.trans.steps.scraper.base;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBaseMeta;
import org.pentaho.di.ui.core.widget.LabelTextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class ScraperDialog extends BaseStepDialog implements StepDialogInterface {
    private final ScraperBaseMeta scraperBaseMeta;

    // widgets
    private LabelTextVar mongoHostTxtField;
    private LabelTextVar mongoDbNameTxtField;
    private LabelTextVar mongoCollectionTxtField;
    private Button mongoParametersCheckBox;
    private LabelTextVar urlTxtField;
    private LabelTextVar mongoPortTxtField;

    // Handlers
    private class ValueChangeModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            scraperBaseMeta.setChanged(true);
        }
    }

    // NOTE: for some reason, this is the constructor PDI actually uses, not the BaseStepDialog ones...

    public ScraperDialog(Shell parent, Object in, TransMeta transMeta, String stepname) {
        this(parent, (BaseStepMeta) in, transMeta, stepname);
    }
    public ScraperDialog(Shell parent, BaseStepMeta baseStepMeta, TransMeta transMeta, String stepname) {
        super(parent, baseStepMeta, transMeta, stepname);
        this.scraperBaseMeta = (ScraperBaseMeta) baseStepMeta;
    }

    @Override
    public String open() {
        // NOTE: Dialog class was lifted from XsdValidatorDialog
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
        props.setLook(shell);
        setShellImage(shell, scraperBaseMeta);

        changed = scraperBaseMeta.hasChanged();

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

        Group urlAndResGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
        props.setLook(urlAndResGroup);
        urlAndResGroup.setText("Url");

        FormLayout urlAndResFormLayout = new FormLayout();
        urlAndResFormLayout.marginWidth = 10;
        urlAndResFormLayout.marginHeight = 10;
        urlAndResGroup.setLayout(urlAndResFormLayout);

        // url
        urlTxtField =
                new LabelTextVar( transMeta, urlAndResGroup, "Url", "Url to scrape");
        props.setLook( urlTxtField );
        urlTxtField.addModifyListener( new ValueChangeModifyListener() );
        FormData fdIfXMLValid6 = new FormData();
        fdIfXMLValid6.left = new FormAttachment( 0, 0 );
        fdIfXMLValid6.top = new FormAttachment(wStepname, 2 * margin );
        fdIfXMLValid6.right = new FormAttachment( 100, 0 );
        urlTxtField.setLayoutData( fdIfXMLValid6 );

        // ///////////////////////////////////////////////////////////
        // / END OF Url and Result fields GROUP
        // ///////////////////////////////////////////////////////////

        // ////////////////////////////////////////
        // START OF Mongo parameters GROUP ///
        // ////////////////////////////////////////

        Group mongoParametersGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
        props.setLook(mongoParametersGroup);
        mongoParametersGroup.setText("Mongo Parameters");

        FormLayout mongoParametersGroupLayout = new FormLayout();
        mongoParametersGroupLayout.marginWidth = 10;
        mongoParametersGroupLayout.marginHeight = 10;
        mongoParametersGroup.setLayout(mongoParametersGroupLayout);

        // Output String Field ?
        Label mongoParametersCheckboxLabel = new Label( mongoParametersGroup, SWT.RIGHT );
        mongoParametersCheckboxLabel.setText( "Re-process projects?" );
        props.setLook( mongoParametersCheckboxLabel );
        FormData mongoParametersCheckboxLabelFormData = new FormData();
        mongoParametersCheckboxLabelFormData.left = new FormAttachment( 0, 0 );
        mongoParametersCheckboxLabelFormData.top = new FormAttachment( urlAndResGroup, 2 * margin );
        mongoParametersCheckboxLabelFormData.right = new FormAttachment( middle, -margin );
        mongoParametersCheckboxLabel.setLayoutData( mongoParametersCheckboxLabelFormData );
        mongoParametersCheckBox = new Button( mongoParametersGroup, SWT.CHECK );
        props.setLook( mongoParametersCheckBox );
        mongoParametersCheckBox.setToolTipText( "Mark this to skip already processed project urls!" );
        FormData mongoParametersCheckBoxFormData = new FormData();
        mongoParametersCheckBoxFormData.left = new FormAttachment( middle, margin );
        mongoParametersCheckBoxFormData.top = new FormAttachment( urlAndResGroup, 2 * margin );
        mongoParametersCheckBox.setLayoutData( mongoParametersCheckBoxFormData );
        mongoParametersCheckBox.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scraperBaseMeta.setChanged();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                scraperBaseMeta.setChanged();
            }
        });
        mongoParametersCheckBox.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                activeMongoProperties();
            }
        } );

        // host
        mongoHostTxtField =
                new LabelTextVar( transMeta, mongoParametersGroup, "Mongo host", "E.g. localhost");
        props.setLook(mongoHostTxtField);
        mongoHostTxtField.addModifyListener( new ValueChangeModifyListener() );
        FormData fdIfXMLValid = new FormData();
        fdIfXMLValid.left = new FormAttachment( 0, 0 );
        fdIfXMLValid.top = new FormAttachment( mongoParametersCheckBox, margin );
        fdIfXMLValid.right = new FormAttachment( 100, 0 );
        mongoHostTxtField.setLayoutData( fdIfXMLValid );

        // port
        mongoPortTxtField =
                new LabelTextVar( transMeta, mongoParametersGroup, "Mongo Port", "E.g. 27013");
        props.setLook(mongoPortTxtField);
        mongoPortTxtField.addModifyListener( new ValueChangeModifyListener() );
        FormData fdIfXMLValid12 = new FormData();
        fdIfXMLValid12.left = new FormAttachment( 0, 0 );
        fdIfXMLValid12.top = new FormAttachment( mongoHostTxtField, margin );
        fdIfXMLValid12.right = new FormAttachment( 100, 0 );
        mongoPortTxtField.setLayoutData( fdIfXMLValid12 );

        // db name
        mongoDbNameTxtField =
                new LabelTextVar( transMeta, mongoParametersGroup, "Mongo DB Name", "E.g. projects");
        props.setLook(mongoDbNameTxtField);
        mongoDbNameTxtField.addModifyListener( new ValueChangeModifyListener() );
        FormData fdIfXMLValid1 = new FormData();
        fdIfXMLValid1.left = new FormAttachment( 0, 0 );
        fdIfXMLValid1.top = new FormAttachment(mongoPortTxtField, margin );
        fdIfXMLValid1.right = new FormAttachment( 100, 0 );
        mongoDbNameTxtField.setLayoutData( fdIfXMLValid1 );

        // collection name
        mongoCollectionTxtField =
                new LabelTextVar( transMeta, mongoParametersGroup, "Mongo Collection Name", "E.g. projects_scraped");
        props.setLook(mongoCollectionTxtField);
        mongoCollectionTxtField.addModifyListener( new ValueChangeModifyListener() );
        FormData fdIfXMLValid2 = new FormData();
        fdIfXMLValid2.left = new FormAttachment( 0, 0 );
        fdIfXMLValid2.top = new FormAttachment(mongoDbNameTxtField, margin );
        fdIfXMLValid2.right = new FormAttachment( 100, 0 );
        mongoCollectionTxtField.setLayoutData( fdIfXMLValid2 );

        // ///////////////////////////////////////////////////////////
        // / END OF Mongo parameters GROUP
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
        lsCancel = e -> cancel();
        lsOK = e -> ok();

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);

        wTabFolder.setSelection(0);

        // Set the shell size, based upon previous time...
        setSize();

        getData();
        activeMongoProperties();

        scraperBaseMeta.setChanged(backupChanged);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return stepname;
    }

    private void activeMongoProperties() {
        mongoHostTxtField.setEnabled(mongoParametersCheckBox.getSelection() );
        mongoDbNameTxtField.setEnabled(mongoParametersCheckBox.getSelection() );
        mongoCollectionTxtField.setEnabled(mongoParametersCheckBox.getSelection() );
        mongoPortTxtField.setEnabled(mongoParametersCheckBox.getSelection() );
    }

    private void getData() {
        mongoParametersCheckBox.setSelection(!scraperBaseMeta.isReProcessing() );
        if (scraperBaseMeta.getSourceUrl() != null) {
            urlTxtField.setText(scraperBaseMeta.getSourceUrl() );
        }
        if (scraperBaseMeta.getMongoHost() != null) {
            mongoHostTxtField.setText(scraperBaseMeta.getMongoHost() );
        }
        if (scraperBaseMeta.getMongoPort() != null) {
            mongoPortTxtField.setText(scraperBaseMeta.getMongoPort().toString() );
        }
        if (scraperBaseMeta.getMongoDbName() != null) {
            mongoDbNameTxtField.setText(scraperBaseMeta.getMongoDbName() );
        }
        if (scraperBaseMeta.getMongoCollectionName() != null) {
            mongoCollectionTxtField.setText(scraperBaseMeta.getMongoCollectionName() );
        }
        wStepname.selectAll();
        wStepname.setFocus();
    }

    private void cancel() {
        stepname = null;
        scraperBaseMeta.setChanged(backupChanged);
        dispose();
    }

    private void ok() {
        stepname = wStepname.getText();
        if (urlTxtField.getText() != null)
            scraperBaseMeta.setSourceUrl(urlTxtField.getText() );
        if (mongoHostTxtField.getText() != null)
            scraperBaseMeta.setMongoHost(mongoHostTxtField.getText() );
        if (mongoPortTxtField.getText() != null && !mongoPortTxtField.getText().equals("") )
            scraperBaseMeta.setMongoPort(Integer.parseInt(mongoPortTxtField.getText() ) );
        if (mongoDbNameTxtField.getText() != null)
            scraperBaseMeta.setMongoDbName(mongoDbNameTxtField.getText() );
        if (mongoCollectionTxtField.getText() != null)
            scraperBaseMeta.setMongoCollectionName(mongoCollectionTxtField.getText() );
        scraperBaseMeta.setReProcessing(!mongoParametersCheckBox.getSelection() );
        dispose();
    }

}
