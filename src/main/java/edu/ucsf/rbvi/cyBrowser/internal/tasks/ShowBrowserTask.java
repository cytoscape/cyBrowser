package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingPanel;

public class ShowBrowserTask extends AbstractEmptyObservableTask {

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
	public String title;
	
	@Tunable (description="Window Icon", 
	          longDescription="ID of the icon to be shown when using the browser window as a CytoPanelComponent",
	          exampleStringValue="MyApp::MY_ICON_NAME",
	          context="nogui")
	public String iconId;

	@Tunable (description="Window ID",
	          longDescription="The ID for this browser window.  Use this with ``cybrowser hide`` to hide the browser",
	          exampleStringValue="Window 1",
	          context="nogui")
	public String id;

	@Tunable (description="Browser panel",
	          longDescription="The panel to put this browser into",
	          exampleStringValue="EAST (Result)",
	          context="nogui")
	public ListSingleSelection<String> panel = new ListSingleSelection<String>("Result","Table","Command","EAST","SOUTH","WEST");


	@Tunable (description="Focus",
	          longDescription="The default is true. If true, the new cybrower panel will be selected.",
	    	  context="nogui")
	public Boolean focus = true;

	private final CyBrowserManager manager;
	private final CyServiceRegistrar registrar;

	public ShowBrowserTask(CyBrowserManager manager, CyServiceRegistrar registrar) {
		this.manager = manager;
		this.registrar = registrar;
		panel.setSelectedValue("Result");
	}

	@Override
	public void run(TaskMonitor monitor) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				var br = manager.getBrowser(id);
				
				if (id == null) {
					if (title != null)
						id = title;
					else
						id = manager.makeId();
				}
				
				var icon = iconId != null ? registrar.getService(IconManager.class).getIcon(iconId) : null;
				
        // long startTime = System.currentTimeMillis();
				ResultsPanelBrowser browser;
        // System.out.println("Creating browser");
				if (br != null && br instanceof ResultsPanelBrowser) {
					browser = (ResultsPanelBrowser) br;
				} else if (br != null && br instanceof SwingBrowser) {
					SwingPanel swingPanel = br.getPanel(id);
					((SwingBrowser) br).setVisible(false);
					browser = new ResultsPanelBrowser(manager, id, swingPanel, title, icon, getCytoPanel());
				} else {
					browser = new ResultsPanelBrowser(manager, id, null, title, icon, getCytoPanel());
				}
        // long time = System.currentTimeMillis();
        // System.out.println("Creating browser -- done ("+(time-startTime)+")");

				if (url != null && url.length() > 3) {
					browser.loadURL(url);
				} else if (text != null) {
          // System.out.println("Loading text");
					browser.loadText(text);
				}
        // long time2 = System.currentTimeMillis();
        // System.out.println("Loading text -- done ("+(time2-time)+")");

        // System.out.println("Registering browser");
				manager.registerCytoPanel(browser, focus);
        // long time3 = System.currentTimeMillis();
        // System.out.println("Registering browser -- done ("+(time3-time2)+")");
				br = browser;

				manager.addBrowser(br, id);
			}
		});

	}

  private CytoPanelName getCytoPanel() {
    String p = panel.getSelectedValue();
    if (p.equalsIgnoreCase("Result") || p.equalsIgnoreCase("EAST"))
      return CytoPanelName.EAST;
    if (p.equalsIgnoreCase("Table") || p.equalsIgnoreCase("SOUTH"))
      return CytoPanelName.SOUTH;
    if (p.equalsIgnoreCase("Command") || p.equalsIgnoreCase("WEST"))
      return CytoPanelName.WEST;

    return CytoPanelName.EAST;
  }

	@ProvidesTitle
	public String getTitle() {
		return "Showing Results Panel Browser";
	}

	@Override
	public <R> R getResults(Class<? extends R> type) {
		return getIDResults(type, id);
	}
}
