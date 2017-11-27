package org.metaborg.spg.sentence.eclipse.dialog;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class DifferenceDialog extends SentenceDialog {
    private static final String TITLE = "Difference test";
    private static final String MESSAGE = "Specify the generator- and ANTLR-configuration.";

    private static final String DEFAULT_MAX_NUMBER_OF_TERMS = "1000";
    private static final String DEFAULT_MAX_TERM_SIZE = "1000";
    private static final String DEFAULT_ANTLR_GRAMMAR = "";
    private static final String DEFAULT_ANTLR_RULE = "";

    private Text txtMaxNumberOfTerms;
    private Text txtMaxTermSize;
    private Text txtAntlrGrammar;
    private Text txtAntlrRule;

    private String maxNumberOfTerms;
    private String maxTermSize;
    private String antlrGrammar;
    private String antlrRule;

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

        createGenerationConfiguration(area);
        createAntlrConfiguration(area);

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

        txtAntlrGrammar = createField(group, "ANTLR grammar:", DEFAULT_ANTLR_GRAMMAR);
        txtAntlrRule = createField(group, "ANTLR start rule:", DEFAULT_ANTLR_RULE);
    }

    @Override
    protected void okPressed() {
        maxNumberOfTerms = txtMaxNumberOfTerms.getText();
        maxTermSize = txtMaxTermSize.getText();
        antlrGrammar = txtAntlrGrammar.getText();
        antlrRule = txtAntlrRule.getText();

        super.okPressed();
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

    public String getAntlrRulee() {
        return antlrRule;
    }
}
