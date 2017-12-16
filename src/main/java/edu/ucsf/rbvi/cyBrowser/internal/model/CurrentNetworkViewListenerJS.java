package edu.ucsf.rbvi.cyBrowser.internal.model;

import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.service.util.CyServiceRegistrar;

import org.apache.log4j.Logger;


public class CurrentNetworkViewListenerJS extends JSListener implements SetCurrentNetworkViewListener {

	CurrentNetworkViewListenerJS(WebEngine engine, String callback) {
		super(engine, callback);
	}

	@Override
	public void handleEvent(SetCurrentNetworkViewEvent e) {
		CyNetworkView view = e.getNetworkView();
		doCallback(callback, "{\"suid\":"+view.getSUID()+"}");
	}
}
