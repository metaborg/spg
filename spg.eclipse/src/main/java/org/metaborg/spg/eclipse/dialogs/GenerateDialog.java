package org.metaborg.spg.eclipse.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class GenerateDialog extends TitleAreaDialog {
	public static String DEFAULT_TERM_LIMIT = "100";
	
    private Text txtTermLimit;
    private String termLimit;

	public GenerateDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
    public void create() {
        super.create();
        
        setTitle("Spoofax Generation");
        setMessage("Term generation configuration", IMessageProvider.INFORMATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        createTermLimit(container);

        return area;
    }

    private void createTermLimit(Composite container) {
        Label lblTermLimit = new Label(container, SWT.NONE);
        lblTermLimit.setText("Term Limit");
        
        GridData dataTermLimit = new GridData();
        dataTermLimit.grabExcessHorizontalSpace = true;
        dataTermLimit.horizontalAlignment = GridData.FILL;
        
        txtTermLimit = new Text(container, SWT.BORDER);
        txtTermLimit.setText(DEFAULT_TERM_LIMIT);
        txtTermLimit.setLayoutData(dataTermLimit);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
    	termLimit = txtTermLimit.getText();
        
        super.okPressed();
    }

    public Integer getTermLimit() {
    	return Integer.valueOf(termLimit);
    }
}
