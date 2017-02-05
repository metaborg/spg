package org.metaborg.spg.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SoundnessDialog extends GenerateDialog {
	public static String DEFAULT_TIMEOUT = "4";
	
    private Text txtTimeout; 
    
    private String timeout;

	public SoundnessDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        
        Group group = new Group(area, SWT.SHADOW_IN);
        group.setText("Run configuration");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        group.setLayout(new GridLayout(2, false));

        txtTimeout = createField(group, "Timeout:", DEFAULT_TIMEOUT);

        return area;
    }

    @Override
    protected void okPressed() {
    	timeout = txtTimeout.getText();
        
        super.okPressed();
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }
    
    public Integer getTimeout() {
    	return Integer.valueOf(timeout);
    }
}
