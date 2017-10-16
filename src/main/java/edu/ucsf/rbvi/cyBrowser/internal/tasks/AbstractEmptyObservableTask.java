package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

abstract public class AbstractEmptyObservableTask extends AbstractTask implements ObservableTask {
	@Override
	abstract public void run(TaskMonitor monitor);

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) 
			return (R)"";
		else if (type.equals(JSONResult.class)) {
			JSONResult res = () -> { return "{}"; };
			return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, JSONResult.class);
	}
}
