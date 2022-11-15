package edu.ucsf.rbvi.cyBrowser.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.IN_TOOL_BAR;
import static org.cytoscape.work.ServiceProperties.LARGE_ICON_ID;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;
import static org.cytoscape.work.ServiceProperties.TOOLTIP;
import static org.cytoscape.work.ServiceProperties.TOOLTIP_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.TOOL_BAR_GRAVITY;

import java.awt.Font;
import java.util.Properties;

import javax.swing.Icon;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.TextIcon;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import edu.ucsf.rbvi.cyBrowser.internal.model.CyBrowserManager;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.CloseBrowserTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.NativeBrowserTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.DialogTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.HideBrowserTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.ListBrowsersTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.SendTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.ShowBrowserTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.tasks.VersionTaskFactory;
import edu.ucsf.rbvi.cyBrowser.internal.util.IconUtil;
import org.cytoscape.util.swing.OpenBrowser;

public class CyActivator extends AbstractCyActivator {

	private static float LARGE_ICON_FONT_SIZE = 28f;
	private static int LARGE_ICON_SIZE = 32;

	@Override
	public void start(BundleContext bc) {
		// See if we have a graphics console or not
		boolean haveGUI = true;
		ServiceReference ref = bc.getServiceReference(CySwingApplication.class.getName());

    // We need to be able to support CORS web pages
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");


		if (ref == null) {
			haveGUI = false;
			// Issue error and return
		}

		// Get a handle on the CyServiceRegistrar
		CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);

		CyBrowserManager manager = new CyBrowserManager(registrar);

		String version = bc.getBundle().getVersion().toString();
		manager.setVersion(version);

		IconManager iconManager = getService(bc, IconManager.class);
		Font iconFont = IconUtil.getIconFont(LARGE_ICON_FONT_SIZE);

    {
			DialogTaskFactory startBrowser = new DialogTaskFactory(manager, "http://tutorials.cytoscape.org/");

			Icon icon = new TextIcon(
					IconUtil.BROWSER_ICON,
					iconFont,
					IconUtil.BROWSER_ICON_COLORS,
					LARGE_ICON_SIZE,
					LARGE_ICON_SIZE
			);
			String iconId = "rbvi::OPEN_CYBROWSER";
			iconManager.addIcon(iconId, icon);

			Properties props = new Properties();
			props.setProperty(IN_MENU_BAR, "false");
			props.setProperty(IN_TOOL_BAR, "true");
			props.setProperty(TOOL_BAR_GRAVITY, "120000000000000.0");
			props.setProperty(LARGE_ICON_ID, iconId);
			props.setProperty(TOOLTIP, "Cytoscape Web Browser");
			props.setProperty(TOOLTIP_LONG_DESCRIPTION, "Open Cytoscape Web Browser.");
			registerService(bc, startBrowser, TaskFactory.class, props);
		}

		{
			DialogTaskFactory startBrowser = new DialogTaskFactory(manager);

			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Tools");
			props.setProperty(TITLE, "Open web page");
			props.setProperty(MENU_GRAVITY, "1.0");
			props.setProperty(IN_MENU_BAR, "true");
			props.setProperty(IN_TOOL_BAR, "false");
			props.setProperty(TOOLTIP, "Open Web Page");
			props.setProperty(TOOLTIP_LONG_DESCRIPTION, "Open Web Page in Cytoscape Web Browser.");
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "dialog");
			props.setProperty(COMMAND_DESCRIPTION, "Launch an HTML browser in a separate window");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
			                  "Launch Cytoscape's internal web browser in a separate window.  " +
			                  "Provide an ``id`` for the window if you " +
			                  "want subsequent control of the window via ``cybrowser hide``");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{\"id\":\"my window\"}");
			registerService(bc, startBrowser, TaskFactory.class, props);
		}

		{
			ShowBrowserTaskFactory startBrowser = new ShowBrowserTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "show");
			props.setProperty(COMMAND_DESCRIPTION, "Launch an HTML browser in a panel");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
			                  "Launch Cytoscape's internal web browser in a pane in a panel.  "+
			                  "Provide an ``id`` for the window if you " +
			                  "want subsequent control of the window via ``cybrowser hide``");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{\"id\":\"my window\"}");
			registerService(bc, startBrowser, TaskFactory.class, props);
		}

		{
			CloseBrowserTaskFactory closeBrowser = new CloseBrowserTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "close");
			props.setProperty(COMMAND_DESCRIPTION, "Close an open browser, removing it's id");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
			                  "Close an internal web browser and remove all content.  "+
			                  "Provide an ``id`` for the browser you " +
			                  "want to close.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{\"id\":\"my window\"}");
			registerService(bc, closeBrowser, TaskFactory.class, props);
		}

		{
			ListBrowsersTaskFactory listBrowsers = new ListBrowsersTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "list");
			props.setProperty(COMMAND_DESCRIPTION, "List all open browsers");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
			                  "List all browsers that are currently open, whether as "+
			                  "a dialog or in the results panel.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "[{\"id\":\"my window\",\"title\":\"title\", \"url\":\"cytoscape.org\"}]");
			registerService(bc, listBrowsers, TaskFactory.class, props);
		}

		{
			HideBrowserTaskFactory hideBrowser = new HideBrowserTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "hide");
			props.setProperty(COMMAND_DESCRIPTION, "Hide an HTML browser in a panel");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Hide an existing browser, whether it's "+
			                  "in a panel or a separate window.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, hideBrowser, TaskFactory.class, props);
		}

		{
			SendTaskFactory sendBrowser = new SendTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "send");
			props.setProperty(COMMAND_DESCRIPTION, "Send (execute) javascript commands to a browser");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
			                  "Send the text to the browser indicated by the ``id`` and return "+
												"the response, if any.  Note that the JSON ``result`` field could either "+
												"be a bare string or JSON formatted text.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{\"browserId\":\"my window\", \"result\":[1,2,3,4]}");
			registerService(bc, sendBrowser, TaskFactory.class, props);
		}

		{
			VersionTaskFactory versionTask = new VersionTaskFactory(version);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "cybrowser");
			props.setProperty(COMMAND, "version");
			props.setProperty(COMMAND_DESCRIPTION, "Display the CyBrowser version");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Display the version of the CyBrowser app.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, "{\"version\":\"1.0\"}");
			registerService(bc, versionTask, TaskFactory.class, props);
		}

		{
		NativeBrowserTaskFactory nativebrowser = new NativeBrowserTaskFactory(manager, registrar);
		Properties props = new Properties();
		props.setProperty(COMMAND_NAMESPACE, "cybrowser");
		props.setProperty(COMMAND, "native");
		props.setProperty(COMMAND_DESCRIPTION, "Launch an HTML browser with native browser");
		props.setProperty(COMMAND_LONG_DESCRIPTION, "Launch an HTML browser with native browser.");
		registerService(bc, nativebrowser, TaskFactory.class, props);
	}

	}

}
