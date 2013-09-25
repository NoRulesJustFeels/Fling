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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
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
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
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
public class FlingFrame extends JFrame implements ActionListener, BroadcastDiscoveryHandler, ChangeListener, WindowListener {
	private static final String LOG_TAG = "FlingFrame";

	// https://www.gstatic.com/cv/receiver.html?${POST_DATA}
	public static final String CHROMECAST = "ChromeCast";
	// TODO Add your own app id here
	private static final String APP_ID = "YOUR_APP_ID_HERE";

	private static final String HEADER_APPLICATION_URL = "Application-URL";
	private static final String CHROME_CAST_MODEL_NAME = "Eureka Dongle";
	private static final String TRANSCODING_EXTENSIONS = "wmv,avi,mkv,mpg,mpeg,flv,3gp,ogm";
	private static final String TRANSCODING_PARAMETERS = "vcodec=VP80,vb=1000,vfilter=canvas{width=640,height=360},acodec=vorb,ab=128,channels=2,samplerate=44100,threads=2";
	private static final String PROPERTY_TRANSCODING_EXTENSIONS = "transcoding.extensions";
	private static final String PROPERTY_TRANSCODING_PARAMETERS = "transcoding.parameters";
	private static final String PROPERTY_MANUAL_SERVERS = "manual.servers";
	private int port = EmbeddedServer.HTTP_PORT;
	private List<DialServer> servers = new ArrayList<DialServer>();
	private List<DialServer> manualServers = new ArrayList<DialServer>();
	private JComboBox deviceList;
	private JDialog progressDialog;
	private JButton refreshButton, playButton, pauseButton, stopButton, settingsButton;
	private JLabel label;
	private JSlider scrubber;
	private JSlider volume;
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

	private int duration;
	private boolean playbackValueIsAdjusting;
	private boolean isTranscoding;
	private String appId;

	public FlingFrame(String appId) {
		super();
		this.appId = appId;
		rampClient = new RampClient(this);

		addWindowListener(this);

		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		Locale locale = Locale.getDefault();
		resourceBundle = ResourceBundle.getBundle("com/entertailion/java/fling/resources/resources", locale);
		setTitle(MessageFormat.format(resourceBundle.getString("fling.title"), Fling.VERSION));

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
		icon = new ImageIcon(url, resourceBundle.getString("settings.title"));
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
				labelPanel.add(new JLabel(resourceBundle.getString("device.manual"), JLabel.RIGHT));
				JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				final JComboBox manualDeviceList = new JComboBox();
				if (manualServers.size() == 0) {
					manualDeviceList.setVisible(false);
				} else {
					for (DialServer dialServer : manualServers) {
						manualDeviceList.addItem(dialServer);
					}
				}
				devicePanel.add(manualDeviceList);
				JButton addButton = new JButton(resourceBundle.getString("device.manual.add"));
				addButton.setToolTipText(resourceBundle.getString("device.manual.add.tooltip"));
				addButton.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						JTextField name = new JTextField();
						JTextField ipAddress = new JTextField();
						Object[] message = { resourceBundle.getString("device.manual.name") + ":", name,
								resourceBundle.getString("device.manual.ipaddress") + ":", ipAddress };

						int option = JOptionPane.showConfirmDialog(null, message, resourceBundle.getString("device.manual"), JOptionPane.OK_CANCEL_OPTION);
						if (option == JOptionPane.OK_OPTION) {
							try {
								manualServers.add(new DialServer(name.getText(), InetAddress.getByName(ipAddress.getText())));

								Object selected = deviceList.getSelectedItem();
								int selectedIndex = deviceList.getSelectedIndex();
								deviceList.removeAllItems();
								deviceList.addItem(resourceBundle.getString("devices.select"));
								for (DialServer dialServer : servers) {
									deviceList.addItem(dialServer);
								}
								for (DialServer dialServer : manualServers) {
									deviceList.addItem(dialServer);
								}
								deviceList.invalidate();
								if (selectedIndex > 0) {
									deviceList.setSelectedItem(selected);
								} else {
									if (deviceList.getItemCount() == 2) {
										// Automatically select single device
										deviceList.setSelectedIndex(1);
									}
								}

								manualDeviceList.removeAllItems();
								for (DialServer dialServer : manualServers) {
									manualDeviceList.addItem(dialServer);
								}
								manualDeviceList.setVisible(true);
								storeProperties();
							} catch (UnknownHostException e1) {
								Log.e(LOG_TAG, "manual IP address", e1);

								JOptionPane.showMessageDialog(FlingFrame.this, resourceBundle.getString("device.manual.invalidip"));
							}
						}
					}
				});
				devicePanel.add(addButton);
				JButton removeButton = new JButton(resourceBundle.getString("device.manual.remove"));
				removeButton.setToolTipText(resourceBundle.getString("device.manual.remove.tooltip"));
				removeButton.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						Object selected = manualDeviceList.getSelectedItem();
						manualDeviceList.removeItem(selected);
						if (manualDeviceList.getItemCount() == 0) {
							manualDeviceList.setVisible(false);
						}
						deviceList.removeItem(selected);
						deviceList.invalidate();
						manualServers.remove(selected);
						storeProperties();
					}
				});
				devicePanel.add(removeButton);
				fieldPanel.add(devicePanel);
				int result = JOptionPane.showConfirmDialog(FlingFrame.this, myPanel, resourceBundle.getString("settings.title"), JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					transcodingParameterValues = transcodingParameters.getText();
					transcodingExtensionValues = transcodingExtensions.getText();
					storeProperties();
				}
			}
		});
		settingsButton.setToolTipText(resourceBundle.getString("settings.title"));
		devicePane.add(settingsButton);
		listPane.add(devicePane);

		// TODO
		volume = new JSlider(JSlider.VERTICAL, 0, 100, 0);
		volume.setUI(new MySliderUI(volume));
		volume.setMajorTickSpacing(25);
		// volume.setMinorTickSpacing(5);
		volume.setPaintTicks(true);
		volume.setEnabled(true);
		volume.setValue(100);
		volume.setToolTipText(resourceBundle.getString("volume.title"));
		volume.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int position = (int) source.getValue();
					rampClient.volume(position / 100.0f);
				}
			}

		});
		JPanel centerPanel = new JPanel(new BorderLayout());
		// centerPanel.add(volume, BorderLayout.WEST);

		centerPanel.add(DragHereIcon.makeUI(this), BorderLayout.CENTER);
		listPane.add(centerPanel);

		scrubber = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		scrubber.addChangeListener(this);
		scrubber.setMajorTickSpacing(25);
		scrubber.setMinorTickSpacing(5);
		scrubber.setPaintTicks(true);
		scrubber.setEnabled(false);
		listPane.add(scrubber);

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
				setDuration(0);
				scrubber.setValue(0);
				scrubber.setEnabled(false);
			}
		});
		buttonPane.add(stopButton);
		listPane.add(buttonPane);
		getContentPane().add(listPane);

		createProgressDialog();
		startWebserver();
		discoverDevices();
	}

	// http://stackoverflow.com/questions/6992633/painting-the-slider-icon-of-jslider
	private static class MySliderUI extends BasicSliderUI {

		private Font font = new Font(Font.SERIF, Font.PLAIN, 12);

		public MySliderUI(JSlider slider) {
			super(slider);
		}

		@Override
		public void paintTrack(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Rectangle t = trackRect;
			g.setColor(Color.darkGray);
			g.drawRect(t.x, t.y, t.width, t.height);

			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setFont(font);
				int w2 = slider.getFontMetrics(font).stringWidth("Volume") / 2;
				g2.drawString("Volume", t.width / 2 - w2, t.height);
			}
		}

		@Override
		public void paintThumb(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Rectangle t = thumbRect;
			g2d.setColor(Color.black);
			int tw2 = t.width / 2;
			g2d.drawLine(t.x, t.y, t.x + t.width - 1, t.y);
			g2d.drawLine(t.x, t.y, t.x + tw2, t.y + t.height);
			g2d.drawLine(t.x + t.width - 1, t.y, t.x + tw2, t.y + t.height);
		}
	}

	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		if (!source.getValueIsAdjusting() && !playbackValueIsAdjusting) {
			int position = (int) source.getValue();
			if (position == 0) {
				rampClient.play(0);
			} else {
				rampClient.play((int) (position / 100.0f * duration));
			}
		}
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

		broadcastClient = new BroadcastDiscoveryClient(this);
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
					Thread.sleep(BroadcastDiscoveryClient.PROBE_INTERVAL_MS-1);

					broadcastClient.stop();

					hideProgressDialog();

					Log.d(LOG_TAG, "size=" + trackedServers.size());
					for (DialServer dialServer : trackedServers) {
						deviceList.addItem(dialServer);
						servers.add(dialServer);
					}

					// Now add user's manual servers
					for (DialServer dialServer : manualServers) {
						deviceList.addItem(dialServer);
					}
					deviceList.invalidate();

					if (deviceList.getItemCount() == 1) {
						JOptionPane.showMessageDialog(FlingFrame.this, resourceBundle.getString("device.notfound"));
					} else if (deviceList.getItemCount() == 2) {
						// Automatically select single device
						deviceList.setSelectedIndex(1);
					}

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

	private InterfaceAddress getPreferredInetAddress(String prefix) {
		InterfaceAddress selectedInterfaceAddress = null;
		try {
			Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();

			while (list.hasMoreElements()) {
				NetworkInterface iface = list.nextElement();
				if (iface == null)
					continue;
				Log.d(LOG_TAG, "interface=" + iface.getName());
				Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
				while (it.hasNext()) {
					InterfaceAddress interfaceAddress = it.next();
					if (interfaceAddress == null)
						continue;
					InetAddress address = interfaceAddress.getAddress();
					Log.d(LOG_TAG, "address=" + address);
					if (address instanceof Inet4Address) {
						// Only pick an interface that is likely to be on the
						// same subnet as the selected ChromeCast device
						if (address.getHostAddress().toString().startsWith(prefix)) {
							return interfaceAddress;
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
			if (selectedDialServer != null) {
				String address = selectedDialServer.getIpAddress().getHostAddress();
				String prefix = address.substring(0, address.indexOf('.') + 1);
				Log.d(LOG_TAG, "prefix=" + prefix);
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
			selectedDialServer = (DialServer) cb.getSelectedItem();
		}
	}

	/**
	 * Send a uri to the ChromeCast device
	 * 
	 * @param keycode
	 */
	protected void sendMediaUrl(String file) {
		if (selectedDialServer == null) {
			JOptionPane.showMessageDialog(this, resourceBundle.getString("device.select"));
			return;
		}
		isTranscoding = false;
		Log.d(LOG_TAG, "sendMediaUrl=" + file);
		if (file != null) {
			duration = 0;
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
					Inet4Address address = getNetworAddress();
					if (address != null) {
						final String url = "http://" + address.getHostAddress() + ":" + port + "/video" + extension;
						if (!rampClient.isClosed()) {
							rampClient.stop();
						}
						rampClient.launchApp(appId==null?APP_ID:appId, selectedDialServer);
						// wait for socket to be ready...
						new Thread(new Runnable() {
							public void run() {
								while (!rampClient.isStarted() && !rampClient.isClosed()) {
									try {
										// make less than 3 second ping time
										Thread.sleep(500);
									} catch (InterruptedException e) {
									}
								}
								if (!rampClient.isClosed()) {
									try {
										Thread.sleep(500);
									} catch (InterruptedException e) {
									}
									rampClient.load(url);
								}
							}
						}).start();
					} else {
						Log.d(LOG_TAG, "could not find a network interface");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				vlcTranscode(file);
			}

			// TODO
			if (!volume.getValueIsAdjusting()) {
				int position = (int) volume.getValue();
				// rampClient.volume(position / 100.0f);
			}
		}
	}

	protected void vlcTranscode(final String file) {
		// Transcoding does not support jumps
		isTranscoding = true;

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

					setDuration((int) (mediaPlayer.getLength() / 1000.0f));
					Log.d(LOG_TAG, "duration=" + duration);
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

				public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
					Log.d(LOG_TAG, "VLC Transcoding: VideoOutput");
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

			if (!rampClient.isClosed()) {
				rampClient.stop();
			}
			Inet4Address address = getNetworAddress();
			if (address != null) {
				// Play a particular item, with options if necessary
				final String options[] = { ":sout=#transcode{" + transcodingParameterValues + "}:http{mux=webm,dst=:" + vlcPort + "/cast.webm}", ":sout-keep" };
				// http://192.168.0.8:8087/cast.webm
				final String url = "http://" + address.getHostAddress() + ":" + vlcPort + "/cast.webm";
				Log.d(LOG_TAG, "url=" + url);
				if (true || isChromeCast()) {
					rampClient.launchApp(appId==null?APP_ID:appId, selectedDialServer);
					// wait for socket to be ready...
					new Thread(new Runnable() {
						public void run() {
							while (!rampClient.isStarted() && !rampClient.isClosed()) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
								}
							}
							if (!rampClient.isClosed()) {
								mediaPlayer.playMedia(file, options);
								rampClient.load(url);
							}
						}
					}).start();
				} else {
					rampClient.load(url);
				}
			} else {
				Log.d(LOG_TAG, "could not find a network interface");
			}
		} catch (Throwable e) {
			Log.e(LOG_TAG, "vlcTranscode: " + file, e);
		}
	}

	// Store user settings
	private Properties loadProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
			transcodingParameterValues = prop.getProperty(PROPERTY_TRANSCODING_PARAMETERS);
			transcodingExtensionValues = prop.getProperty(PROPERTY_TRANSCODING_EXTENSIONS);
			String manual = prop.getProperty(PROPERTY_MANUAL_SERVERS);
			if (manual != null) {
				String[] parts = manual.split(":");
				for (int i = 0; i < parts.length / 2; i++) {
					manualServers.add(new DialServer(parts[i * 2], InetAddress.getByName(parts[i * 2 + 1])));
				}
			}
		} catch (Exception ex) {
		}
		if (transcodingParameterValues == null) {
			transcodingParameterValues = TRANSCODING_PARAMETERS;
		}
		if (transcodingExtensionValues == null) {
			transcodingExtensionValues = TRANSCODING_EXTENSIONS;
		}
		return prop;
	}

	// Restore user settings
	private void storeProperties() {
		Properties prop = new Properties();
		try {
			prop.setProperty(PROPERTY_TRANSCODING_PARAMETERS, transcodingParameterValues);
			prop.setProperty(PROPERTY_TRANSCODING_EXTENSIONS, transcodingExtensionValues);
			String manual = "";
			for (DialServer dialServer : manualServers) {
				if (manual.length() > 0) {
					manual = manual + ":";
				}
				manual = manual + dialServer.getFriendlyName() + ":" + dialServer.getIpAddress().getHostAddress();
			}
			prop.setProperty(PROPERTY_MANUAL_SERVERS, manual);
			prop.store(new FileOutputStream("config.properties"), null);
		} catch (Exception ex) {
		}
	}

	public void updateTime(int time) {
		label.setText(simpleDateFormat.format(new Date(time * 1000)));
		if (duration > 0 && !scrubber.getValueIsAdjusting()) {
			playbackValueIsAdjusting = true;
			scrubber.setValue((int) ((time * 1.0f) / duration * 100));
			playbackValueIsAdjusting = false;
		}
	}

	// Current video duration in seconds
	public int getDuration() {
		return duration;
	}

	// Display the current duration in the slider
	public void setDuration(int duration) {
		this.duration = duration;
		if (duration >= 0) {
			Hashtable labelTable = new Hashtable();
			labelTable.put(new Integer(0), new JLabel("0"));
			labelTable.put(new Integer(100), new JLabel(simpleDateFormat.format(new Date(duration * 1000))));
			scrubber.setLabelTable(labelTable);
			scrubber.setPaintLabels(true);
			if (!isTranscoding) {
				scrubber.setEnabled(true);
			} else {
				scrubber.setEnabled(false);
			}
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		if (rampClient != null) {
			rampClient.stop();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public boolean isChromeCast() {
		String id = appId==null?APP_ID:appId;
		return id.equals(FlingFrame.CHROMECAST);
	}

}
