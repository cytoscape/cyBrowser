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
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import org.apache.log4j.Logger;


public abstract class JSListener {
	public enum ListenerType {
		NODESELECTION("Node Selection","nodeSelection"),
		EDGESELECTION("Edge Selection","edgeSelection"),
		NETWORKLOADED("Network Loaded","networkLoaded"),
		SESSIONLOADED("Session Loaded","sessionLoaded");

		String name;
		String shortName;
		ListenerType(String name, String shortName) {
			this.name = name;
			this.shortName = shortName;
		}

		public String toString() {return shortName;}
		public String getName() {return name;}
		public static ListenerType getType(String shortName) {
			for (ListenerType lt: ListenerType.values()) {
				if (lt.shortName.equals(shortName)) return lt;
			}
			return null;
		}

	};

}
