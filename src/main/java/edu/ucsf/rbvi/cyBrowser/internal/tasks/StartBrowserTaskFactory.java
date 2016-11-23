package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class StartBrowserTaskFactory extends AbstractTaskFactory {

	CyServiceRegistrar registrar;

	public StartBrowserTaskFactory(CyServiceRegistrar registrar) {
		this.registrar = registrar;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new StartBrowserTask(registrar));
	}
}

