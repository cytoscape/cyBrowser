package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Component;

import javax.swing.Icon;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class ResultsPanelBrowser implements CytoPanelComponent2, CyBrowser {

	private final CyBrowserManager manager;
	private final SwingPanel panel;
	private final String title;
	private final Icon icon;
	private final String id;
	private final CytoPanelName cytoPanel;

	public ResultsPanelBrowser(
		CyBrowserManager manager,
		String id,
		SwingPanel reuse,
		String title,
		Icon icon,
		CytoPanelName cytoPanel
	) {
		this.manager = manager;
		this.cytoPanel = cytoPanel;
		panel = new SwingPanel(manager, id, null, reuse, false, false);
		this.id = id;

		if (title != null)
			this.title = title;
		else
			this.title = "CyBrowser";

		this.icon = icon;
	}
	
	public ResultsPanelBrowser(
		CyBrowserManager manager,
		String id,
		SwingPanel reuse,
		String title,
		CytoPanelName cytoPanel
	) {
		this(manager, id, reuse, title, null, cytoPanel);
	}

	public ResultsPanelBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title) {
		this(manager, id, reuse, title, CytoPanelName.EAST);
	}

	public ResultsPanelBrowser(CyBrowserManager manager, String id, String title) {
		this(manager, id, null, title, CytoPanelName.EAST);
	}

	@Override
	public SwingPanel getPanel(String id) {
		return panel;
	}

	@Override
	public void loadURL(final String url) {
		loadURL(url, false);
	}

	@Override
	public void loadURL(final String url, boolean newTab) {
		loadURL(url, false, null);
	}

	@Override
	public void loadURL(final String url, boolean newTab, String tabID) {
		/* TODO: handle tab */
		panel.loadURL(url);
	}

	@Override
	public String getURL(String id) {
		return panel.getURL();
	}

	@Override
	public void loadText(final String text) {
		loadText(text, false, null);
	}

	@Override
	public void loadText(final String text, boolean newTab) {
		loadText(text, newTab, null);
	}

	@Override
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
	public Icon getIcon() {
		return icon;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getTitle(String id) {
		return title;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return cytoPanel;
	}
}
