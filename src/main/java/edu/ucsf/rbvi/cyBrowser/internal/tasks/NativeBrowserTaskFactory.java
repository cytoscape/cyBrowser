package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.service.util.CyServiceRegistrar;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class NativeBrowserTaskFactory extends AbstractTaskFactory {

	final CyBrowserManager manager;
  final CyServiceRegistrar serviceRegistrar;

	public NativeBrowserTaskFactory(CyBrowserManager manager, CyServiceRegistrar serviceRegistrar) {
		this.manager = manager;
    this.serviceRegistrar = serviceRegistrar;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new NativeBrowserTask(manager, serviceRegistrar));
	}
}
