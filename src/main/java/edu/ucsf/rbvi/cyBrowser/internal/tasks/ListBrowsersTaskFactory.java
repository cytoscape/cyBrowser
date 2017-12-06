package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class ListBrowsersTaskFactory extends AbstractTaskFactory {

	final CyBrowserManager manager;

	public ListBrowsersTaskFactory(CyBrowserManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ListBrowsersTask(manager));
	}
}

