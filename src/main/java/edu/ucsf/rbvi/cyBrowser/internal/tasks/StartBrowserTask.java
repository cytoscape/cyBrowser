package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import javax.swing.SwingUtilities;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class StartBrowserTask extends AbstractTask {

	@Tunable (description="URL", gravity=1.0)
	public String url;
	CyServiceRegistrar registrar;

	public StartBrowserTask(CyServiceRegistrar registrar) {
		this.registrar = registrar;
	}

	public void run(TaskMonitor monitor) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SwingBrowser browser = new SwingBrowser(registrar);
				browser.setVisible(true);
				if (url != null && url.length() > 3) {
					browser.loadURL(url);
				}
			}
		});

	}

	@ProvidesTitle
	public String getTitle() {
		return "Starting Cytoscape Web Browser";
	}
}
