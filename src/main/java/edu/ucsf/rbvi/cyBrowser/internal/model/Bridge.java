package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import javax.swing.SwingUtilities;

import netscape.javascript.JSObject;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.json.JSONResult;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

public class Bridge implements TaskObserver {
	private String callbackMethod = null;
	private final WebEngine engine;
	private final CommandExecutorTaskFactory commandTaskFactory;
	private final StringToModel stringToModel;
	private final SynchronousTaskManager taskManager;
	private final CyServiceRegistrar registrar;
	private List<JSListener> listeners;

	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	
	public Bridge(final WebEngine engine, final CyServiceRegistrar registrar) {
		this.engine = engine;

		// Get the services we'll need
		commandTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		taskManager = registrar.getService(SynchronousTaskManager.class);
		stringToModel = registrar.getService(StringToModel.class);
		listeners = new ArrayList<JSListener>();
		this.registrar = registrar;
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
		// System.out.println("All tasks finished");
		callbackMethod = null;
	}

	@Override
	public void taskFinished(ObservableTask task) {
		String results = task.getResults(JSONResult.class).getJSON();
		// System.out.println("Task "+task+" finished: "+results);
		logger.info("CyBrowser: results: '"+results+"'");
		if (callbackMethod != null) {
			String cb = callbackMethod; // 
			callbackMethod = null;
			Platform.runLater(new Runnable() {
				@Override public void run() {
					System.out.println("Executing: "+cb+"(`"+results+"`)");
					// We need to use templated strings to preserve newlines
					// engine.executeScript(cb+"(`"+results+"`)");
					JSObject jsobj = (JSObject) engine.executeScript("window");
					jsobj.call(cb, results);
				}
			});
		}
	}

	public void executeCommand(String command) {
		TaskObserver observer = this;
		// System.out.println("command = "+command);
		logger.info("CyBrowser: executing command: '"+command+"'");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					TaskIterator commandTasks = commandTaskFactory.createTaskIterator(observer, command);
					taskManager.execute(commandTasks, observer);
				} catch (Exception e) {
					logger.error("CyBrowser: error processing command: "+e.getMessage());
				}
			}
		});
	}

	public void executeCyCommand(String command) {
		System.out.println("Bridge: executing '"+command+"'");
		executeCommand(command);
		callbackMethod = null;
	}

	public void executeCyCommandWithResults(String command, String callback) {
		System.out.println("Bridge: executing '"+command+"' with results");
		executeCommand(command);
		callbackMethod = callback;
	}

	/**
	 * Register a listener for network-specific changes such as node and edge selection.
	 *
	 * Listener types:
	 *   * nodeSelection: node selection listener
	 *   * edgeSelection: edge selection listener
	 * window.cybrowser.registerListener(["nodeSelection","edgeSelection"], "myNetwork", callback); 
	 */
	public void registerSelectionListeners(String types, String network, String callback) {
		// for each type:
		//   listeners.add(new JSListener(engine, type, network, listener));

		CyNetwork net = null;
		if (network != null)
			net = stringToModel.getNetwork(network);
		String[] typeArray = types.split(",");
		for (String type: typeArray) {
			JSListener listener = JSListenerFactory.createListener(registrar, engine, type, net, callback);
			listeners.add(listener);
		}
	}

	/**
	 * Register a listener for general Cytoscape changes.
	 *
	 * Listener types:
	 *   * currentNetwork: called when the current network changes
	 *   * currentNetworkView: called when the current network view changes
	 *   * networkLoaded: called when a new network is loaded
	 *   * sessionLoaded: called when a new session is loaded
	 */
	public void registerListeners(String types, String callback) {
		String[] typeArray = types.split(",");
		for (String type: typeArray) {
			JSListener listener = JSListenerFactory.createListener(registrar, engine, type, callback);
			listeners.add(listener);
		}
	}

	public void clearListeners() {
		for (JSListener l: listeners) {
			registrar.unregisterAllServices(l);
		}
		listeners.clear();
	}
}
