package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class ShowBrowserTaskFactory extends AbstractTaskFactory {

	private final CyBrowserManager manager;
	private final CyServiceRegistrar registrar;

	public ShowBrowserTaskFactory(CyBrowserManager manager, CyServiceRegistrar registrar) {
		this.manager = manager;
		this.registrar = registrar;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowBrowserTask(manager, registrar));
	}
}
