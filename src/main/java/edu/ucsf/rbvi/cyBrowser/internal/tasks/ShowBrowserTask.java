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

public class ShowBrowserTask extends AbstractTask {

	@Tunable (description="URL", gravity=1.0)
	public String url;

	@Tunable (description="HTML Text", context="nogui")
	public String text;

	@Tunable (description="Window Title", context="nogui")
	public String title = null;

	@Tunable (description="Window ID", context="nogui")
	public String id = null;

	final CyServiceRegistrar registrar;
	final CyBrowserManager manager;
  final CytoPanel cytoPanel = null;

	public ShowBrowserTask(CyServiceRegistrar registrar, CyBrowserManager manager) {
		this.registrar = registrar;
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CyBrowser br = manager.getBrowser(id);
				ResultsPanelBrowser browser;
				if (br != null && br instanceof ResultsPanelBrowser)
					browser = (ResultsPanelBrowser) br;
				else
					browser = new ResultsPanelBrowser(registrar, title);

				if (url != null && url.length() > 3) {
					browser.loadURL(url);
				} else if (text != null) {
					browser.loadText(text);
				}
				manager.registerCytoPanel(browser);
				br = (CyBrowser) browser;

				manager.addBrowser(br, id);
			}
		});

	}

	@ProvidesTitle
	public String getTitle() {
		return "Showing Results Panel Browser";
	}
}
