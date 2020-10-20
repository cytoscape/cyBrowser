package edu.ucsf.rbvi.cyBrowser.internal.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

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
				fileName = urlTarg.getPath();
				fileName = (new File(fileName)).getName();
			} catch (Exception e) {
				logger.error("Malformed URL: '"+targ+"'");
				return;
			}
		}

		// Handle data urls
		if (targ.startsWith("data:")) {
			int offset = targ.indexOf(",");  // get a pointer to where the data starts
			String[] inst = targ.substring(0, offset).split("[:;]");
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

				// We might have extra URL args -- strip them
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
					((HttpDownloader)processor).startProgress();
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

			String base64String= targ.substring(targ.indexOf(",")+1);
			try {
				byte[] imagedata = Base64.getMimeDecoder().decode(base64String.getBytes(StandardCharsets.UTF_8));
				try (OutputStream stream = new FileOutputStream(file)) {
					stream.write(imagedata);
				}
			} catch (Exception e) {
				logger.error("Failed to write image to file: "+file.toString());
				e.printStackTrace();
			}
		}

	}

	private static class HttpDownloader implements Runnable {
		File file;
		String targ;
		final CloseableHttpResponse response;
    static int BufferSize = 4096;
    JProgressBar jprogressBar;
    JDialog progressDialog;

		public HttpDownloader(CloseableHttpResponse response) {
			this.response = response;
		}

		public void setFile(File file) {this.file = file; }
		public void setURL(String targ) {this.targ = targ; }

    public void startProgress() {
      JOptionPane pane = new JOptionPane();
      pane.setMessage("Downloading "+file.getName()+" ...");
      jprogressBar = new JProgressBar(1, 100);
      jprogressBar.setValue(0);
      pane.add(jprogressBar, 1);
      progressDialog = pane.createDialog("Downloading "+file.getName());
      progressDialog.setModal(false);
      progressDialog.setVisible(true);
    }

		public void run() {
			// Download (in a separate thread?)
			logger.info("Downloading file: "+file.toString()+" from "+targ);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
          double length = (double)entity.getContentLength();
          InputStream in = entity.getContent();
					FileOutputStream outstream = new FileOutputStream(file);
          double read = 0;
          byte[] buffer = new byte[BufferSize];
          int bytesRead;
          do {
            bytesRead = in.read(buffer, 0, BufferSize);
            if (bytesRead > 0) {
              outstream.write(buffer, 0, bytesRead);
              read += (double)bytesRead;
              jprogressBar.setValue((int)((read*100)/length));
            }
          } while (bytesRead > 0);
				}
        progressDialog.setVisible(false);
        progressDialog.dispose();
			} catch (Exception e) {
        e.printStackTrace();
				logger.error("IO error downloading file: '"+file.toString()+"' from '"+targ+"': "+e.getMessage());
        progressDialog.setVisible(false);
        progressDialog.dispose();
				return;
			}
			logger.info("Downloaded file: "+file.toString());
		}
	}

}
