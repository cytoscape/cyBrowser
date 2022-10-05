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
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.cyBrowser.internal.model.Bridge;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowser;
import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.model.Downloader;

import static javafx.concurrent.Worker.State.FAILED;

public class SwingPanel extends JPanel {

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
	private final SwingBrowser parent;
	private final JPanel panel;
	private final boolean showURL;
	private final boolean showDebug;
	private String callbackMethod = null;
	private String url = null;
	private boolean suppressLink = false;
	private String title = null;

	private String lastTitle = null;
	private String lastText = null;

	// Three class variables to help with our context menus
	private String selection;
	private Element anchor;

	// Our javascript bridge
	private Bridge jsBridge;

	// Our ID
	private final String id;

	final Logger logger = Logger.getLogger(CyUserLog.NAME);

	public SwingPanel(CyBrowserManager manager, String id, SwingBrowser parentDialog, SwingPanel reuse,
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
	}

	public String getURL() {
		return engine.getLocation();
	}

	public Document getText() {
		if (engine == null || engine.getDocument() == null) return null;
		return engine.getDocument();
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String execute(final String script) {
		final String[] returnVal = new String[1]; // I *hate* Java sometimes...

		// We're already on the Application Thread
		if (Platform.isFxApplicationThread()) {
			return ((JSObject) engine.executeScript(script)).toString();
		}

		// Run on the application thread
		final CountDownLatch doneLatch = new CountDownLatch(1);
		final WebEngine engineTemp = engine;
		Platform.runLater(new Runnable() {
			public void run() {
				try {
					Object obj = engineTemp.executeScript(script);
					returnVal[0] = obj.toString();
				} finally {
					doneLatch.countDown();
				}
			}
		});

		try {
			doneLatch.await();
		} catch (InterruptedException e) {
		}
		return returnVal[0];
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
			ActionListener open = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openNativeBrowser(txtURL.getText());
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
				JButton btnOpen = new JButton("Open");
        btnOpen.setToolTipText("Open in native browser");
				btnOpen.addActionListener(open);
				rightButtonPanel.add(btnOpen);

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
						engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://cdnjs.cloudflare.com/ajax/libs/firebug-lite/1.4.0/firebug-lite.min.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://cdnjs.cloudflare.com/ajax/libs/firebug-lite/1.4.0/firebug-lite.min.js' + '#startOpened');}");
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
								title = newValue;
								if (parent != null)
									parent.setTitle(id, newValue);
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
						Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
						stage.setAlwaysOnTop(true);
						stage.toFront();
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
						Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
						stage.setAlwaysOnTop(true);
						stage.toFront();
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

				engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
          @Override
          public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
            System.out.println("webEngine exception: "+t1.getMessage());
          }
        });

        /*
        com.sun.javafx.webkit.WebConsoleListener.setDefaultListener(new com.sun.javafx.webkit.WebConsoleListener(){
          @Override
          public void messageAdded(WebView webView, String message, int lineNumber, String sourceId) {
            System.out.println("WebConsoleListener Console: [" + sourceId + ":" + lineNumber + "] " + message);
          }
        });
        */

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

              //System.out.println("Creating new bridge");

							// Set member for 'window' object
							// In Javascript access: window.cybrowser...
							jsBridge = new Bridge(engine, registrar, parent);
							jsobj.setMember("cybrowser", jsBridge);
              // System.out.println("Setting console.log to our log");
              engine.executeScript("console.log = function(message) { cybrowser.log(message); }");

							// Now set up a listener for link click events
							EventListener listener = new EventListener() {
								@Override
								public void handleEvent(Event ev) {
									String domEventType = ev.getType();
									if (domEventType.equals(EVENT_TYPE_CLICK)) {
										if (suppressLink) {
											if (ev.getCancelable()) {
												ev.preventDefault();
												ev.stopPropagation();
											}
											suppressLink = false;
										} else {
											Element aElement = (Element)ev.getTarget();
											String href = null;
											boolean haveDownload = false;
											String download = null;
											String target = null;

											if (aElement.getTagName().equals("A")) {
												href = aElement.toString();
												haveDownload = aElement.hasAttribute("download");
												download = aElement.getAttribute("download");
												target = aElement.getAttribute("target");
											} else {
												// OK, how did we get here?
												// We need to do this in case there's a download link wrapping
												// an image or something
												if (aElement.getParentNode().getNodeName().equals("A")) {
													Element pElement = (Element)aElement.getParentNode();
													download = pElement.getAttribute("download");
													haveDownload = pElement.hasAttribute("download");
													target = pElement.getAttribute("target");
													href = pElement.toString();
												}
											}

											if (download == null || download.length() < 1)
												download = null;

											if (href != null && haveDownload) {
												downloadAction(href, download, false);
												ev.preventDefault();
												ev.stopPropagation();
											} else if (href != null && href.startsWith("cycmd:")) {
												String command = href.substring("cycmd:".length());
												jsBridge.executeCommand(command, null);
												ev.preventDefault();
												ev.stopPropagation();
											} else if (href != null && target != null && target.equalsIgnoreCase("_blank")) {
												anchor = (Element)ev.getTarget();
												openNewAction(true);
												ev.preventDefault();
												ev.stopPropagation();
											} else {
												CyBrowser current = manager.getBrowser(id);
												if (current != null) {
													lastTitle = current.getTitle(id);
												}

												if (txtURL != null) {
													lastText = txtURL.getText();
												}
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
						} else if (newState == Worker.State.CANCELLED) {
							try{
								URL targ = new URL(txtURL.getText());

								URLConnection conn =  targ.openConnection();
								Map<String, List<String>> map = conn.getHeaderFields();
								String fileName = null;

								List<String> contentDisposition = map.get("Content-Disposition");
                if (contentDisposition != null && !contentDisposition.isEmpty()) {
				  				for (String cd: contentDisposition) {
				  					int index = cd.indexOf("filename=");
				  					if (index == -1) continue;
				  					String fn = cd.substring(index+9);
                   // is the filename quoted?
                   if (fn.indexOf("\"") == -1) {
                     fileName = fn;
                   } else {
				  					  String[] st = fn.split("\"");
				  					  fileName = st[1];
                   }
				  				}
                }

								List<String> contentTypeList = map.get("Content-Type");
								if (contentTypeList != null && !contentTypeList.isEmpty()) {
									String contentType = contentTypeList.get(0);
									if (contentType.equalsIgnoreCase("text/html"))
										return;

									// Downloading
									downloadAction(txtURL.getText(), fileName, true);

									// Reset our title, etc.
									CyBrowser current = manager.getBrowser(id);
									String title = current.getTitle(id);
									parent.setTitle(id, lastTitle);
									txtURL.setText(lastText);
								}
							} catch (Exception e) {
                e.printStackTrace();
								logger.error("Failed to download '"+txtURL.getText()+"': "+e.getMessage());
							}
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

		{
			NodeList nodeList = doc.getElementsByTagName("frame");
			for (int i = 0; i < nodeList.getLength(); i++) {
				HTMLFrameElement element = (HTMLFrameElement)nodeList.item(i);
				addListenersToAnchors(element.getContentDocument(), listener);
			}
		}

		{
			NodeList nodeList = doc.getElementsByTagName("iframe");
			for (int i = 0; i < nodeList.getLength(); i++) {
				HTMLIFrameElement element = (HTMLIFrameElement)nodeList.item(i);
				addListenersToAnchors(element.getContentDocument(), listener);
			}
		}

		/*
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
		*/
	}

	public void loadText(final String text) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				url = null;
				engine.loadContent(text);
				// Clear Cytoscape listeners?
				if (jsBridge != null)
					jsBridge.clearListeners();
			}
		});
	}

	public void loadURL(final String urlToLoad) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (urlToLoad == null) {
					engine.load(null);
					if (jsBridge != null)
						jsBridge.clearListeners();
					return;
				}

				String tmp = toURL(urlToLoad);

				if (tmp == null) {
					tmp = toURL("http://" + urlToLoad);
				}

				url = tmp;
				engine.load(tmp);

				// Clear Cytoscape listeners?
				if (jsBridge != null)
					jsBridge.clearListeners();
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

	private void openLinkAction() {
    String url = anchor.toString();
    openNativeBrowser(url);
  }

  private void openNativeBrowser(String url) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        OpenBrowser nativeBrowser = registrar.getService(OpenBrowser.class);
        nativeBrowser.openURL(url, false);
      }
    });
  }

	private void downloadAction() {
		String targ = anchor.toString();
		downloadAction(targ, null, true);
	}

	// TODO: move this code to a utility class so it can be used by Bridge
	private void downloadAction(String targ, String fileName, boolean prompt) {
		Downloader.download(registrar, parent, targ, fileName, prompt);
	}

	private void openNewAction(boolean openTab) {
		// Choose the right ID and title
		CyBrowser current = manager.getBrowser(this.id);
		String title = current.getTitle(this.id);
		if (title != null)
			title = title + " "+manager.browserCount;

		String newId = id + " "+manager.browserCount;

		manager.browserCount++;

		CyBrowser browser;

		// Open the window or tab
		if (parent != null) {
			if (!openTab) {
				// Open a new window
				SwingBrowser sb = new SwingBrowser(manager, newId, title, showDebug);
				sb.setVisible(true);
				browser = sb;
			} else {
				browser = parent;
				parent.addTab(newId, title, showDebug);
			}
		} else {
			// Open a new tab
			browser = new ResultsPanelBrowser(manager, newId, title);
			manager.registerCytoPanel((ResultsPanelBrowser)browser, true);
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
			MenuItem openLink = new MenuItem("Open link in native browser");
			openLink.setOnAction(e -> openLinkAction());
			hrefContextMenu.getItems().add(openLink);

			MenuItem copyLink = new MenuItem("Copy link location");
			copyLink.setOnAction(e -> copyLinkAction());
			hrefContextMenu.getItems().add(copyLink);
			if (parent != null) {
				MenuItem openNew = new MenuItem("Open in new window");
				openNew.setOnAction(e -> openNewAction(false));
				MenuItem openNewTab = new MenuItem("Open in new tab");
				openNewTab.setOnAction(e -> openNewAction(true));
				hrefContextMenu.getItems().addAll(openNew, openNewTab);
			} else {
				MenuItem openNew = new MenuItem("Open in new tab");
				openNew.setOnAction(e -> openNewAction(true));
				hrefContextMenu.getItems().add(openNew);
			}
			MenuItem download = new MenuItem("Download to file");
			download.setOnAction(e -> downloadAction());
			hrefContextMenu.getItems().add(download);
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
			MenuItem openLink = new MenuItem("Open link in native browser");
			openLink.setOnAction(e -> openLinkAction());
			hrefContextMenu.getItems().add(openLink);

			MenuItem copyLink = new MenuItem("Copy link location");
			copyLink.setOnAction(e -> copyLinkAction());
			selectedAnchorContextMenu.getItems().add(copyLink);
			if (parent != null) {
				MenuItem openNew = new MenuItem("Open in new window");
				openNew.setOnAction(e -> openNewAction(false));
				MenuItem openNewTab = new MenuItem("Open in new tab");
				openNewTab.setOnAction(e -> openNewAction(true));
				selectedAnchorContextMenu.getItems().addAll(openNew, openNewTab);
			} else {
				MenuItem openNew = new MenuItem("Open in new tab");
				openNew.setOnAction(e -> openNewAction(true));
				selectedAnchorContextMenu.getItems().add(openNew);
			}
			MenuItem copy = new MenuItem("Copy text");
			copy.setOnAction(e -> copyAction());
			selectedAnchorContextMenu.getItems().add(copy);
		}


		webView.setOnMousePressed(e -> {
			double x = e.getX();
			double y = e.getY();

			if (e.getButton() == MouseButton.SECONDARY ||
			    (e.isControlDown() && e.getButton() == MouseButton.PRIMARY)) {

				// FIXME
				Object anchorElement = engine.executeScript(
									"function getAnchor() {\n"+
									"  var elements = document.querySelectorAll(':hover');\n"+
									"  lastElement = elements.item(elements.length-1);\n"+
									"  if ((lastElement != null) &&"+
									"      ((lastElement.tagName == 'FRAME') || (lastElement.tagName == 'IFRAME'))) {\n"+
									"     elements = lastElement.contentDocument.querySelectorAll(':hover');\n"+
									"     lastElement = elements.item(elements.length-1);\n"+
									"  }\n"+
									"  return lastElement;\n"+
									"}; getAnchor();"
									);

				anchor = null;
				if (anchorElement instanceof Element) {
					Element el = findAnchor((Element)anchorElement);
					if (el != null && el.getTagName().equalsIgnoreCase("A")) {
						anchor = el;
					}
				}

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

	private Element findAnchor(Element e) {
		// If we have a frame, we need to find the element within the frame
		while (e != null && !e.getTagName().equalsIgnoreCase("A")) {
			Node n = ((Node)e).getParentNode();

			while (n != null && !(Element.class.isInstance(n))) {
				n = n.getParentNode();
			}
			e = (Element)n;
		}
		return e;
	}
}
