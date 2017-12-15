package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	String version = "unknown";
	public static int browserCount = 1;

	public CyBrowserManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
		this.cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		idMap = new HashMap<>();
	}

	public CyServiceRegistrar getRegistrar() {
		return registrar;
	}

	public void registerCytoPanel(ResultsPanelBrowser browser) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				registrar.registerService(browser, CytoPanelComponent.class, new Properties());
				cytoPanel.setState(CytoPanelState.DOCK);
				int index = cytoPanel.indexOfComponent(browser.getComponent());
				cytoPanel.setSelectedIndex(index);
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
		if (id == null) id = "";
		// System.out.println("Retrieving browser with id: '"+id+"'");
		if (idMap.containsKey(id)){
			// System.out.println("Found browser: "+idMap.get(id)+" with id: '"+id+"'");
			return idMap.get(id);
		}
		return null;
	}

	public String makeId() {
		String id = "CyBrowser "+browserCount;
		browserCount++;
		return id;
	}

	public void addBrowser(CyBrowser browser, String id) {
		if (id == null) {
			id = "CyBrowser "+browserCount;
			browserCount++;
		}
		// System.out.println("Adding browser: "+browser+" with id: '"+id+"'");
		idMap.put(id, browser);
	}

	public void removeBrowser(String id) {
		if (id == null) id = "";
		// System.out.println("Removing browser: '"+id+"'");
		idMap.remove(id);
	}

	public void removeBrowser(CyBrowser browser) {
		List<String> ids = new ArrayList<String>(idMap.keySet());
		for (String id: ids) {
			if (idMap.get(id).equals(browser))
				removeBrowser(id);
		}
	}

	// FIXME: This will need to change for tabbed browsing
	public void closeBrowser(String id) {
		CyBrowser browser = getBrowser(id);
		if (browser == null) {
			System.out.println("Unable to get browser for id: '"+id+"'");
			return;
		}
		browser.loadURL(null); // Clear the browser

		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				if (browser instanceof ResultsPanelBrowser)
					unregisterCytoPanel((ResultsPanelBrowser)browser);
				else if (browser instanceof SwingBrowser)
					((SwingBrowser)browser).dispose();
			}
		});
			
		removeBrowser(id);
	}

	public Map<String, CyBrowser> getBrowserMap() {
		return idMap;
	}

	public void setVersion(String v) { this.version = v; }
	public String getVersion() { return version; }
}
