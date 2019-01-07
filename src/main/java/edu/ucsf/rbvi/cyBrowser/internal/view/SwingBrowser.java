package edu.ucsf.rbvi.cyBrowser.internal.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.UIManager;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

public class SwingBrowser extends JFrame implements CyBrowser, ChangeListener {

	private final CyBrowserManager manager;
	private SwingPanel currentPanel;
	// private final String id;
	private final Map<String,SwingPanel> idMap;
	private final Map<String,JButton> buttonMap;
	private final Map<String,BrowserTab> tabMap;
	private String initialTitle = null;
	private JTabbedPane tabbedPane = null;

	public SwingBrowser(CyBrowserManager manager, String id, SwingPanel reuse, String title, 
	                    boolean showDebug) {
		super();
		this.manager = manager;
		if (title != null) {
			setTitle(title);
			this.initialTitle = title;
		} else {
			setTitle("CyBrowser");
		}

		idMap = new HashMap<>();
		buttonMap = new HashMap<>();
		tabMap = new HashMap<>();
		this.currentPanel = new SwingPanel(manager, id, this, reuse, true, showDebug);
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(this);
		getContentPane().add(tabbedPane);
		addTab(id, currentPanel);

		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				currentPanel.loadURL(null);
				manager.removeBrowser(SwingBrowser.this);
			}
		});

		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		JFrame desktop = manager.getRegistrar().getService(CySwingApplication.class).getJFrame();
		setLocationRelativeTo(desktop);
	}

	public SwingBrowser(CyBrowserManager manager, String id, String title, 
	                    boolean showDebug) {
		super();
		this.manager = manager;
		if (title != null) {
			setTitle(title);
			this.initialTitle = title;
		} else {
			setTitle("CyBrowser");
		}

		idMap = new HashMap<>();
		buttonMap = new HashMap<>();
		tabMap = new HashMap<>();
		currentPanel = new SwingPanel(manager, id, this, null, true, showDebug);

		tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(this);
		getContentPane().add(tabbedPane);
		addTab(id, currentPanel);

		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				currentPanel.loadURL(null);
				manager.removeBrowser(id);
			}
		});

		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		JFrame desktop = manager.getRegistrar().getService(CySwingApplication.class).getJFrame();
		setLocationRelativeTo(desktop);
	}

	public String getTitle(String id) { 
		if (id == null)
			return initialTitle;
		if (idMap.containsKey(id)) 
			return idMap.get(id).getTitle();
		return null;
	}

	public String getURL(String id) {
		if (id == null)
			return currentPanel.getURL();
		if (idMap.containsKey(id)) 
			return idMap.get(id).getURL();
		return null;
	}

	public SwingPanel getPanel(String id) { 
		if (id == null)
			return currentPanel; 
		if (idMap.containsKey(id)) 
			return idMap.get(id);
		return null;
	}

	public void loadURL(final String url, boolean newTab) {
		loadURL(url, newTab, null);
	}

	public void loadURL(final String url, boolean newTab, String tabID) {
		if (!newTab || currentPanel.getURL() == null) {
			loadURL(url);
			if (tabID != null)
				idMap.put(tabID, currentPanel);
		} else {
			if (tabID == null)
				tabID = currentPanel.getId() + " "+manager.browserCount;
			if (!idMap.containsKey(tabID)) {
				manager.browserCount++;
				addTab(tabID, currentPanel.getTitle() + " "+manager.browserCount, false);
			} else {
				currentPanel = idMap.get(tabID);
				tabbedPane.setSelectedComponent(currentPanel);
			}
			loadURL(url);
		}
	}

	public void loadURL(final String url) {
		currentPanel.loadURL(url);
		// Initialize the title.  This will be the title that's used
		// if the page doesn't have one
		try {
			URL u = new URL(url);
			String urlTitle = u.getHost()+u.getPath();

			int tab = tabbedPane.indexOfComponent(currentPanel);
			tabMap.get(currentPanel.getId()).setTitle(urlTitle);
		} catch(Exception e) {}
	}

	public void loadText(final String text) {
		currentPanel.loadText(text);
	}

	public void loadText(final String text, boolean newTab) {
		loadText(text, newTab, null);
	}

	public void loadText(final String text, boolean newTab, String tabID) {
		if (!newTab || currentPanel.getText() == null) {
			currentPanel.loadText(text);
			if (tabID != null)
				idMap.put(tabID, currentPanel);
		} else {
			if (tabID == null)
				tabID = currentPanel.getId() + " "+manager.browserCount;

			System.out.println("tabID = "+tabID);
			if (!idMap.containsKey(tabID)) {
				manager.browserCount++;
				addTab(tabID, currentPanel.getTitle() + " "+manager.browserCount, false);
			} else {
				currentPanel = idMap.get(tabID);
				tabbedPane.setSelectedComponent(currentPanel);
			}
			currentPanel.loadText(text);
		}
	}

	public void setTitle(String id, String title) {
		if (idMap.containsKey(id)) {
			SwingPanel panel = idMap.get(id);
			tabMap.get(id).setTitle(title);

			if (panel.equals(currentPanel))
				setTitle(title);
		}
	}

	public void addTab(String id, String title, boolean showDebug) {
		currentPanel = new SwingPanel(manager, id, this, null, true, showDebug);
		addTab(id, currentPanel);
	}

	private void addTab(String id, SwingPanel panel) {
		tabbedPane.addTab(panel.getTitle(), panel);
		idMap.put(id, panel);
		String title = panel.getTitle();
		if (title == null || title.length() == 0)
			title = initialTitle;

		// Get the tab
		int tab = tabbedPane.indexOfComponent(panel);
		BrowserTab newTab = new BrowserTab(title, id);
		tabMap.put(id, newTab);
		tabbedPane.setTabComponentAt(tab, newTab);
		tabbedPane.setSelectedComponent(panel);

		if (idMap.size() > 1) {
			for (String btnId: buttonMap.keySet()) {
				buttonMap.get(btnId).setEnabled(true);
			}
		}

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		currentPanel = (SwingPanel)tabbedPane.getSelectedComponent();
		if (currentPanel == null) return;
		String ttl = currentPanel.getTitle();
		if (ttl != null)
			setTitle(ttl);
		else
			setTitle("CyBrowser");
	}

	public class CloseActionHandler implements ActionListener {
		String id;
		public CloseActionHandler(String id) {
			this.id = id;
		}

		public void actionPerformed(ActionEvent evt) {
			SwingPanel panel = idMap.get(id);
			if (panel != null) {
				panel.loadURL(null);
				tabbedPane.remove(panel);
				idMap.remove(id);
				buttonMap.remove(id);
				manager.removeBrowser(id);
				currentPanel = (SwingPanel)tabbedPane.getSelectedComponent();
				if (idMap.size() == 1)
					buttonMap.get(currentPanel.getId()).setEnabled(false);
			}
		}
	}

	public class BrowserTab extends JPanel {
		JLabel lblTitle;
		String id;

		public BrowserTab (String title, String id) {
			super(new GridBagLayout());

			setOpaque(false);
			if (title == null || title.length() == 0)
				title = "Empty Tab";
			int len = Math.min(title.length(), 20);
			lblTitle = new JLabel(title.substring(0, len));
			lblTitle.setFont(lblTitle.getFont().deriveFont(10.0f));
			JButton btnClose = new JButton(IconManager.ICON_CLOSE); 
			btnClose.setFont(manager.getRegistrar().getService(IconManager.class).getIconFont(8.0f));
			// JButton btnClose = new JButton(UIManager.getIcon("InternalFrame.closeIcon")); 
			buttonMap.put(id, btnClose);
	
			// Always start out false
			if (idMap.size() > 1)
				btnClose.setEnabled(true);
			else
				btnClose.setEnabled(false);
	
			// btnClose.setFont(manager.getRegistrar().getService(IconManager.class).getIconFont(12.0f));
			btnClose.setBorderPainted(false);
			btnClose.setContentAreaFilled(false);
			btnClose.setFocusPainted(false);
			btnClose.setBorder(BorderFactory.createEmptyBorder(1,0,1,1));
			// btnClose.setText(IconManager.ICON_CLOSE);
	
			btnClose.addActionListener(new CloseActionHandler(id));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0;
	
			add(btnClose, gbc);
	
			gbc.gridx++;
			gbc.weightx = 1;
			add(lblTitle, gbc);
		}

		public void setTitle(String title) {
			lblTitle.setText(title);
		}

		public void setId(String id) {
			JButton btnClose = buttonMap.get(this.id);
			this.id = id;
			buttonMap.put(this.id, btnClose);
			btnClose.addActionListener(new CloseActionHandler(id));
		}
	}
}
