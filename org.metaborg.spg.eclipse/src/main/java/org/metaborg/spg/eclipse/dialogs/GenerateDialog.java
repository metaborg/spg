package org.metaborg.spg.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class GenerateDialog extends AmbiguityDialog {
	public static String DEFAULT_FUEL = "500";
	public static boolean DEFAULT_STORE = false;
	
    private Text txtFuel;
    private Button ckbStore;
    
	private String fuel;
	private boolean store;

	public GenerateDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        
		Group group = new Group(area, SWT.NONE);
		group.setText("Generation configuration");
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        group.setLayout(new GridLayout(2, false));

        txtFuel = createField(group, "Fuel:", DEFAULT_FUEL);
        ckbStore = createCheckbox(group, "Store programs:", DEFAULT_STORE);

        return area;
    }

    @Override
    protected void okPressed() {
    	fuel = txtFuel.getText();
        store = ckbStore.getSelection();
        
        super.okPressed();
    }
        
    public Integer getFuel() {
    	return Integer.valueOf(fuel);
    }
    
    public boolean getStore() {
    	return store;
    }
}
