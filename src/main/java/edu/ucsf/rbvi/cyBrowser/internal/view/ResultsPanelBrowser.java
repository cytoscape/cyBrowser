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
  private final CytoPanelName cytoPanel;

	public ResultsPanelBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title, CytoPanelName cytoPanel) {
		super();
		this.manager = manager;
    this.cytoPanel = cytoPanel;
		panel = new SwingPanel(manager, id, null, reuse, false, false);
		this.id = id;

		if (title != null)
			this.title = title;
		else
			this.title = "CyBrowser";
  }

	public ResultsPanelBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title) {
    this(manager, id, reuse, title, CytoPanelName.EAST);
	}

	public ResultsPanelBrowser(CyBrowserManager manager, String id, String title) {
    this(manager, id, null, title, CytoPanelName.EAST);
  }

	public SwingPanel getPanel(String id) { return panel; }

	public void loadURL(final String url) {
		loadURL(url, false);
	}

	public void loadURL(final String url, boolean newTab) {
		loadURL(url, false, null);
	}

	public void loadURL(final String url, boolean newTab, String tabID) {
		/* TODO: handle tab */
		panel.loadURL(url);
	}

	public String getURL(String id) { return panel.getURL(); }

	public void loadText(final String text) {
		loadText(text, false, null);
	}

	public void loadText(final String text, boolean newTab) {
		loadText(text, newTab, null);
	}

	public void loadText(final String text, boolean newTab, String tabID) {
		/* TODO: handle tab */
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

	public String getTitle(String id) { return title; }

	@Override
	public CytoPanelName getCytoPanelName() { return cytoPanel; }
}
