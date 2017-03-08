package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.service.util.CyServiceRegistrar;

public class SwingBrowser extends JDialog {

	private final CyServiceRegistrar registrar;
	private final SwingPanel panel;
 
	public SwingBrowser(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		panel = new SwingPanel(registrar, this, true);
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
