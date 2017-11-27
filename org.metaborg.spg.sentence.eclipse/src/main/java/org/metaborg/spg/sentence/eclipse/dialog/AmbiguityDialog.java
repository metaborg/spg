package org.metaborg.spg.sentence.eclipse.dialog;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AmbiguityDialog extends SentenceDialog {
    private static final String TITLE = "Ambiguity test";
    private static final String MESSAGE = "Specify the generator configuration.";

    private static final String DEFAULT_MAX_NUMBER_OF_TERMS = "1000";
    private static final String DEFAULT_MAX_TERM_SIZE = "1000";

    private Text txtMaxNumberOfTerms;
    private Text txtMaxTermSize;

    private String maxNumberOfTerms;
    private String maxTermSize;

    public AmbiguityDialog(Shell parentShell) {
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

        Group group = new Group(area, SWT.NONE);
        group.setText("Generator configuration");
        group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtMaxNumberOfTerms = createField(group, "Max number of terms:", DEFAULT_MAX_NUMBER_OF_TERMS);
        txtMaxTermSize = createField(group, "Max term size:", DEFAULT_MAX_TERM_SIZE);

        return area;
    }

    @Override
    protected void okPressed() {
        maxNumberOfTerms = txtMaxNumberOfTerms.getText();
        maxTermSize = txtMaxTermSize.getText();

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
}
