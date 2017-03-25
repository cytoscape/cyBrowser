package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class ShowBrowserTaskFactory extends AbstractTaskFactory {

	CyServiceRegistrar registrar;
	final CyBrowserManager manager;

	public ShowBrowserTaskFactory(CyServiceRegistrar registrar, CyBrowserManager manager) {
		this.registrar = registrar;
		this.manager = manager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowBrowserTask(registrar, manager));
	}
}

