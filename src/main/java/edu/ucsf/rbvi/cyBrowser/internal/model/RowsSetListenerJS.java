package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.util.List;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import org.apache.log4j.Logger;


public class RowsSetListenerJS extends JSListener implements RowsSetListener {
	CyTable table = null;
	WebEngine engine;
	String callback;
	CyNetwork network;
	Class<? extends CyIdentifiable> type;

	RowsSetListenerJS(WebEngine engine, CyNetwork network, String callback, Class<? extends CyIdentifiable> type) {
		if (network != null)
			table = network.getTable(type, CyNetwork.LOCAL_ATTRS);
		this.engine = engine;
		this.callback = callback;
		this.network = network;
		this.type = type;
	}

	@Override
	public void handleEvent(RowsSetEvent e) {
		if (!e.containsColumn(CyNetwork.SELECTED)) return;
		if (table != null && e.getSource() != table) return;
		List<? extends CyIdentifiable> selection;
		if (type.equals(CyNode.class))
	 		selection = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		else if (type.equals(CyEdge.class))
	 		selection = CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true);
		else
			return;

		String s = "[";
		for (CyIdentifiable id: selection) {
			s += toJSON(id)+",";
		}
		if (s.length() > 1)
			s = s.substring(0, s.length()-1);
		s += "]";

		final String idString = s;

		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
				JSObject windowObject = (JSObject)engine.executeScript("window");
				windowObject.call(callback, idString);
			}
		});
	}

	private String toJSON(CyIdentifiable id) {
		return "{\"suid\":"+id.getSUID()+",\"name\":\""+network.getRow(id).get(CyNetwork.NAME, String.class)+"\"}";
	}
}
