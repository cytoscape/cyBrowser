package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

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

import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;

public class Bridge {
	private final WebEngine engine;
	private final CommandExecutorTaskFactory commandTaskFactory;
	private final StringToModel stringToModel;
	private final SynchronousTaskManager taskManager;
	private final CyServiceRegistrar registrar;
	private final SwingBrowser parent;
	private List<JSListener> listeners;

	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	
	public Bridge(final WebEngine engine, final CyServiceRegistrar registrar, final SwingBrowser parent) {
		this.engine = engine;
		this.parent = parent;

		// Get the services we'll need
		commandTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		taskManager = registrar.getService(SynchronousTaskManager.class);
		stringToModel = registrar.getService(StringToModel.class);
		listeners = new ArrayList<JSListener>();
		this.registrar = registrar;
	}

  // Hook to pick up console.log messages from javascript.  Note
  // that this can be overridden by users
  public void log(String text) {
    // System.out.println("Browser console.log: "+text);
    logger.info("cyBrowser: "+text);
  }

	public void executeCommand(String command, String callbackMethod) {
		TaskObserver observer = new CallbackObserver(callbackMethod);
		// System.out.println("command = "+command);
		logger.info("CyBrowser: executing command: '"+command+"'");
		// SwingUtilities.invokeLater(new Runnable() {
		Runnable runnableTask = () ->  {
				try {
					TaskIterator commandTasks = commandTaskFactory.createTaskIterator(observer, command);
					taskManager.execute(commandTasks, observer);
				} catch (Exception e) {
					logger.error("CyBrowser: error processing command: "+e.getMessage());
				}
		};

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(runnableTask);
		/*
		if (SwingUtilities.isEventDispatchThread()) {
			runnableTask.run();
		} else {
			SwingUtilities.invokeLater(runnableTask);
		}
		*/
	}

	public void executeCyCommand(String command) {
		logger.info("Bridge: executing command: '"+command+"'");
		executeCommand(command, null);
	}

	public void executeCyCommandWithResults(String command, String callback) {
		// System.out.println("Bridge: executing command: '"+command+"' with results");
		logger.info("Bridge: executing command: '"+command+"' with results");
		executeCommand(command, callback);
	}

  public void cyLog(String message) {
		System.out.println(message);
    logger.info(message);
  }

	public void downloadFile(String href, String fileName) {
			Downloader.download(registrar, parent, href, fileName, true);
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

  class CallbackObserver implements TaskObserver {

    String callbackMethod;

    public CallbackObserver(String callbackMethod) {
      this.callbackMethod = callbackMethod;
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
      // System.out.println("All tasks finished");
    }

    @Override
    public void taskFinished(ObservableTask task) {
      String results = task.getResults(JSONResult.class).getJSON();
      // System.out.println("Task "+task+" finished: "+results);
      // logger.info("CyBrowser: results: '"+results+"'");
      if (this.callbackMethod != null) {
        String cb = this.callbackMethod; // 
        Platform.runLater(new Runnable() {
          @Override public void run() {
            // System.out.println("Executing: "+cb+"(`"+results+"`)");
            // We need to use templated strings to preserve newlines
            // engine.executeScript(cb+"(`"+results+"`)");
            JSObject jsobj = (JSObject) engine.executeScript("window");
            jsobj.call(cb, results);
          }
        });
      }
    }
	}
}
