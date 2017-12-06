package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.CyServiceRegistrar;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class ResultsPanelBrowser implements CytoPanelComponent2, CyBrowser {

	private final CyBrowserManager manager;
	private final SwingPanel panel;
	private final String title;
	private final String id;

	public ResultsPanelBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title) {
		super();
		this.manager = manager;
		panel = new SwingPanel(manager, id, null, reuse, false, false);
		this.id = id;

		if (title != null)
			this.title = title;
		else
			this.title = "CyBrowser";
	}
 
	public ResultsPanelBrowser(CyBrowserManager manager, String id, String title) {
		super();
		this.manager = manager;
		panel = new SwingPanel(manager, id, null, null, false, false);
		this.id = id;

		if (title != null)
			this.title = title;
		else
			this.title = "CyBrowser";
		
	}

	public SwingPanel getPanel() { return panel; }

	public void loadURL(final String url) {
		panel.loadURL(url);
	}

	public String getURL() { return panel.getURL(); }

	public void loadText(final String text) {
		panel.loadText(text);
	}

	@Override
	public String getIdentifier() {
		return this.getClass().getName();
	}

	@Override
	public Component getComponent() {
		return panel;
	}

	@Override
	public Icon getIcon() { return null; }

	@Override
	public String getTitle() { return title; }

	@Override
	public CytoPanelName getCytoPanelName() { return CytoPanelName.EAST; }
}
