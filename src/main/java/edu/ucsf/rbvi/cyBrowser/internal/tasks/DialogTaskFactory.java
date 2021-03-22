package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class DialogTaskFactory extends AbstractTaskFactory {

	final CyBrowserManager manager;
  final String url;
  Map<String, Object> args;
  CommandExecutorTaskFactory ceTF;

	public DialogTaskFactory(CyBrowserManager manager, String defaultURL) {
		this.manager = manager;
    url = defaultURL;
    ceTF = manager.getRegistrar().getService(CommandExecutorTaskFactory.class);
    args = new HashMap<>();
    args.put("url", url);
    args.put("resultsPanel", Boolean.FALSE);
	}

	public DialogTaskFactory(CyBrowserManager manager) {
		this.manager = manager;
    url = null;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
    if (url == null)
		  return new TaskIterator(new DialogTask(manager));

    return ceTF.createTaskIterator("cybrowser", "dialog", args, null);
	}
}

