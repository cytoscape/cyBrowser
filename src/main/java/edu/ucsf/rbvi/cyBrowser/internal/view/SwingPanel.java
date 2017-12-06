package edu.ucsf.rbvi.cyBrowser.internal.view;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLFrameElement;
import org.w3c.dom.html.HTMLIFrameElement;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.util.Callback;
 
import java.awt.BorderLayout;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import netscape.javascript.JSObject;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;
 
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;

import static javafx.concurrent.Worker.State.FAILED;

public class SwingPanel extends JPanel implements TaskObserver {
 
	protected JFXPanel jfxPanel = new JFXPanel();
	private WebEngine engine;
 
	private final JLabel lblStatus = new JLabel();

	public static final String EVENT_TYPE_CLICK = "click";
	public static final String EVENT_TYPE_CONTEXT_MENU = "contextmenu";

	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();
	private JButton btnBack = null;
	private JButton btnForward = null;

	private final CyServiceRegistrar registrar;
	private final CyBrowserManager manager;
	private final CommandExecutorTaskFactory commandTaskFactory;
	private final SynchronousTaskManager taskManager;
	private final JDialog parent;
	private final JPanel panel;
	private final boolean showURL;
	private final boolean showDebug;
	private String callbackMethod = null;
	private String url = null;
	private boolean suppressLink = false;

	// Three class variables to help with our context menus
	private String selection;
	private Element anchor;

	// Our ID
	private final String id;

	final Logger logger = Logger.getLogger(CyUserLog.NAME);
 
	public SwingPanel(CyBrowserManager manager, String id, JDialog parentDialog, SwingPanel reuse, 
	                  boolean showURL, boolean showDebug) {
		super(new BorderLayout());
		this.manager = manager;
		this.id = id;
		this.registrar = manager.getRegistrar();
		this.showURL = showURL;
		this.showDebug = showDebug;
		parent = parentDialog;
		panel = this;
		if (parent == null)
			setPreferredSize(new Dimension(200, 600));
		initComponents(reuse);
		Platform.setImplicitExit(false);

		// Get the services we'll need
		commandTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		taskManager = registrar.getService(SynchronousTaskManager.class);
	}

	public String getURL() {
		return engine.getLocation();
	}

	
	private void initComponents(SwingPanel reuse) {
		if (reuse == null)
			createScene();
		else {
			jfxPanel = reuse.jfxPanel;
			engine = reuse.engine;
		}
 
		if (showURL) {
			ActionListener al = new ActionListener() {
				@Override 
				public void actionPerformed(ActionEvent e) {
					loadURL(txtURL.getText());
				}
			};
	 
			txtURL.addActionListener(al);

			progressBar.setPreferredSize(new Dimension(150, 18));
			progressBar.setStringPainted(true);
  
			JPanel topBar = new JPanel(new BorderLayout(5, 0));
			topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			{
				JPanel leftButtonPanel = new JPanel(new FlowLayout());
				btnBack = createHistoryButton(IconManager.ICON_ARROW_LEFT, -1);
				btnForward = createHistoryButton(IconManager.ICON_ARROW_RIGHT, 1);
				leftButtonPanel.add(btnBack);
				leftButtonPanel.add(btnForward);
				topBar.add(leftButtonPanel, BorderLayout.WEST);
			}
			topBar.add(txtURL, BorderLayout.CENTER);
			{
				JPanel rightButtonPanel = new JPanel(new FlowLayout());
				JButton btnGo = new JButton("Go");
				btnGo.addActionListener(al);
				rightButtonPanel.add(btnGo);
				if (showDebug) {
					JButton btnBug = createBugButton();
					rightButtonPanel.add(btnBug);
				}
				topBar.add(rightButtonPanel, BorderLayout.EAST);
			}
 
			JPanel statusBar = new JPanel(new BorderLayout(5, 0));
			statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			statusBar.add(lblStatus, BorderLayout.CENTER);
			statusBar.add(progressBar, BorderLayout.EAST);
	 
			add(topBar, BorderLayout.NORTH);
			add(statusBar, BorderLayout.SOUTH);
		}
		add(jfxPanel, BorderLayout.CENTER);
	}

	private JButton createBugButton() {
		JButton btn = new JButton(IconManager.ICON_BUG);
		btn.setFont(registrar.getService(IconManager.class).getIconFont(14.0f));
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}"); 
					}
				});
			}
		});
		return btn;
	}

	private JButton createHistoryButton(String icon, int index) {
		JButton btn = new JButton(icon);
		btn.setFont(registrar.getService(IconManager.class).getIconFont(14.0f));
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						engine.getHistory().go(index);
						txtURL.setText(getUrlFromHistory());
						enableControls();
					}
				});
			}
		});
		btn.setEnabled(false);
		return btn;
	}

	private String getUrlFromHistory() {
		return engine.getHistory().getEntries().get(engine.getHistory().getCurrentIndex()).getUrl();
	}
 
	private void createScene() {
 
		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
 
				WebView view = new WebView();
				engine = view.getEngine();
				String userAgent = engine.getUserAgent();
				engine.setUserAgent(userAgent+" CyBrowser/"+manager.getVersion());

				view.setContextMenuEnabled(false);

				createContextMenu(view);

				engine.titleProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override 
							public void run() {
								if (parent != null)
									parent.setTitle(newValue);
							}
						});
					}
				});
 
				engine.setOnAlert(new EventHandler<WebEvent<String>>() {
					@Override 
					public void handle(final WebEvent<String> event) {
						Dialog<Void> alert = new Dialog<>();
						alert.setTitle("CyBrowser Alert");
						Label txt = new Label(event.getData());
						txt.setStyle("-fx-text-alignment: center;");
						// alert.getDialogPane().setContentText(event.getData());
						alert.getDialogPane().setContent(txt);
						alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
						alert.showAndWait();
					}
				});
 
				engine.setConfirmHandler(new Callback<String,Boolean>() {
					@Override 
					public Boolean call(String message) {
						Dialog<ButtonType> confirm = new Dialog<>();
						confirm.setTitle("CyBrowser Confirmation");
						confirm.getDialogPane().setContentText(message);
						confirm.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO );
						Optional<ButtonType> result = confirm.showAndWait();
						if (result.isPresent() && result.get() == ButtonType.YES)
							return true;
						return false;
					}
				});

				engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
					@Override 
					public void handle(final WebEvent<String> event) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override 
							public void run() {
								lblStatus.setText(event.getData());
							}
						});
					}
				});
 
				engine.locationProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> ov, 
					                    String oldValue, final String newValue) {
						// System.out.println("location changed");
						SwingUtilities.invokeLater(new Runnable() {
							@Override 
							public void run() {
								txtURL.setText(newValue);
							}
						});
					}
				});
 
				engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, 
					                    Number oldValue, final Number newValue) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override 
							public void run() {
								progressBar.setValue(newValue.intValue());
							}
						});
					}
				});

				engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
					@Override
					public void changed(ObservableValue ov, State oldState, State newState) {
						/*
						System.out.println("webEngine result "+newState.toString());
						*/
						if (newState == Worker.State.SUCCEEDED) {
							// OK, set up our callback
							JSObject jsobj = (JSObject) engine.executeScript("window");

							// Set member for 'window' object
							// In Javascript access: window.cybrowser...
							jsobj.setMember("cybrowser", new Bridge());

							// Now set up a listener for link click events
							EventListener listener = new EventListener() {
								@Override
								public void handleEvent(Event ev) {
									String domEventType = ev.getType();
									if (domEventType.equals(EVENT_TYPE_CLICK)) {
										// System.out.println("Dom click");
										if (suppressLink) {
											if (ev.getCancelable()) {
												ev.preventDefault();
												ev.stopPropagation();
											}
											suppressLink = false;
										} else {
											String href = ((Element)ev.getTarget()).getAttribute("href");
											if (href != null && href.startsWith("cycmd:")) {
												String command = href.substring("cycmd:".length());
												executeCommand(command);
											}
										}
									}
								}
							};

							Document doc = engine.getDocument();
							addListenersToAnchors(doc, listener);
							enableControls();
						} else if (newState == Worker.State.FAILED) {
								Alert alert = new Alert(AlertType.ERROR);
								alert.setTitle("Load failed");
								String alertText = "";
								String exceptionMessage = engine.getLoadWorker().getException().getMessage();
								if (url == null)
									alertText = "\n\nFailed to load HTML text: "+exceptionMessage;
								else
									alertText = "\n\nFailed to load '"+url+"': "+exceptionMessage;
								Text text = new Text(alertText);
								text.setWrappingWidth(400);
								text.setFont(Font.font("Verdana", 10));
								alert.getDialogPane().setStyle("-fx-padding: 4px,4px,4px,4px;");
								alert.getDialogPane().setContent(text);
								alert.showAndWait();
						}
					}
				});
				jfxPanel.setScene(new Scene(view));
			}
		});
	}

	private void addListenersToAnchors(Document doc, EventListener listener) {
		// Assign our listener to all of the anchors in this document
		{
			NodeList nodeList = doc.getElementsByTagName("a");
			for (int i = 0; i < nodeList.getLength(); i++) {
				((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
			}
		}

		// Ugh.  Now we need to look for frames
		{
			NodeList nodeList = doc.getElementsByTagName("frame");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Class frameClass = nodeList.item(i).getClass();

				HTMLFrameElement fe = (HTMLFrameElement)nodeList.item(i);

				//XXX WHY DOES THIS NEEED TO BE DONE THROUGH REFLECTION??????
				try {
					Method getDocMethod = frameClass.getMethod("getContentDocument");
					Object o = getDocMethod.invoke(fe);
					addListenersToAnchors((Document)o, listener);
				} catch (Exception iae) {
					iae.printStackTrace();
				}
			}
		}

		// Ugh.  Now we need to look for iframes
		{
			NodeList nodeList = doc.getElementsByTagName("iframe");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Class frameClass = nodeList.item(i).getClass();

				HTMLIFrameElement fe = (HTMLIFrameElement)nodeList.item(i);

				//XXX WHY DOES THIS NEEED TO BE DONE THROUGH REFLECTION??????
				try {
					Method getDocMethod = frameClass.getMethod("getContentDocument");
					Object o = getDocMethod.invoke(fe);
					addListenersToAnchors((Document)o, listener);
				} catch (Exception iae) {
					iae.printStackTrace();
				}
			}
		}
	}
 
	public void loadText(final String text) {
		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
				url = null;
				engine.loadContent(text);
			}
		});
	}

	public void loadURL(final String urlToLoad) {
		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
				if (urlToLoad == null) {
					engine.load(null);
					return;
				}

				String tmp = toURL(urlToLoad);
 
				if (tmp == null) {
					tmp = toURL("http://" + urlToLoad);
				}

				url = tmp;
 
				engine.load(tmp);
				enableControls();
			}
		});
	}

	private void enableControls() {
		if (btnBack == null || btnForward == null)
			return;

		int index = engine.getHistory().getCurrentIndex();
		if (index > 0)
			btnBack.setEnabled(true);
		else
			btnBack.setEnabled(false);
		if (index < (engine.getHistory().getEntries().size()-1))
			btnForward.setEnabled(true);
		else
			btnForward.setEnabled(false);
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
		// System.out.println("All tasks finished");
		callbackMethod = null;
	}

	@Override
	public void taskFinished(ObservableTask task) {
		String results = task.getResults(String.class);
		// System.out.println("Task "+task+" finished: "+results);
		logger.info("CyBrowser: results: '"+results+"'");
		if (callbackMethod != null) {
			String cb = callbackMethod; // 
			callbackMethod = null;
			Platform.runLater(new Runnable() {
				@Override public void run() {
					// System.out.println("Executing: "+cb+"(`"+results+"`)");
					// We need to use templated strings to preserve newlines
					// engine.executeScript(cb+"(`"+results+"`)");
					JSObject jsobj = (JSObject) engine.executeScript("window");
					jsobj.call(cb, results);
				}
			});
		}
	}

	private void executeCommand(String command) {
		TaskObserver observer = this;
		// System.out.println("command = "+command);
		logger.info("CyBrowser: executing command: '"+command+"'");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					TaskIterator commandTasks = commandTaskFactory.createTaskIterator(observer, command);
					taskManager.execute(commandTasks, observer);
				} catch (Exception e) {
					logger.error("CyBrowser: error processing command: "+e.getMessage());
				}
			}
		});
	}

	private static String toURL(String str) {
		try {
			return new URL(str).toExternalForm();
		} catch (MalformedURLException exception) {
				return null;
		}
	}

	private void closeAction() {
		manager.closeBrowser(id);
	}

	private void copyLinkAction() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(anchor.toString());
    clipboard.setContent(content);
	}

	private void openNewAction() {
		// Choose the right ID and title
		CyBrowser current = manager.getBrowser(this.id);
		String title = current.getTitle();
		if (title != null)
			title = title + " "+manager.browserCount;

		String newId = id + " "+manager.browserCount;

		manager.browserCount++;

		CyBrowser browser;

		// Open the window or tab
		if (parent != null) {
			// Open a new window
			SwingBrowser sb = new SwingBrowser(manager, newId, title, showDebug);
			sb.setVisible(true);
			browser = sb;
		} else {
			// Open a new tab
			browser = new ResultsPanelBrowser(manager, newId, title);
			manager.registerCytoPanel((ResultsPanelBrowser)browser);
		}
		manager.addBrowser(browser, newId);

		// Load the url
		browser.loadURL(anchor.toString());

	}

	private void copyAction() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(selection);
    clipboard.setContent(content);
	}

	private void reloadAction() {
		engine.reload();
	}


	// We actually want three context menus:
	// 1) Link context menu
	//   a) Open link in new window/tab
	//   b) Copy link location
	//   c) Open link
	// 2) Selected text context menu
	//   a) Copy as text
	//   b) Copy as html
	// 3) Other
	//   a) Reload
	//   b) Close
	//
	private void createContextMenu(WebView webView) {

		// General context menu
		ContextMenu contextMenu = new ContextMenu();
		{
			MenuItem reload = new MenuItem("Reload");
			reload.setOnAction(e -> reloadAction());
			MenuItem close = new MenuItem("Close browser");
			close.setOnAction(e -> closeAction());
			contextMenu.getItems().addAll(reload, close);
		}

		// Link context menu
		ContextMenu hrefContextMenu = new ContextMenu();
		{
			MenuItem copyLink = new MenuItem("Copy link location");
			copyLink.setOnAction(e -> copyLinkAction());
			MenuItem openNew = new MenuItem("Open in new window/tab");
			openNew.setOnAction(e -> openNewAction());
			hrefContextMenu.getItems().addAll(copyLink, openNew);
		}

		// Selection context menu
		ContextMenu selectionContextMenu = new ContextMenu();
		{
			MenuItem copy = new MenuItem("Copy");
			copy.setOnAction(e -> copyAction());
			selectionContextMenu.getItems().addAll(copy);
		}

		// Selection context menu
		ContextMenu selectedAnchorContextMenu = new ContextMenu();
		{
			MenuItem copyLink = new MenuItem("Copy link location");
			copyLink.setOnAction(e -> copyLinkAction());
			MenuItem openNew = new MenuItem("Open in new window/tab");
			openNew.setOnAction(e -> openNewAction());
			MenuItem copy = new MenuItem("Copy text");
			copy.setOnAction(e -> copyAction());
			selectedAnchorContextMenu.getItems().addAll(copyLink, openNew, copy);
		}


		webView.setOnMousePressed(e -> {
			double x = e.getX();
			double y = e.getY();

			// To put info into clipboard:
			// final Clipboard clipboard = Clipboard.getSystemClipboard();
     // final ClipboardContent content = new ClipboardContent();
     // content.putString("Some text");
     // content.putHtml("<b>Some</b> text");
     // clipboard.setContent(content);

			if (e.getButton() == MouseButton.SECONDARY || 
			    (e.isControlDown() && e.getButton() == MouseButton.PRIMARY)) {

				Object anchorElement = engine.executeScript(
									"function getAnchor() {\n"+
									"  var elements = document.querySelectorAll(':hover');\n"+
									"  lastElement = elements.item(elements.length-1);\n"+
									"  if ((lastElement.tagName == 'FRAME') || (lastElement.tagName == 'IFRAME')) {\n"+
									"     elements = lastElement.contentDocument.querySelectorAll(':hover');\n"+
									"     lastElement = elements.item(elements.length-1);\n"+
									"  }\n"+
									"  return lastElement;\n"+
									"}; getAnchor();"
									);

				anchor = null;
				if (anchorElement instanceof Element) {
					Element el = (Element)anchorElement;
					if (el.getTagName().equalsIgnoreCase("A"))
						anchor = el;
				}

				// System.out.println("anchor = "+anchor);

				// Determine if we have a selection
				selection = (String) engine.executeScript("window.getSelection().toString()");
				// System.out.println("selection = "+selection);

				// Figure out which context menu to display
				if (anchor != null && selection != null && selection.length() > 0) {
					selectedAnchorContextMenu.show(webView, e.getScreenX(), e.getScreenY());
				} else if (anchor != null) {
					hrefContextMenu.show(webView, e.getScreenX(), e.getScreenY());
					suppressLink = true;
				} else if (selection != null && selection.length() > 0) {
					selectionContextMenu.show(webView, e.getScreenX(), e.getScreenY());
				} else {
					contextMenu.show(webView, e.getScreenX(), e.getScreenY());
				}
			} else {
				suppressLink = false;
				hrefContextMenu.hide();
				selectionContextMenu.hide();
				selectedAnchorContextMenu.hide();
				contextMenu.hide();
			}
		});
	}

	/*
	// TODO: Figure out how to handle frames and base (for relative URLs)
	private Element findAnchor(Element e, int x, int y) {
		// If we have a frame, we need to find the element within the frame
		System.out.println("Tag name of element = "+e.getTagName());
		while (e != null && !e.getTagName().equalsIgnoreCase("A")) {
			Node n = ((Node)e).getParentNode();

			while (n != null && !(Element.class.isInstance(n))) {
				n = n.getParentNode();
			}
			e = (Element)n;
			System.out.println("e = "+e.getTagName());
		}
		return e;
	}
	*/

	// This class provides methods that will be accessible from Javascript.
	// Initially, the only method is the executeCyCommand method.
	public class Bridge {
		public void executeCyCommand(String command) {
			// System.out.println("Bridge: executing '"+command+"'");
			executeCommand(command);
			callbackMethod = null;
		}

		public void executeCyCommandWithResults(String command, String callback) {
			executeCommand(command);
			callbackMethod = callback;
		}
	}
}
