package org.metaborg.spg.sentence.antlr.eclipse.widget;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.metaborg.spg.sentence.antlr.eclipse.Activator;

public class SmartCombo extends Combo {
	private final IDialogSettings settings = Activator.getDefault().getDialogSettings();

	public SmartCombo(String id, Composite parent, int style) {
		super(parent, style);

		parent.addDisposeListener(e -> {
			storeHistory(id);
		});

		loadHistory(id);
	}

	protected void loadHistory(String id) {
		String[] history = settings.getArray(id);

		if (history != null) {
			setItems(history);
		}
	}

	protected void storeHistory(String id) {
		settings.put(id, getAllItems());
	}

	protected String[] getAllItems() {
		Set<String> history = new LinkedHashSet<String>();

		String text = getText();
		if (text != null && text.trim().length() > 0) {
			history.add(text);
		}

		String[] items = getItems();
		for (String item : items) {
			history.add(item);
		}

		return history.toArray(new String[history.size()]);
	}

	@Override
	protected void checkSubclass() {
	}
}
