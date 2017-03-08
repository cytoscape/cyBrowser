package edu.ucsf.rbvi.cyBrowser.internal.view;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;

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
 
import static javafx.concurrent.Worker.State.FAILED;

public class SwingPanel extends JPanel implements TaskObserver {
 
	private final JFXPanel jfxPanel = new JFXPanel();
	private WebEngine engine;
 
	private final JLabel lblStatus = new JLabel();

	public static final String EVENT_TYPE_CLICK = "click";

	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();
	private JButton btnBack = null;
	private JButton btnForward = null;

	private final CyServiceRegistrar registrar;
	private final CommandExecutorTaskFactory commandTaskFactory;
	private final SynchronousTaskManager taskManager;
	private final JDialog parent;
	private final JPanel panel;
	private final boolean showURL;

	final Logger logger = Logger.getLogger(CyUserLog.NAME);
 
	public SwingPanel(CyServiceRegistrar registrar, JDialog parentDialog, boolean showURL) {
		super(new BorderLayout());
		this.registrar = registrar;
		this.showURL = showURL;
		parent = parentDialog;
		panel = this;
		initComponents();
		Platform.setImplicitExit(false);

		// Get the services we'll need
		commandTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		taskManager = registrar.getService(SynchronousTaskManager.class);
	}

	
	private void initComponents() {
		createScene();
 
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
				JButton btnBug = createBugButton();
				rightButtonPanel.add(btnGo);
				rightButtonPanel.add(btnBug);
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
					public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
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
					public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override 
							public void run() {
								progressBar.setValue(newValue.intValue());
							}
						});
					}
				});

				engine.getLoadWorker()
						.exceptionProperty()
						.addListener(new ChangeListener<Throwable>() {
 
							public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
								if (engine.getLoadWorker().getState() == FAILED) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override public void run() {
											JOptionPane.showMessageDialog(
													panel,
													(value != null) ?
													engine.getLocation() + "\n" + value.getMessage() :
													engine.getLocation() + "\nUnexpected error.",
													"Loading error...",
													JOptionPane.ERROR_MESSAGE);
										}
									});
								}
							}
						});

				engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
					@Override
					public void changed(ObservableValue ov, State oldState, State newState) {
						/*
						System.out.println("webEngine result "+newState.toString());
						if (engine.getLoadWorker().getException() != null && newState == State.FAILED){
							String exceptionMessage = ", "+engine.getLoadWorker().getException().toString();
							System.out.println("webEngine failed: "+exceptionMessage);
						}
						*/
						if (newState == Worker.State.SUCCEEDED) {
							EventListener listener = new EventListener() {
								@Override
								public void handleEvent(Event ev) {
									String domEventType = ev.getType();
									if (domEventType.equals(EVENT_TYPE_CLICK)) {
										String href = ((Element)ev.getTarget()).getAttribute("href");
										if (href != null && href.startsWith("cycmd:")) {
											String command = href.substring("cycmd:".length());
											executeCommand(command);
										}
									}
								}
							};

							Document doc = engine.getDocument();
							NodeList nodeList = doc.getElementsByTagName("a");
							for (int i = 0; i < nodeList.getLength(); i++) {
								((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
							}
							enableControls();
						}
					}
				});
				jfxPanel.setScene(new Scene(view));
			}
		});
	}
 
	public void loadText(final String text) {
		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
				engine.loadContent(text);
			}
		});
	}

	public void loadURL(final String url) {
		Platform.runLater(new Runnable() {
			@Override 
			public void run() {
				String tmp = toURL(url);
 
				if (tmp == null) {
					tmp = toURL("http://" + url);
				}
 
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
	}

	@Override
	public void taskFinished(ObservableTask task) {
		String results = task.getResults(String.class);
		// System.out.println("Task "+task+" finished: "+results);
		logger.info("CyBrowser: results: '"+results+"'");
	}

	private void executeCommand(String command) {
		TaskObserver observer = this;
		// System.out.println("command = "+command);
		logger.info("CyBrowser: executing command: '"+command+"'");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				TaskIterator commandTasks = commandTaskFactory.createTaskIterator(observer, command);
				taskManager.execute(commandTasks, observer);
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

}
