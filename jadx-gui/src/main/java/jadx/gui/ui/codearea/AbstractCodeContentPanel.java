package jadx.gui.ui.codearea;

import java.awt.Component;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;

/**
 * The abstract base class for a content panel that show text based code or a.g. a resource
 */
public abstract class AbstractCodeContentPanel extends ContentPanel {
	private static final long serialVersionUID = 4685846894279064422L;

	protected AbstractCodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
	}

	public abstract @Nullable AbstractCodeArea getCodeArea();

	public abstract Component getChildrenComponent();

	public void scrollToPos(int pos) {
		AbstractCodeArea codeArea = getCodeArea();
		if (codeArea != null) {
			codeArea.requestFocus();
			codeArea.scrollToPos(pos);
		}
	}
}
