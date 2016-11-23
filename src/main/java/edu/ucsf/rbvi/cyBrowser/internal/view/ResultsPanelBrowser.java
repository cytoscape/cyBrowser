package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.CyServiceRegistrar;

public class ResultsPanelBrowser implements CytoPanelComponent2 {

	private final CyServiceRegistrar registrar;
	private final SwingPanel panel;
	private String title;
 
	public ResultsPanelBrowser(CyServiceRegistrar registrar, String title) {
		super();
		this.registrar = registrar;
		panel = new SwingPanel(registrar, null, false);

		if (title != null)
			this.title = title;
		else
			this.title = "CyBrowser";
		
	}

	public void loadURL(final String url) {
		panel.loadURL(url);
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
	public String getTitle() { return "CyBrowser"; }

	@Override
	public CytoPanelName getCytoPanelName() { return CytoPanelName.EAST; }
}
