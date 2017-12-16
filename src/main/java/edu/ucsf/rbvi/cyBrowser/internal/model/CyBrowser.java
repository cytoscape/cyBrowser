package edu.ucsf.rbvi.cyBrowser.internal.model;
import netscape.javascript.JSObject;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingPanel;

public interface CyBrowser {
	public void loadURL(String url);
	public void loadText(String text);
	public String getTitle(String id);
	public String getURL(String id);
	public SwingPanel getPanel(String id);
}
