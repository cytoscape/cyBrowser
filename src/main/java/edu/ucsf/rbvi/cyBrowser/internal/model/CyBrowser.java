package edu.ucsf.rbvi.cyBrowser.internal.model;
import edu.ucsf.rbvi.cyBrowser.internal.view.SwingPanel;

public interface CyBrowser {
	public void loadURL(String url);
	public void loadText(String text);
	public String getTitle();
	public String getURL();
	public SwingPanel getPanel();
}
