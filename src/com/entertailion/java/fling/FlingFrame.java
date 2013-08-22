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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

/**
 * Main window frame for the Fling application
 * 
 * @author leon_nicholls
 * 
 */
public class FlingFrame extends JFrame implements ActionListener, BroadcastDiscoveryHandler {
	private static final String LOG_TAG = "FlingFrame";

	// TODO Add your own app id here
	public static final String CHROMECAST = "ChromeCast";
	private static final String APP_ID = CHROMECAST; // use the public receiver;
														// change to your app id
														// if Google blocks this

	private static final String HEADER_APPLICATION_URL = "Application-URL";
	private static final String CHROME_CAST_MODEL_NAME = "Eureka Dongle";
	private static final String TRANSCODING_EXTENSIONS = "wmv,avi,mkv,mpg,mpeg,flv,3gp,ogm";
	private static final String TRANSCODING_PARAMETERS = "vcodec=VP80,vb=1000,vfilter=canvas{width=640,height=360},acodec=vorb,ab=128,channels=2,samplerate=44100";
	private static final String PROPERTY_TRANSCODING_EXTENSIONS = "transcoding.extensions";
	private static final String PROPERTY_TRANSCODING_PARAMETERS = "transcoding.parameters";
	private static final String SELECTED_NETWORK = "selected.network";
	private int port = EmbeddedServer.HTTP_PORT;
	private List<DialServer> servers = new ArrayList<DialServer>();
	private JComboBox deviceList;
	private JDialog progressDialog;
	private JButton refreshButton, playButton, pauseButton, stopButton, settingsButton;
	private JLabel label;
	private ResourceBundle resourceBundle;
	private EmbeddedServer embeddedServer;

	private BroadcastDiscoveryClient broadcastClient;
	private Thread broadcastClientThread;
	private TrackedDialServers trackedServers = new TrackedDialServers();
	private RampClient rampClient;

	private MediaPlayerFactory mediaPlayerFactory;
	private MediaPlayer mediaPlayer;

	private String transcodingParameterValues = TRANSCODING_PARAMETERS;
	private String transcodingExtensionValues = TRANSCODING_EXTENSIONS;

	private DialServer selectedDialServer;

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
	
	private String selectedNetwork;

	public FlingFrame() {
		super();

		rampClient = new RampClient(this);

		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		Locale locale = Locale.getDefault();
		resourceBundle = ResourceBundle.getBundle("com/entertailion/java/fling/resources/resources", locale);

		JPanel listPane = new JPanel();
		// show list of ChromeCast devices detected on the local network
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		JPanel devicePane = new JPanel();
		devicePane.setLayout(new BoxLayout(devicePane, BoxLayout.LINE_AXIS));
		deviceList = new JComboBox();
		deviceList.addActionListener(this);
		devicePane.add(deviceList);
		URL url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/refresh.png");
		ImageIcon icon = new ImageIcon(url, resourceBundle.getString("button.refresh"));
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
		refreshButton.setToolTipText(resourceBundle.getString("button.refresh"));
		devicePane.add(refreshButton);
		url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/settings.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.manualIp"));
		settingsButton = new JButton(icon);
		settingsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JTextField transcodingExtensions = new JTextField(50);
				transcodingExtensions.setText(transcodingExtensionValues);
				JTextField transcodingParameters = new JTextField(50);
				transcodingParameters.setText(transcodingParameterValues);

				JPanel myPanel = new JPanel(new BorderLayout());
				JPanel labelPanel = new JPanel(new GridLayout(3, 1));
				JPanel fieldPanel = new JPanel(new GridLayout(3, 1));
				myPanel.add(labelPanel, BorderLayout.WEST);
				myPanel.add(fieldPanel, BorderLayout.CENTER);
				labelPanel.add(new JLabel(resourceBundle.getString("transcoding.extensions"), JLabel.RIGHT));
				fieldPanel.add(transcodingExtensions);
				labelPanel.add(new JLabel(resourceBundle.getString("transcoding.parameters"), JLabel.RIGHT));
				fieldPanel.add(transcodingParameters);
				JComboBox networkList = new JComboBox();
				networkList.addItem(resourceBundle.getString("network.selection")); // automatic selection option
				labelPanel.add(new JLabel(resourceBundle.getString("network.select"), JLabel.RIGHT));
				fieldPanel.add(networkList);
				int selectedPosition = 0;
				try {
					int count = 1;
					Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
					while (list.hasMoreElements()) {
						NetworkInterface iface = list.nextElement();
						if (iface == null)
							continue;

						if (!iface.isLoopback() && iface.isUp()) {
							Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
							while (it.hasNext()) {
								InterfaceAddress interfaceAddress = it.next();
								if (interfaceAddress == null)
									continue;
								InetAddress address = interfaceAddress.getAddress();
								Log.d(LOG_TAG, "address=" + address);
								if (address instanceof Inet4Address) {
									String network = address.getHostAddress().toString();
									networkList.addItem(network);
									if (network.equals(selectedNetwork)) {
										selectedPosition = count;
									}
									count++;
								}
							}
						}
					}
				} catch (Exception ex) {
				}
				networkList.setSelectedIndex(selectedPosition);
				String previousSelectedNetwork = selectedNetwork;
				int result = JOptionPane.showConfirmDialog(FlingFrame.this, myPanel, resourceBundle.getString("settings.title"), JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					transcodingParameterValues = transcodingParameters.getText();
					transcodingExtensionValues = transcodingExtensions.getText();
					int pos = networkList.getSelectedIndex();
					if (pos==0) {
						selectedNetwork = "";
					} else {
						selectedNetwork = (String)networkList.getSelectedItem();
					}
					storeProperties();
					
					if (!previousSelectedNetwork.equals(selectedNetwork)) {
						JOptionPane.showMessageDialog(FlingFrame.this, resourceBundle.getString("network.changed"));
					}
				}
			}
		});
		settingsButton.setToolTipText(resourceBundle.getString("button.manualIp"));
		devicePane.add(settingsButton);
		listPane.add(devicePane);
		listPane.add(DragHereIcon.makeUI(this));

		// panel of playback buttons
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		label = new JLabel("00:00:00");
		buttonPane.add(label);
		url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/play.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.play"));
		playButton = new JButton(icon);
		playButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				rampClient.play();
			}
		});
		buttonPane.add(playButton);
		url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/pause.png");
		icon = new ImageIcon(url, resourceBundle.getString("button.pause"));
		pauseButton = new JButton(icon);
		pauseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				rampClient.pause();
			}
		});
		buttonPane.add(pauseButton);
		url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/stop.png");
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
		Properties properties = loadProperties();
		Inet4Address broadcastAddress = getBroadcastAddress();
		if (broadcastAddress != null) {
			if (!broadcastAddress.getHostAddress().endsWith(".255")) {
				// invalid broadcast address; use default instead
				try {
					broadcastAddress = (Inet4Address) Inet4Address.getByName("255.255.255.255");
				} catch (Exception e) {
				}
			}
			broadcastClient = new BroadcastDiscoveryClient(broadcastAddress, this);
			broadcastClientThread = new Thread(broadcastClient);
			
			deviceList.removeAllItems();
			// Prompt selection option
			deviceList.addItem(resourceBundle.getString("devices.select")); 
			
			// discovering devices can take time, so do it in a thread
			new Thread(new Runnable() {
				public void run() {
					try {
						showProgressDialog(resourceBundle.getString("progress.discoveringDevices"));
						broadcastClientThread.start();

						// wait a while...
						// TODO do this better
						Thread.sleep(10 * 1000);

						broadcastClient.stop();

						Log.d(LOG_TAG, "size=" + trackedServers.size());
						if (trackedServers.size() > 0) {
							for (DialServer dialServer : trackedServers) {
								//deviceList.addItem(dialServer.getFriendlyName() + " / " + dialServer.getIpAddress().getHostName());
								deviceList.addItem(dialServer);
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
		} else {
			Log.d(LOG_TAG, "broadcastAddress null");
		}
	}

	public void onBroadcastFound(final BroadcastAdvertisement advert) {
		if (advert.getLocation() != null) {
			new Thread(new Runnable() {
				public void run() {
					Log.d(LOG_TAG, "location=" + advert.getLocation());
					HttpResponse response = new HttpRequestHelper().sendHttpGet(advert.getLocation());
					if (response != null) {
						String appsUrl = null;
						Header header = response.getLastHeader(HEADER_APPLICATION_URL);
						if (header != null) {
							appsUrl = header.getValue();
							if (!appsUrl.endsWith("/")) {
								appsUrl = appsUrl + "/";
							}
							Log.d(LOG_TAG, "appsUrl=" + appsUrl);
						}
						try {
							InputStream inputStream = response.getEntity().getContent();
							BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

							InputSource inStream = new org.xml.sax.InputSource();
							inStream.setCharacterStream(reader);
							SAXParserFactory spf = SAXParserFactory.newInstance();
							SAXParser sp = spf.newSAXParser();
							XMLReader xr = sp.getXMLReader();
							BroadcastHandler broadcastHandler = new BroadcastHandler();
							xr.setContentHandler(broadcastHandler);
							xr.parse(inStream);
							Log.d(LOG_TAG, "modelName=" + broadcastHandler.getDialServer().getModelName());
							// Only handle ChromeCast devices; not other DIAL
							// devices like ChromeCast devices
							if (broadcastHandler.getDialServer().getModelName().equals(CHROME_CAST_MODEL_NAME)) {
								Log.d(LOG_TAG, "ChromeCast device found: " + advert.getIpAddress().getHostAddress());
								DialServer dialServer = new DialServer(advert.getLocation(), advert.getIpAddress(), advert.getPort(), appsUrl, broadcastHandler
										.getDialServer().getFriendlyName(), broadcastHandler.getDialServer().getUuid(), broadcastHandler.getDialServer()
										.getManufacturer(), broadcastHandler.getDialServer().getModelName());
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
			InterfaceAddress interfaceAddress = null;
			if (selectedNetwork.length()>0) {
				String prefix = selectedNetwork.substring(0, selectedNetwork.indexOf('.')+1);
				Log.d(LOG_TAG, "prefix="+prefix);
				interfaceAddress = getPreferredInetAddress(prefix);
			} else {
				InterfaceAddress oneNineTwoInetAddress = getPreferredInetAddress("192.");
				if (oneNineTwoInetAddress != null) {
					interfaceAddress = oneNineTwoInetAddress;
				} else {
					InterfaceAddress oneSevenTwoInetAddress = getPreferredInetAddress("172.");
					if (oneSevenTwoInetAddress != null) {
						interfaceAddress = oneSevenTwoInetAddress;
					} else {
						interfaceAddress = getPreferredInetAddress("10.");
					}
				}
			}
			if (interfaceAddress != null) {
				InetAddress broadcast = interfaceAddress.getBroadcast();
				Log.d(LOG_TAG, "broadcast=" + broadcast);
				if (broadcast != null) {
					return (Inet4Address) broadcast;
				}
			}

		} catch (Exception ex) {
		}

		return selectedInetAddress;
	}

	private InterfaceAddress getPreferredInetAddress(String prefix) {
		InterfaceAddress selectedInterfaceAddress = null;
		try {
			Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();

			while (list.hasMoreElements()) {
				NetworkInterface iface = list.nextElement();
				if (iface == null)
					continue;

				if (!iface.isLoopback() && iface.isUp()) {
					Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
					while (it.hasNext()) {
						InterfaceAddress interfaceAddress = it.next();
						if (interfaceAddress == null)
							continue;
						InetAddress address = interfaceAddress.getAddress();
						Log.d(LOG_TAG, "address=" + address);
						if (address instanceof Inet4Address) {
							if (address.getHostAddress().toString().startsWith(prefix)) {
								return interfaceAddress;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		}
		return selectedInterfaceAddress;
	}

	/**
	 * Get the network address.
	 * 
	 * @return
	 */
	public Inet4Address getNetworAddress() {
		Inet4Address selectedInetAddress = null;
		try {
			InterfaceAddress interfaceAddress = null;
			if (selectedNetwork.length()>0) {
				String prefix = selectedNetwork.substring(0, selectedNetwork.indexOf('.')+1);
				Log.d(LOG_TAG, "prefix="+prefix);
				interfaceAddress = getPreferredInetAddress(prefix);
			} else {
				InterfaceAddress oneNineTwoInetAddress = getPreferredInetAddress("192.");
				if (oneNineTwoInetAddress != null) {
					interfaceAddress = oneNineTwoInetAddress;
				} else {
					InterfaceAddress oneSevenTwoInetAddress = getPreferredInetAddress("172.");
					if (oneSevenTwoInetAddress != null) {
						interfaceAddress = oneSevenTwoInetAddress;
					} else {
						interfaceAddress = getPreferredInetAddress("10.");
					}
				}
			}
			if (interfaceAddress != null) {
				InetAddress networkAddress = interfaceAddress.getAddress();
				Log.d(LOG_TAG, "networkAddress=" + networkAddress);
				if (networkAddress != null) {
					return (Inet4Address) networkAddress;
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
		progressDialog = new JDialog(this, resourceBundle.getString("progress.title"), true);
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
			selectedDialServer = (DialServer)cb.getSelectedItem();
			//selectedDialServer = servers.get(pos - 1);
			if (APP_ID.equals(FlingFrame.CHROMECAST)) {
				// Don't launch ChromeCast app now; there is a timeout that will
				// close the app if the media request isn't sent quickly.
				// Transcoding can take time to be ready to send video.
			} else {
				rampClient.launchApp(APP_ID, selectedDialServer);
			}
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
			boolean found = false;
			String[] extensions = transcodingExtensionValues.split(",");
			for (String extension : extensions) {
				if (file.endsWith(extension.trim())) {
					found = true;
					break;
				}
			}
			if (!found) {
				try {
					int pos = file.lastIndexOf('.');
					String extension = "";
					if (pos > -1) {
						extension = file.substring(pos);
					}
					final String url = "http://" + getNetworAddress().getHostAddress() + ":" + port + "/video" + extension;
					if (!rampClient.isClosed()) {
						rampClient.stop();
					}
					if (APP_ID.equals(FlingFrame.CHROMECAST)) {
						rampClient.launchApp(APP_ID, selectedDialServer);
						// wait for socket to be ready...
						new Thread(new Runnable() {
							public void run() {
								while (!rampClient.isStarted() && !rampClient.isClosed()) {
									try {
										Thread.sleep(500); // make less than 3
															// second ping time
									} catch (InterruptedException e) {
									}
								}
								if (!rampClient.isClosed()) {
									rampClient.load(url);
								}
							}
						}).start();
					} else {
						rampClient.load(url);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				vlcTranscode(file);
			}
		}
	}

	protected void vlcTranscode(String file) {
		// http://caprica.github.io/vlcj/javadoc/2.1.0/index.html
		try {
			// clean up previous session
			if (mediaPlayer != null) {
				mediaPlayer.release();
			}
			if (mediaPlayerFactory != null) {
				mediaPlayerFactory.release();
			}
			mediaPlayerFactory = new MediaPlayerFactory();
			mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();

			// Add a component to be notified of player events
			mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
				public void opening(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Opening");
				}

				public void buffering(MediaPlayer mediaPlayer, float newCache) {
					Log.d(LOG_TAG, "VLC Transcoding: Buffering");
				}

				public void playing(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Playing");
				}

				public void paused(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Paused");
				}

				public void stopped(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Stopped");
				}

				public void finished(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Finished");
				}

				public void error(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Error");
				}
			});

			// Find a port for VLC HTTP server
			boolean started = false;
			int vlcPort = port + 1;
			while (!started) {
				try {
					ServerSocket serverSocket = new ServerSocket(vlcPort);
					Log.d(LOG_TAG, "Available port for VLC: " + vlcPort);
					started = true;
					serverSocket.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					vlcPort++;
				} catch (Exception ex) {
					break;
				}
			}

			// Play a particular item, with options if necessary
			String options[] = { ":sout=#transcode{" + transcodingParameterValues + "}:http{mux=webm,dst=:" + vlcPort + "/cast.webm}", ":sout-keep" };
			mediaPlayer.playMedia(file, options);

			// http://192.168.0.8:8087/cast.webm
			final String url = "http://" + getNetworAddress().getHostAddress() + ":" + vlcPort + "/cast.webm";
			Log.d(LOG_TAG, "url=" + url);
			if (!rampClient.isClosed()) {
				rampClient.stop();
			}
			if (APP_ID.equals(FlingFrame.CHROMECAST)) {
				rampClient.launchApp(APP_ID, selectedDialServer);
				// wait for socket to be ready...
				new Thread(new Runnable() {
					public void run() {
						while (!rampClient.isStarted() && !rampClient.isClosed()) {
							try {
								Thread.sleep(500); // make less than 3
													// second ping time
							} catch (InterruptedException e) {
							}
						}
						if (!rampClient.isClosed()) {
							rampClient.load(url);
						}
					}
				}).start();
			} else {
				rampClient.load(url);
			}
		} catch (Throwable e) {
			Log.e(LOG_TAG, "vlcTranscode: " + file, e);
		}
	}

	private Properties loadProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
			transcodingParameterValues = prop.getProperty(PROPERTY_TRANSCODING_PARAMETERS);
			transcodingExtensionValues = prop.getProperty(PROPERTY_TRANSCODING_EXTENSIONS);
			selectedNetwork = prop.getProperty(SELECTED_NETWORK);
		} catch (Exception ex) {
		}
		if (transcodingParameterValues == null) {
			transcodingParameterValues = TRANSCODING_PARAMETERS;
		}
		if (transcodingExtensionValues == null) {
			transcodingExtensionValues = TRANSCODING_EXTENSIONS;
		}
		if (selectedNetwork == null) {
			selectedNetwork = "";
		}
		return prop;
	}

	private void storeProperties() {
		Properties prop = new Properties();
		try {
			prop.setProperty(PROPERTY_TRANSCODING_PARAMETERS, transcodingParameterValues);
			prop.setProperty(PROPERTY_TRANSCODING_EXTENSIONS, transcodingExtensionValues);
			prop.setProperty(SELECTED_NETWORK, selectedNetwork);
			prop.store(new FileOutputStream("config.properties"), null);
		} catch (Exception ex) {
		}
	}

	public void updateTime(int time) {
		label.setText(simpleDateFormat.format(new Date(time * 1000)));
	}

}
