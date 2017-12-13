package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.Properties;

import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedListener;

import org.apache.log4j.Logger;


public class JSListenerFactory {
	public static JSListener createListener(CyServiceRegistrar registrar, WebEngine engine, 
	                                        String type, String callback) {
		JSListener.ListenerType listenerType = JSListener.ListenerType.getType(type);
		if (listenerType == null) return null;
		switch(listenerType) {
		case NETWORKLOADED:
			{
				NetworkAddedListenerJS listener = new NetworkAddedListenerJS(engine, callback);
				registrar.registerService(listener, NetworkAddedListener.class, new Properties());
				return listener;
			}
		case SESSIONLOADED:
			{
				SessionLoadedListenerJS listener = new SessionLoadedListenerJS(engine, callback);
				registrar.registerService(listener, SessionLoadedListener.class, new Properties());
				return listener;
			}
		case NODESELECTION:
		case EDGESELECTION:
			return createListener(registrar, engine, type, null, callback);
		default:
			return null;
		}
	}

	public static JSListener createListener(CyServiceRegistrar registrar, WebEngine engine, String type, 
	                                        CyNetwork network, String callback) {
		JSListener.ListenerType listenerType = JSListener.ListenerType.getType(type);
		if (listenerType == null) return null;

		Class<? extends CyIdentifiable> objType = null;
		switch(listenerType) {
		case NODESELECTION:
			objType = CyNode.class;
			break;
		case EDGESELECTION:
			objType = CyEdge.class;
			break;
		default:
			return null;
		}
		RowsSetListenerJS listener = new RowsSetListenerJS(engine, network, callback, objType);
		registrar.registerService(listener, RowsSetListener.class, new Properties());
		return listener;
	}
}
