package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import java.util.Properties;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;


import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class HideBrowserTask extends AbstractTask {

	@Tunable (description="Window ID", context="nogui")
	public String id = null;

	final CyServiceRegistrar registrar;
	final CyBrowserManager manager;

	public HideBrowserTask(CyServiceRegistrar registrar, CyBrowserManager manager) {
		this.registrar = registrar;
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		CyBrowser browser = manager.getBrowser(id);
		if (browser instanceof ResultsPanelBrowser)
			manager.unregisterCytoPanel((ResultsPanelBrowser)browser);
		manager.removeBrowser(id);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Hiding Cytoscape Web Browser";
	}
}
