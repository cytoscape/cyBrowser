package edu.ucsf.rbvi.cyBrowser.internal.model;

import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;

import org.apache.log4j.Logger;


public class SessionLoadedListenerJS extends JSListener implements SessionLoadedListener {
	WebEngine engine;
	String callback;

	SessionLoadedListenerJS(WebEngine engine, String callback) {
		this.engine = engine;
		this.callback = callback;
	}

	@Override
	public void handleEvent(SessionLoadedEvent e) {
		engine.executeScript(callback);
	}
}
