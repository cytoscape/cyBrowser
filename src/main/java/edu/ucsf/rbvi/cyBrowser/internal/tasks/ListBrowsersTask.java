package edu.ucsf.rbvi.cyBrowser.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;


import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.ResultsPanelBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class ListBrowsersTask extends AbstractTask implements ObservableTask {

	final CyBrowserManager manager;

	public ListBrowsersTask(CyBrowserManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, JSONResult.class, List.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		Map<String, CyBrowser> map = manager.getBrowserMap();
		if (type.equals(String.class)) {
			String res = "";
			if (map != null) {
				for (String id: map.keySet()) {
					CyBrowser b = map.get(id);
					res += id+": ";
					if (b.getTitle() != null) res += b.getTitle()+" ";
					if (b.getURL() != null) res += "("+b.getURL()+") ";
					res += "\n";
				}
			}
			return (R)res;
		} else if (type.equals(JSONResult.class)) {
			JSONResult res = () -> { 
				if (map == null) 
					return "{}"; 

				String jsonRes = "[";
				for (String id: map.keySet()) {
					CyBrowser b = map.get(id);
					jsonRes += "{\"id\": "+id;
					if (b.getTitle() != null) jsonRes += ", \"title\":\""+b.getTitle()+"\"";
					if (b.getURL() != null) jsonRes += ", \"url\":\""+b.getURL()+"\"";
					jsonRes += "}\n";
				}
				jsonRes += "]";
				return jsonRes;
			};
			return (R)res;
		} else if (type.equals(List.class)) {
			return (R)new ArrayList<String>(map.keySet());
		}
		return null;
	}

}
