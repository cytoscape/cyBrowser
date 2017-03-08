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


import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class StartBrowserTask extends AbstractTask {

	@Tunable (description="URL", gravity=1.0)
	public String url;

	@Tunable (description="HTML Text", context="nogui")
	public String text;

	final CyServiceRegistrar registrar;
	final CyBrowserManager manager;
	final boolean dialog;
  final CytoPanel cytoPanel = null;

	public StartBrowserTask(CyServiceRegistrar registrar, CyBrowserManager manager, boolean dialog) {
		this.registrar = registrar;
		this.manager = manager;
		this.dialog = dialog;
	}

	public void run(TaskMonitor monitor) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (dialog) {
					SwingBrowser browser = new SwingBrowser(registrar);
					browser.setVisible(true);
					if (url != null && url.length() > 3) {
						browser.loadURL(url);
					}
				} else {
					ResultsPanelBrowser browser = new ResultsPanelBrowser(registrar, null);
					if (url != null && url.length() > 3) {
						browser.loadURL(url);
					} else if (text != null) {
						browser.loadText(text);
					}
					manager.registerCytoPanel(browser);
				}
			}
		});

	}

	@ProvidesTitle
	public String getTitle() {
		return "Starting Cytoscape Web Browser";
	}
}
