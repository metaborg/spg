package org.metaborg.spg.sentence.eclipse.dialog;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public abstract class SentenceDialog extends TitleAreaDialog {
    private static final String EMPTY_STRING = "";

    public SentenceDialog(Shell parentShell) {
        super(parentShell);
    }

    protected Text createField(Composite container, String fieldLabel) {
        return createField(container, fieldLabel, EMPTY_STRING);
    }

    // TODO: Label + Widget = Fieldset?
    protected Text createField(Composite container, String fieldLabel, String fieldDefault) {
        Label label = new Label(container, SWT.NONE);
        label.setText(fieldLabel);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        Text text = new Text(container, SWT.FILL | SWT.BORDER);
        text.setText(fieldDefault);
        text.setLayoutData(gridData);

        return text;
    }

    protected Button createCheckbox(Composite container, String fieldLabel, boolean fieldDefault) {
        Label label = new Label(container, SWT.NONE);
        label.setText(fieldLabel);

        Button button = new Button(container, SWT.CHECK);
        button.setSelection(fieldDefault);

        return button;
    }
}
