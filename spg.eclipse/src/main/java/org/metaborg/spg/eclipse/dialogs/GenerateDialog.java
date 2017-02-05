package org.metaborg.spg.eclipse.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class GenerateDialog extends TitleAreaDialog {
	public static String DEFAULT_TERM_LIMIT = "100";
	public static String DEFAULT_TERM_SIZE = "100";
	public static String DEFAULT_FUEL = "500";
	
    private Text txtTermLimit;
    private Text txtTermSize;
    private Text txtFuel; 
    
    private String termLimit;
	private String termSize;
	private String fuel;

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
        
		Group group = new Group(area, SWT.NONE);
		group.setText("Generation configuration");
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtTermLimit = createField(group, "Term Limit:", DEFAULT_TERM_LIMIT);
        txtTermSize = createField(group, "Term Size:", DEFAULT_TERM_SIZE);
        txtFuel = createField(group, "Fuel:", DEFAULT_FUEL);

        return area;
    }
    
    protected Text createField(Composite container, String optionLabel, String optionDefault) {
        Label label = new Label(container, SWT.NONE);
        label.setText(optionLabel);
        
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        
        Text text = new Text(container, SWT.FILL | SWT.BORDER);
        text.setText(optionDefault);
        text.setLayoutData(gridData);
        
        return text;
    }

    @Override
    protected void okPressed() {
    	termLimit = txtTermLimit.getText();
    	termSize = txtTermSize.getText();
    	fuel = txtFuel.getText();
        
        super.okPressed();
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

    public Integer getTermLimit() {
    	return Integer.valueOf(termLimit);
    }
    
    public Integer getTermSize() {
    	return Integer.valueOf(termSize);
    }
    
    public Integer getFuel() {
    	return Integer.valueOf(fuel);
    }
}
