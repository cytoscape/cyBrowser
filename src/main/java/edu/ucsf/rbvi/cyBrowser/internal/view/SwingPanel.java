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

	private final JButton btnGo = new JButton("Go");
	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();

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
	 
			btnGo.addActionListener(al);
			txtURL.addActionListener(al);
  
			progressBar.setPreferredSize(new Dimension(150, 18));
			progressBar.setStringPainted(true);
  
			JPanel topBar = new JPanel(new BorderLayout(5, 0));
			topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			topBar.add(txtURL, BorderLayout.CENTER);
			topBar.add(btnGo, BorderLayout.EAST);
 
			JPanel statusBar = new JPanel(new BorderLayout(5, 0));
			statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			statusBar.add(lblStatus, BorderLayout.CENTER);
			statusBar.add(progressBar, BorderLayout.EAST);
	 
			add(topBar, BorderLayout.NORTH);
			add(statusBar, BorderLayout.SOUTH);
		}
		add(jfxPanel, BorderLayout.CENTER);
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
						if (newState == Worker.State.SUCCEEDED) {
							EventListener listener = new EventListener() {
								@Override
								public void handleEvent(Event ev) {
									String domEventType = ev.getType();
									if (domEventType.equals(EVENT_TYPE_CLICK)) {
										String href = ((Element)ev.getTarget()).getAttribute("href");
										if (href.startsWith("cycmd:")) {
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
						}
					}
				});
				jfxPanel.setScene(new Scene(view));
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
			}
		});
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {}

	@Override
	public void taskFinished(ObservableTask task) {
		String results = task.getResults(String.class);
		logger.info("CyBrowser: results: '"+results+"'");
	}

	private void executeCommand(String command) {
		System.out.println("command = "+command);
		logger.info("CyBrowser: executing command: '"+command+"'");
		TaskIterator commandTasks = commandTaskFactory.createTaskIterator(this, command);
		taskManager.execute(commandTasks);
	}

	private static String toURL(String str) {
		try {
			return new URL(str).toExternalForm();
		} catch (MalformedURLException exception) {
				return null;
		}
	}

}
