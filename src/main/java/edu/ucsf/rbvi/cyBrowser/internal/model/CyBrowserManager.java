package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.Properties;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.service.util.CyServiceRegistrar;

import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class CyBrowserManager {

	CyServiceRegistrar registrar;
	ResultsPanelBrowser browser = null;
  CytoPanel cytoPanel = null;

	public CyBrowserManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
		this.cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
	}

	public void registerCytoPanel(ResultsPanelBrowser browser) {
		this.browser = browser;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				registrar.registerService(browser, CytoPanelComponent.class, new Properties());
				cytoPanel.setState(CytoPanelState.DOCK);
			}
		});
	}

	public void unregisterCytoPanel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				registrar.unregisterService(browser, CytoPanelComponent.class);
				cytoPanel.setState(CytoPanelState.DOCK);
			}
		});
	}
}
