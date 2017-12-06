package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.cytoscape.service.util.CyServiceRegistrar;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class SwingBrowser extends JDialog implements CyBrowser {

	private final CyBrowserManager manager;
	private final SwingPanel panel;
	private final String id;
	private String initialTitle = null;

	public SwingBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title, 
	                    boolean showDebug) {
		super();
		this.manager = manager;
		if (title != null) {
			setTitle(title);
			this.initialTitle = title;
		}
		this.id = id;
		this.panel = new SwingPanel(manager, id, this, reuse, true, showDebug);
		getContentPane().add(panel);

		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				System.out.println("Window Closed");
				panel.loadURL(null);
				manager.removeBrowser(SwingBrowser.this);
			}
		});

		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
	}

	public SwingBrowser(CyBrowserManager manager, String id, String title, 
	                    boolean showDebug) {
		super();
		this.manager = manager;
		if (title != null) {
			setTitle(title);
			this.initialTitle = title;
		}
		this.id = id;
		panel = new SwingPanel(manager, id, this, null, true, showDebug);
		getContentPane().add(panel);

		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				System.out.println("Window Closed");
				panel.loadURL(null);
				manager.removeBrowser(id);
			}
		});

		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
	}

	public String getTitle() { return initialTitle; }

	public SwingPanel getPanel() { return panel; }

	public void loadURL(final String url) {
		panel.loadURL(url);
	}

	public String getURL() {
		return panel.getURL();
	}

	public void loadText(final String text) {
		panel.loadText(text);
	}
}
