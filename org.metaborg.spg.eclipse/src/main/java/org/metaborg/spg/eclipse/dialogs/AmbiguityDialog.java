package org.metaborg.spg.eclipse.dialogs;

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

public class AmbiguityDialog extends TitleAreaDialog {
	public static String DEFAULT_TERM_SIZE = "100";
	
    private Text txtTermLimit;
    private Text txtTermSize;
    
    private String termLimit;
	private String termSize;

	public AmbiguityDialog(Shell parentShell) {
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
        
		Group group = new Group(area, SWT.NONE);
		group.setText("Basic configuration");
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtTermLimit = createField(group, "Term Limit:", getDefaultTermLimit());
        txtTermSize = createField(group, "Term Size:", DEFAULT_TERM_SIZE);

        return area;
    }
    
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

    @Override
    protected void okPressed() {
    	termLimit = txtTermLimit.getText();
    	termSize = txtTermSize.getText();
        
        super.okPressed();
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }
    
    protected String getDefaultTermLimit() {
    	return "100";
    }

    public Integer getTermLimit() {
    	return Integer.valueOf(termLimit);
    }
    
    public Integer getTermSize() {
    	return Integer.valueOf(termSize);
    }
}
