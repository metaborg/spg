package org.metaborg.spg.sentence.eclipse.dialog;

import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.metaborg.spg.sentence.eclipse.widget.SmartCombo;

import java.io.File;
import java.util.Arrays;

public class DifferenceDialog extends SentenceDialog {
    private static final String TITLE = "Difference test";
    private static final String MESSAGE = "Specify the ANTLR- and generator-configuration.";

    private static final String DEFAULT_MAX_NUMBER_OF_TERMS = "10000";
    private static final String DEFAULT_MAX_TERM_SIZE = "10000";

    private Text txtMaxNumberOfTerms;
    private Text txtMaxTermSize;
    private Combo txtAntlrGrammar;
    private Combo txtAntlrStartSymbol;

    private String maxNumberOfTerms;
    private String maxTermSize;
    private String antlrGrammar;
    private String antlrStartSymbol;

    public DifferenceDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();

        setTitle(TITLE);
        setMessage(MESSAGE, IMessageProvider.INFORMATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        createAntlrConfiguration(area);
        createGenerationConfiguration(area);

        return area;
    }

    private void createGenerationConfiguration(Composite area) {
        Group group = new Group(area, SWT.NONE);
        group.setText("Generator configuration");
        group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtMaxNumberOfTerms = createField(group, "Max number of terms:", DEFAULT_MAX_NUMBER_OF_TERMS);
        txtMaxTermSize = createField(group, "Max term size:", DEFAULT_MAX_TERM_SIZE);
    }

    private void createAntlrConfiguration(Composite area) {
        Group group = new Group(area, SWT.NONE);
        group.setText("ANTLR configuration");
        group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtAntlrGrammar = createSmartCombo("antlrGrammar", group, "ANTLR grammar:");
        txtAntlrGrammar.setFocus();

        txtAntlrStartSymbol = createCombo(group, "ANTLR start symbol:");

        txtAntlrGrammar.addListener(SWT.Traverse, e -> {
            if (e.keyCode == SWT.CR) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }

                scanGrammar(txtAntlrGrammar.getText().trim());
            }
        });

        txtAntlrGrammar.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                scanGrammar(txtAntlrGrammar.getText().trim());
            }

            public void widgetSelected(SelectionEvent e) {
                Display.getDefault().asyncExec(() ->
                        scanGrammar(txtAntlrGrammar.getText().trim())
                );
            }
        });
    }

    protected SmartCombo createSmartCombo(String id, Composite container, String fieldLabel) {
        Label label = new Label(container, SWT.NONE);
        label.setText(fieldLabel);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        SmartCombo smartCombo = new SmartCombo(id, container, SWT.NONE);
        smartCombo.setLayoutData(gridData);

        return smartCombo;
    }

    protected Combo createCombo(Composite container, String fieldLabel) {
        Label label = new Label(container, SWT.NONE);
        label.setText(fieldLabel);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        Combo combo = new Combo(container, SWT.NONE);
        combo.setLayoutData(gridData);

        return combo;
    }

    protected void scanGrammar(String location) {
        if (!new File(location).exists()) {
            return;
        }

        String[] rules = Grammar.load(location).getRuleNames();
        Arrays.sort(rules);

        populateStartSymbols(rules);
    }

    protected void populateStartSymbols(String[] symbols) {
        txtAntlrStartSymbol.setItems(symbols);
    }

    @Override
    protected void okPressed() {
        if (!validate()) {
            return;
        }

        maxNumberOfTerms = txtMaxNumberOfTerms.getText();
        maxTermSize = txtMaxTermSize.getText();
        antlrGrammar = txtAntlrGrammar.getText();
        antlrStartSymbol = txtAntlrStartSymbol.getText();

        super.okPressed();
    }

    protected boolean validate() {
        String location = txtAntlrGrammar.getText();

        if (!new File(location).exists()) {
            setErrorMessage("Could not find the ANTLR grammar.");
            return false;
        }

        Grammar grammar = Grammar.load(location);

        if (grammar == null) {
            setErrorMessage("Could not load the ANTLR grammar.");
            return false;
        }

        Rule rule = grammar.getRule(txtAntlrStartSymbol.getText());

        if (rule == null) {
            setErrorMessage("Could not find the given start symbol.");
            return false;
        }

        return true;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public Integer getMaxNumberOfTerms() {
        return Integer.valueOf(maxNumberOfTerms);
    }

    public Integer getMaxTermSize() {
        return Integer.valueOf(maxTermSize);
    }

    public String getAntlrGrammar() {
        return antlrGrammar;
    }

    public String getAntlrStartSymbol() {
        return antlrStartSymbol;
    }
}
