package edu.ucsf.rbvi.cyBrowser.internal.model;

import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;

import org.apache.log4j.Logger;


public class CurrentNetworkListenerJS extends JSListener implements SetCurrentNetworkListener {

	CurrentNetworkListenerJS(WebEngine engine, String callback) {
		super(engine, callback);
	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {
		CyNetwork network = e.getNetwork();
		doCallback(callback, "{\"suid\":"+network.getSUID()+"}");
	}
}
