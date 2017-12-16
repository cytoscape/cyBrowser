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
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingPanel;

public class SendTask extends AbstractTask implements ObservableTask {

	final CyBrowserManager manager;
	String jsReturn;

	@Tunable(description="Script to execute in browser", 
	         longDescription="A string that represents a JavaScript variable, script, or call "+
	                         "to be executed in the browser.  Note that only string results are "+
	                         "returned",
	         exampleStringValue="navigator.userAgent;",
	         context="nogui")
	public String script;

	@Tunable (description="Window ID", 
	          longDescription="The ID for the browser window to close",
	          exampleStringValue="Window 1",
	          context="nogui")
	public String id = null;

	public SendTask(CyBrowserManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		CyBrowser browser = manager.getBrowser(id);
		SwingPanel panel = browser.getPanel(id);
		jsReturn = panel.execute(script);
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, JSONResult.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		Map<String, CyBrowser> map = manager.getBrowserMap();
		if (type.equals(String.class)) {
			if (jsReturn != null)
				return (R)jsReturn.toString();
			return (R)null;
		} else if (type.equals(JSONResult.class)) {
			JSONResult res = () -> { 
				if (jsReturn == null) 
					return "{}"; 

				// Check to see if the result is a JSON string
				String jsonReturn = "{\"browserId\":\""+id+"\", \"result\": ";
				if (jsReturn.startsWith("[") || jsReturn.startsWith("{"))
					return jsonReturn+jsReturn+"}";
				return jsonReturn+"\""+jsReturn+"\"}";
			};
			return (R)res;
		}
		return null;
	}

}
