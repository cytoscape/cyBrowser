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
import org.cytoscape.util.swing.OpenBrowser;


import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class NativeBrowserTask extends AbstractEmptyObservableTask {

  @Tunable (description="URL",
	          longDescription="The URL the browser should load",
	          exampleStringValue="http://www.cytoscape.org",
	          gravity=1.0)
	public String url;

	final CyBrowserManager manager;
  final CyServiceRegistrar serviceRegistrar;

	public NativeBrowserTask(CyBrowserManager manager, CyServiceRegistrar serviceRegistrar) {
		this.manager = manager;
    this.serviceRegistrar = serviceRegistrar;
	}

	public void run(TaskMonitor monitor) {
    OpenBrowser openBrowser = serviceRegistrar.getService(OpenBrowser.class);
    openBrowser.openURL(url, false);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Open in Native Browser";
	}
}
