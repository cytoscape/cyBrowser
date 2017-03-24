package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.service.util.CyServiceRegistrar;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;

public class SwingBrowser extends JDialog implements CyBrowser {

	private final CyServiceRegistrar registrar;
	private final SwingPanel panel;
 
	public SwingBrowser(CyServiceRegistrar registrar, String title, boolean showDebug) {
		super();
		this.registrar = registrar;
		if (title != null) setTitle(title);
		panel = new SwingPanel(registrar, this, true, showDebug);
		getContentPane().add(panel);
		
		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
	}

	public void loadURL(final String url) {
		panel.loadURL(url);
	}

	public void loadText(final String text) {
		panel.loadText(text);
	}
}
