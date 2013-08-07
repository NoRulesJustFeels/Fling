/*
 * Copyright 2013 ENTERTAILION LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.entertailion.java.fling;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Main window frame for the Fling application
 * 
 * @author leon_nicholls
 * 
 */
public class FlingFrame extends JFrame implements ActionListener,
		BroadcastDiscoveryHandler {
	private static final String LOG_TAG = "FlingFrame";

	// TODO Add your own app id here
	private static final String APP_ID = "YOUR_APP_ID_HERE";

	private static final String HEADER_APPLICATION_URL = "Application-URL";
	private static final String CHROME_CAST_MODEL_NAME = "Eureka Dongle";
	private int port = EmbeddedServer.HTTP_PORT;
	private List<DialServer> servers = new ArrayList<DialServer>();
	private JComboBox deviceList;
	private JDialog progressDialog;
	private JButton refreshButton, playButton, pauseButton, stopButton;
	private ResourceBundle resourceBundle;
	private EmbeddedServer embeddedServer;

	private BroadcastDiscoveryClient broadcastClient;
	private Thread broadcastClientThread;
	private TrackedDialServers trackedServers = new TrackedDialServers();
	private RampClient rampClient = new RampClient();

	public FlingFrame() {
		super();

		Locale locale = Locale.getDefault();
		resourceBundle = ResourceBundle.getBundle(
				"com/entertailion/java/fling/resources/resources", locale);

		JPanel listPane = new JPanel();
		// show list of ChromeCast devices detected on the local network
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		JPanel devicePane = new JPanel();
		devicePane.setLayout(new BoxLayout(devicePane, BoxLayout.LINE_AXIS));
		deviceList = new JComboBox();
		deviceList.addActionListener(this);
		devicePane.add(deviceList);
		URL url = ClassLoader
				.getSystemResource("com/entertailion/java/fling/resources/refresh.png");
		ImageIcon icon = new ImageIcon(url,
				resourceBundle.getString("button.refresh"));
		refreshButton = new JButton(icon);
		refreshButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// refresh the list of devices
				if (deviceList.getItemCount() > 0) {
					deviceList.setSelectedIndex(0);
				}
				discoverDevices();
			}
		});
		refreshButton
				.setToolTipText(resourceBundle.getString("button.refresh"));
		devicePane.add(refreshButton);
		listPane.add(devicePane);
		listPane.add(DragHereIcon.makeUI(this));

		// panel of playback buttons
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		url = ClassLoader
				.getSystemResource("com/entertailion/java/fling/resources/play.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.play"));
		playButton = new JButton(icon);
		playButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				rampClient.play();
			}
		});
		buttonPane.add(playButton);
		url = ClassLoader
				.getSystemResource("com/entertailion/java/fling/resources/pause.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.pause"));
		pauseButton = new JButton(icon);
		pauseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				rampClient.pause();
			}
		});
		buttonPane.add(pauseButton);
		url = ClassLoader
				.getSystemResource("com/entertailion/java/fling/resources/stop.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.stop"));
		stopButton = new JButton(icon);
		stopButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				rampClient.stop();
			}
		});
		buttonPane.add(stopButton);
		listPane.add(buttonPane);
		getContentPane().add(listPane);

		createProgressDialog();
		startWebserver();
		discoverDevices();
	}

	/**
	 * Start a web server to serve the videos to the media player on the
	 * ChromeCast device
	 */
	private void startWebserver() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean started = false;
				while (!started) {
					try {
						embeddedServer = new EmbeddedServer(port);
						Log.d(LOG_TAG, "Started web server on port " + port);
						started = true;
					} catch (IOException ioe) {
						ioe.printStackTrace();
						port++;
					} catch (Exception ex) {
						break;
					}
				}
			}

		}).start();
	}

	/**
	 * Discover ChromeCast devices on the local network
	 * 
	 */
	private void discoverDevices() {
		broadcastClient = new BroadcastDiscoveryClient(getBroadcastAddress(),
				this);
		broadcastClientThread = new Thread(broadcastClient);

		// discovering devices can take time, so do it in a thread
		new Thread(new Runnable() {
			public void run() {
				try {
					showProgressDialog(resourceBundle
							.getString("progress.discoveringDevices"));
					broadcastClientThread.start();

					// wait a while...
					// TODO do this better
					Thread.sleep(10 * 1000);

					broadcastClient.stop();

					Log.d(LOG_TAG, "size=" + trackedServers.size());
					if (trackedServers.size() > 0) {
						deviceList.removeAllItems();
						deviceList.addItem(resourceBundle
								.getString("devices.select")); // no selection
																// option
						for (DialServer dialServer : trackedServers) {
							deviceList.addItem(dialServer.getFriendlyName()
									+ " / "
									+ dialServer.getIpAddress().getHostName());
							servers.add(dialServer);
						}
					}

					deviceList.invalidate();
					hideProgressDialog();
				} catch (InterruptedException e) {
					Log.e(LOG_TAG, "discoverDevices", e);
				}
			}
		}).start();
	}

	public void onBroadcastFound(final BroadcastAdvertisement advert) {
		if (advert.getLocation() != null) {
			new Thread(new Runnable() {
				public void run() {
					Log.d(LOG_TAG, "location=" + advert.getLocation());
					HttpResponse response = new HttpRequestHelper()
							.sendHttpGet(advert.getLocation());
					if (response != null) {
						String appsUrl = null;
						Header header = response
								.getLastHeader(HEADER_APPLICATION_URL);
						if (header != null) {
							appsUrl = header.getValue();
							Log.d(LOG_TAG, "appsUrl=" + appsUrl);
						}
						try {
							InputStream inputStream = response.getEntity()
									.getContent();
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(inputStream));

							InputSource inStream = new org.xml.sax.InputSource();
							inStream.setCharacterStream(reader);
							SAXParserFactory spf = SAXParserFactory
									.newInstance();
							SAXParser sp = spf.newSAXParser();
							XMLReader xr = sp.getXMLReader();
							BroadcastHandler broadcastHandler = new BroadcastHandler();
							xr.setContentHandler(broadcastHandler);
							xr.parse(inStream);
							Log.d(LOG_TAG, "modelName="
									+ broadcastHandler.getDialServer()
											.getModelName());
							// Only handle ChromeCast devices; not other DIAL
							// devices like ChromeCast devices
							if (broadcastHandler.getDialServer().getModelName()
									.equals(CHROME_CAST_MODEL_NAME)) {
								Log.d(LOG_TAG, "ChromeCast device found: "
										+ advert.getIpAddress()
												.getHostAddress());
								DialServer dialServer = new DialServer(advert
										.getLocation(), advert.getIpAddress(),
										advert.getPort(), appsUrl,
										broadcastHandler.getDialServer()
												.getFriendlyName(),
										broadcastHandler.getDialServer()
												.getUuid(), broadcastHandler
												.getDialServer()
												.getManufacturer(),
										broadcastHandler.getDialServer()
												.getModelName());
								trackedServers.add(dialServer);
							}
						} catch (Exception e) {
							Log.e(LOG_TAG, "parse device description", e);
						}
					}
				}
			}).start();
		}
	}

	/**
	 * Get the network broadcast address. Used to listen for multi-cast messages
	 * to discover ChromeCast devices
	 * 
	 * @return
	 */
	public Inet4Address getBroadcastAddress() {
		Inet4Address selectedInetAddress = null;
		try {
			Enumeration<NetworkInterface> list = NetworkInterface
					.getNetworkInterfaces();

			while (list.hasMoreElements()) {
				NetworkInterface iface = list.nextElement();
				if (iface == null)
					continue;

				if (!iface.isLoopback() && iface.isUp()) {
					Iterator<InterfaceAddress> it = iface
							.getInterfaceAddresses().iterator();
					while (it.hasNext()) {
						InterfaceAddress interfaceAddress = it.next();
						if (interfaceAddress == null)
							continue;
						InetAddress address = interfaceAddress.getAddress();
						if (address instanceof Inet4Address) {
							if (address.getHostAddress().toString().charAt(0) != '0') {
								InetAddress broadcast = interfaceAddress
										.getBroadcast();
								if (selectedInetAddress == null) {
									selectedInetAddress = (Inet4Address) broadcast;
								} else if (iface.getName().startsWith("wlan")
										|| iface.getName().startsWith("en")) { // prefer
																				// wlan
																				// interface
									selectedInetAddress = (Inet4Address) broadcast;
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		}

		return selectedInetAddress;
	}

	/**
	 * Get the network address.
	 * 
	 * @return
	 */
	public Inet4Address getNetworAddress() {
		Inet4Address selectedInetAddress = null;
		try {
			Enumeration<NetworkInterface> list = NetworkInterface
					.getNetworkInterfaces();

			while (list.hasMoreElements()) {
				NetworkInterface iface = list.nextElement();
				if (iface == null)
					continue;

				if (!iface.isLoopback() && iface.isUp()) {
					Iterator<InterfaceAddress> it = iface
							.getInterfaceAddresses().iterator();
					while (it.hasNext()) {
						InterfaceAddress interfaceAddress = it.next();
						if (interfaceAddress == null)
							continue;
						InetAddress address = interfaceAddress.getAddress();
						if (address instanceof Inet4Address) {
							if (address.getHostAddress().toString().charAt(0) != '0') {
								InetAddress networkAddress = interfaceAddress
										.getAddress();
								if (selectedInetAddress == null) {
									selectedInetAddress = (Inet4Address) networkAddress;
								} else if (iface.getName().startsWith("wlan")
										|| iface.getName().startsWith("en")) { // prefer
																				// wlan
																				// interface
									selectedInetAddress = (Inet4Address) networkAddress;
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		}

		return selectedInetAddress;
	}

	/**
	 * Create a progress indicator
	 */
	private void createProgressDialog() {
		progressDialog = new JDialog(this,
				resourceBundle.getString("progress.title"), true);
		JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		progressDialog.add(BorderLayout.CENTER, progressBar);
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progressDialog.setSize(300, 75);
		progressDialog.setLocationRelativeTo(this);
	}

	/**
	 * Show the progress indicator with a title message
	 * 
	 * @param message
	 */
	private void showProgressDialog(final String message) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressDialog.setLocationRelativeTo(FlingFrame.this);
				progressDialog.setTitle(message);
				progressDialog.setVisible(true);
			}
		});
	}

	/**
	 * Hide the progress indicator
	 */
	private void hideProgressDialog() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressDialog.setVisible(false);
			}
		});
	}

	/**
	 * Event handler for device dropdown list selection
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource();
		int pos = cb.getSelectedIndex();
		// when device is selected, attempt to connect
		if (servers != null && pos > 0) {
			final DialServer dialServer = servers.get(pos - 1);

			rampClient.launchApp(APP_ID, dialServer);
		}
	}

	/**
	 * Send a uri to the ChromeCast device
	 * 
	 * @param keycode
	 */
	protected void sendMediaUrl(String file) {
		Log.d(LOG_TAG, "sendMediaUrl=" + file);
		if (file != null) {
			try {
				int pos = file.lastIndexOf('.');
				String extension = "";
				if (pos > -1) {
					extension = file.substring(pos);
				}
				String url = "http://" + getNetworAddress().getHostAddress()
						+ ":" + port + "/video" + extension;
				rampClient.stop();
				rampClient.load(url);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
