package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.FileUtil;

import org.apache.log4j.Logger;

import edu.ucsf.rbvi.cyBrowser.internal.view.SwingBrowser;
 
public class Downloader {
 
	final static Logger logger = Logger.getLogger(CyUserLog.NAME);

	// TODO: move this code to a utility class so it can be used by Bridge
	public static void download(CyServiceRegistrar registrar, SwingBrowser parent, 
	                            String targ, String fileName, boolean prompt) {
		FileUtil fileUtil = registrar.getService(FileUtil.class);
		if (fileName == null && !targ.startsWith("data:")) {
			try {
				URL urlTarg = new URL(targ);
				fileName = urlTarg.getFile();
			} catch (Exception e) {
				logger.error("Malformed URL: '"+targ+"'");
				return;
			}
		}

		// Handle data urls
		if (targ.startsWith("data:")) {
			int offset = targ.indexOf(",");  // get a pointer to where the data starts
			String[] inst = targ.substring(0, offset).split("[:;]");
			System.out.println("inst[0] = "+inst[0]+" inst[1] = "+inst[1]+" inst[2] = "+inst[2]);
			if (inst[2].startsWith("base64") && inst[1].startsWith("image")) {
				doDownload(fileUtil, parent, targ, fileName, true, new DataDownloader());
			} else {
				logger.error("Currently only base64-encoded images are supported for data url's");
				return;
			}
			return;
		}

		if (fileName.charAt(0) == '/')
			fileName = fileName.substring(1);

		try {
			// Get a connection
			CloseableHttpClient client = HttpClients.createDefault();
			HttpGet request = new HttpGet(targ);
			CloseableHttpResponse response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == 200) {
				doDownload(fileUtil, parent, targ, fileName, prompt, new HttpDownloader(response));
			}
		} catch (Exception e) {
			logger.error("Error downloading file from: '"+targ+"': "+e.getMessage());
		}
	}

	private static void doDownload(final FileUtil fileUtil, final SwingBrowser parent, final String targ,
	                               final String fileName, final boolean prompt, Runnable processor) {

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				File file;
				if (prompt) {
					// File chooser to get a file
					file = fileUtil.getFile(parent, "Name of downloaded file", FileUtil.SAVE, null, 
					                        fileName, null, new ArrayList<>());
					if (file == null) return;
				} else {
					// TODO: prompt user if it's OK to download?
					int download = 
					         JOptionPane.showConfirmDialog(parent, 
					                                       "You have requested a download of "+fileName+", OK to download?",
					                                       "Download", JOptionPane.OK_CANCEL_OPTION);
					if (download == 2) return;
					// This is problematic since we don't have a concept of a default "Download" location
					// Use home for now
					file = new File(System.getProperty("user.home")+File.separator+fileName);
				}

				if (processor instanceof HttpDownloader) {
					((HttpDownloader)processor).setFile(file);
					((HttpDownloader)processor).setURL(targ);
				} else if (processor instanceof DataDownloader) {
					((DataDownloader)processor).setFile(file);
					((DataDownloader)processor).setURL(targ);
				}

				try {
					Thread t = new Thread(processor);
					t.start();
				} catch(Exception e) {
					logger.error("Unable to open file: '"+file.toString()+"': "+e.getMessage());
				}
			}
		});
	}

	private static class DataDownloader implements Runnable {
		File file;
		String targ;

		public DataDownloader() {
		}

		public void setFile(File file) {this.file = file; }
		public void setURL(String targ) {this.targ = targ; }

		public void run() {
			logger.info("Saving file: "+file.toString());

			try {
				byte[] imagedata = DatatypeConverter.parseBase64Binary(targ.substring(targ.indexOf(",") + 1));
				try (OutputStream stream = new FileOutputStream(file)) {
					stream.write(imagedata);
				}
			} catch (Exception e) {
				logger.error("Failed to write image to file: "+file.toString());
			}
		}

	}

	private static class HttpDownloader implements Runnable {
		File file;
		String targ;
		final CloseableHttpResponse response;

		public HttpDownloader(CloseableHttpResponse response) {
			this.response = response;
		}

		public void setFile(File file) {this.file = file; }
		public void setURL(String targ) {this.targ = targ; }

		public void run() {
			// Download (in a separate thread?)
			logger.info("Downloading file: "+file.toString()+" from "+targ);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					FileOutputStream outstream = new FileOutputStream(file);
					entity.writeTo(outstream);
				}
			} catch (Exception e) {
				logger.error("IO error downloading file: '"+file.toString()+"' from '"+targ+"': "+e.getMessage());
				return;
			}
			logger.info("Downloaded file: "+file.toString());
		}
	}

}
