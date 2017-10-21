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
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingPanel;

public class DialogTask extends AbstractEmptyObservableTask {

	@Tunable (description="URL", 
	          longDescription="The URL the browser should load",
	          exampleStringValue="http://www.cytoscape.org",
	          gravity=1.0)
	public String url;

	@Tunable (description="HTML Text", 
	          longDescription="HTML text to initially load into the browser",
	          exampleStringValue="<HTML><HEAD><TITLE>Hello</TITLE></HEAD><BODY>Hello, world!</BODY></HTML>",
	          context="nogui")
	public String text;

	@Tunable (description="Window Title", 
	          longDescription="Text to be shown in the title bar of the browser window",
	          exampleStringValue="Cytoscape Home Page",
	          context="nogui")
	public String title = null;

	@Tunable (description="Window ID", 
	          longDescription="The ID for this browser window.  Use this with ``cybrowser hide`` to hide the browser",
	          exampleStringValue="Window 1",
	          context="nogui")
	public String id = null;

	@Tunable (description="Show debug tools", 
	          longDescription="Whether or not to show the web programmer debugging tools",
	          exampleStringValue="false",
	          context="nogui")
	public boolean debug = false;

	final CyBrowserManager manager;
  final CytoPanel cytoPanel = null;

	public DialogTask(CyBrowserManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CyBrowser br = manager.getBrowser(id);
				SwingBrowser browser;
				if (br != null && br instanceof SwingBrowser)
					browser = (SwingBrowser) br;
				else if (br != null && br instanceof ResultsPanelBrowser) {
					SwingPanel panel = br.getPanel();
					manager.unregisterCytoPanel((ResultsPanelBrowser)br);
					browser = new SwingBrowser(manager, panel, title, debug);
				} else
					browser = new SwingBrowser(manager, title, debug);
				browser.setVisible(true);
				if (url != null && url.length() > 3) {
					browser.loadURL(url);
				}
				br = (CyBrowser) browser;
				if (id != null)
					manager.addBrowser(br, id);
			}
		});

	}

	@ProvidesTitle
	public String getTitle() {
		return "Starting Cytoscape Web Browser";
	}

	@Override
	public <R> R getResults(Class<? extends R> type) {
		return getIDResults(type, id);
	}
}
