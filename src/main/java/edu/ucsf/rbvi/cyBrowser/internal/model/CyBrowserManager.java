package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.HashMap;
import java.util.Map;
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
  CytoPanel cytoPanel = null;
	Map<String, CyBrowser> idMap;

	public CyBrowserManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
		this.cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		idMap = new HashMap<>();
	}

	public void registerCytoPanel(ResultsPanelBrowser browser) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				registrar.registerService(browser, CytoPanelComponent.class, new Properties());
				cytoPanel.setState(CytoPanelState.DOCK);
			}
		});
	}

	public void unregisterCytoPanel(ResultsPanelBrowser browser) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (browser == null) {
					for (CyBrowser br: idMap.values()) {
						if (br instanceof ResultsPanelBrowser)
							registrar.unregisterService((ResultsPanelBrowser)br, CytoPanelComponent.class);
					}
				}
				registrar.unregisterService(browser, CytoPanelComponent.class);
				cytoPanel.setState(CytoPanelState.DOCK);
			}
		});
	}

	public  CyBrowser getBrowser(String id) {
		if (idMap.containsKey(id))
			return idMap.get(id);
		return null;
	}

	public void addBrowser(CyBrowser browser, String id) {
		idMap.put(id, browser);
	}

	public void removeBrowser(String id) {
		idMap.remove(id);
	}
}
