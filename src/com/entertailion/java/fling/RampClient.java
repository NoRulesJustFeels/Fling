/*
 * Copyright (C) 2013 ENTERTAILION, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.entertailion.java.fling;

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/*
 * Manage RAMP protocol 
 * 
 * @author leon_nicholls
 */
public class RampClient implements RampWebSocketListener {

	private static final String LOG_TAG = "RampClient";

	private static final String STATE_RUNNING = "running";
	private static final String STATE_STOPPED = "stopped";

	private static final String HEADER_CONNECTION = "Connection";
	private static final String HEADER_CONNECTION_VALUE = "keep-alive";
	private static final String HEADER_ORIGN = "Origin";
	private static final String HEADER_ORIGIN_VALUE = "chrome-extension://boadgeojelhgndaghljhdicfkmllpafd";
	private static final String HEADER_USER_AGENT = "User-Agent";
	private static final String HEADER_USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.71 Safari/537.36";
	private static final String HEADER_DNT = "DNT";
	private static final String HEADER_DNT_VALUE = "1";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String HEADER_ACCEPT_ENCODING_VALUE = "gzip,deflate,sdch";
	private static final String HEADER_ACCEPT = "Accept";
	private static final String HEADER_ACCEPT_VALUE = "*/*";
	private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
	private static final String HEADER_ACCEPT_LANGUAGE_VALUE = "en-US,en;q=0.8";
	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String HEADER_CONTENT_TYPE_JSON_VALUE = "application/json";
	private static final String HEADER_CONTENT_TYPE_TEXT_VALUE = "text/plain";

	private String connectionServiceUrl;
	private String state;
	private String protocol;
	private String response;
	private boolean started;
	private boolean closed;

	private RampWebSocketClient rampWebSocketClient;
	private int commandId;
	private String app;
	private String activityId;
	private String senderId;

	private Thread pongThread;
	private DialServer dialServer;
	private FlingFrame flingFrame;

	public RampClient(FlingFrame flingFrame) {
		this.flingFrame = flingFrame;
		this.senderId = UUID.randomUUID().toString();
	}

	public void launchApp(String app, DialServer dialServer) {
		this.app = app;
		this.dialServer = dialServer;
		this.activityId = UUID.randomUUID().toString();
		try {
			String device = "http://" + dialServer.getIpAddress().getHostAddress() + ":" + dialServer.getPort();
			Log.d(LOG_TAG, "device=" + device);
			Log.d(LOG_TAG, "apps url=" + dialServer.getAppsUrl());

			// application instance url
			String location = null;

			DefaultHttpClient defaultHttpClient = HttpRequestHelper.createHttpClient();
			CustomRedirectHandler handler = new CustomRedirectHandler();
			defaultHttpClient.setRedirectHandler(handler);
			BasicHttpContext localContext = new BasicHttpContext();

			// check if any app is running
			HttpGet httpGet = new HttpGet(dialServer.getAppsUrl());
			httpGet.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
			httpGet.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
			httpGet.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
			httpGet.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
			httpGet.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
			httpGet.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
			HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
			if (httpResponse != null) {
				int responseCode = httpResponse.getStatusLine().getStatusCode();
				Log.d(LOG_TAG, "get response code=" + httpResponse.getStatusLine().getStatusCode());
				if (responseCode == 204) {
					// nothing is running
				} else if (responseCode == 200) {
					// app is running

					// Need to get real URL after a redirect
					// http://stackoverflow.com/a/10286025/594751
					String lastUrl = dialServer.getAppsUrl();
					if (handler.lastRedirectedUri != null) {
						lastUrl = handler.lastRedirectedUri.toString();
						Log.d(LOG_TAG, "lastUrl=" + lastUrl);
					}

					String response = EntityUtils.toString(httpResponse.getEntity());
					Log.d(LOG_TAG, "get response=" + response);
					parseXml(new StringReader(response));

					Header[] headers = httpResponse.getAllHeaders();
					for (int i = 0; i < headers.length; i++) {
						Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
					}

					// stop the app instance
					HttpDelete httpDelete = new HttpDelete(lastUrl);
					httpResponse = defaultHttpClient.execute(httpDelete);
					if (httpResponse != null) {
						Log.d(LOG_TAG, "delete response code=" + httpResponse.getStatusLine().getStatusCode());
						response = EntityUtils.toString(httpResponse.getEntity());
						Log.d(LOG_TAG, "delete response=" + response);
					} else {
						Log.d(LOG_TAG, "no delete response");
					}
				}

			} else {
				Log.i(LOG_TAG, "no get response");
				return;
			}

			// Check if app is installed on device
			int responseCode = getAppStatus(defaultHttpClient, dialServer.getAppsUrl() + app);
			if (responseCode != 200) {
				return;
			}
			parseXml(new StringReader(response));
			Log.d(LOG_TAG, "state=" + state);

			// start the app with POST
			HttpPost httpPost = new HttpPost(dialServer.getAppsUrl() + app);
			httpPost.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
			httpPost.setHeader(HEADER_ORIGN, HEADER_ORIGIN_VALUE);
			httpPost.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
			httpPost.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
			httpPost.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
			httpPost.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
			httpPost.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
			httpPost.setHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_TEXT_VALUE);
			if (app.equals(FlingFrame.CHROMECAST)) {
				httpPost.setEntity(new StringEntity("v=release-d4fa0a24f89ec5ba83f7bf3324282c8d046bf612&id=local%3A1&idle=windowclose"));
			}

			httpResponse = defaultHttpClient.execute(httpPost, localContext);
			if (httpResponse != null) {
				Log.d(LOG_TAG, "post response code=" + httpResponse.getStatusLine().getStatusCode());
				response = EntityUtils.toString(httpResponse.getEntity());
				Log.d(LOG_TAG, "post response=" + response);
				Header[] headers = httpResponse.getHeaders("LOCATION");
				if (headers.length > 0) {
					location = headers[0].getValue();
					Log.d(LOG_TAG, "post response location=" + location);
				}

				headers = httpResponse.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
				}
			} else {
				Log.i(LOG_TAG, "no post response");
				return;
			}

			// Keep trying to get the app status until the
			// connection service URL is available
			state = STATE_STOPPED;
			do {
				responseCode = getAppStatus(defaultHttpClient, dialServer.getAppsUrl() + app);
				if (responseCode != 200) {
					break;
				}
				parseXml(new StringReader(response));
				Log.d(LOG_TAG, "state=" + state);
				Log.d(LOG_TAG, "connectionServiceUrl=" + connectionServiceUrl);
				Log.d(LOG_TAG, "protocol=" + protocol);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			} while (state.equals(STATE_RUNNING) && connectionServiceUrl == null);

			if (connectionServiceUrl == null) {
				Log.i(LOG_TAG, "connectionServiceUrl is null");
				return; // oops, something went wrong
			}

			// get the websocket URL
			String webSocketAddress = null;
			httpPost = new HttpPost(connectionServiceUrl); // "http://192.168.0.17:8008/connection/YouTube"
			httpPost.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
			httpPost.setHeader(HEADER_ORIGN, HEADER_ORIGIN_VALUE);
			httpPost.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
			httpPost.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
			httpPost.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
			httpPost.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
			httpPost.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
			httpPost.setHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_JSON_VALUE);
			httpPost.setEntity(new StringEntity("{\"channel\":0,\"senderId\":{\"appName\":\"" + app + "\", \"senderId\":\"" + senderId + "\"}}"));

			httpResponse = defaultHttpClient.execute(httpPost, localContext);
			if (httpResponse != null) {
				responseCode = httpResponse.getStatusLine().getStatusCode();
				Log.d(LOG_TAG, "post response code=" + responseCode);
				if (responseCode == 200) {
					// should return JSON payload
					response = EntityUtils.toString(httpResponse.getEntity());
					Log.d(LOG_TAG, "post response=" + response);
					Header[] headers = httpResponse.getAllHeaders();
					for (int i = 0; i < headers.length; i++) {
						Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
					}

					// http://code.google.com/p/json-simple/
					JSONParser parser = new JSONParser();
					try {
						Object obj = parser.parse(new StringReader(response)); // {"URL":"ws://192.168.0.17:8008/session?33","pingInterval":0}
						JSONObject jsonObject = (JSONObject) obj;
						webSocketAddress = (String) jsonObject.get("URL");
						Log.d(LOG_TAG, "webSocketAddress: " + webSocketAddress);
						long pingInterval = (Long) jsonObject.get("pingInterval"); // TODO
					} catch (Exception e) {
						Log.e(LOG_TAG, "parse JSON", e);
					}
				}
			} else {
				Log.i(LOG_TAG, "no post response");
				return;
			}

			// Make a web socket connection for doing RAMP
			// to control media playback
			this.started = false;
			this.closed = false;
			if (webSocketAddress != null) {
				// https://github.com/TooTallNate/Java-WebSocket
				URI uri = URI.create(webSocketAddress);

				rampWebSocketClient = new RampWebSocketClient(uri, this);

				new Thread(new Runnable() {
					public void run() {
						Thread t = new Thread(rampWebSocketClient);
						t.start();
						try {
							t.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						} finally {
							rampWebSocketClient.close();
						}
					}
				}).start();
			} else {
				Log.i(LOG_TAG, "webSocketAddress is null");
			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "launchApp", e);
		}
	}

	public void closeCurrentApp() {
		if (dialServer != null) {
			try {
				DefaultHttpClient defaultHttpClient = HttpRequestHelper.createHttpClient();
				CustomRedirectHandler handler = new CustomRedirectHandler();
				defaultHttpClient.setRedirectHandler(handler);
				BasicHttpContext localContext = new BasicHttpContext();

				// check if any app is running
				HttpGet httpGet = new HttpGet(dialServer.getAppsUrl());
				httpGet.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
				httpGet.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
				httpGet.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
				httpGet.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
				httpGet.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
				httpGet.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
				HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
				if (httpResponse != null) {
					int responseCode = httpResponse.getStatusLine().getStatusCode();
					Log.d(LOG_TAG, "get response code=" + httpResponse.getStatusLine().getStatusCode());
					if (responseCode == 204) {
						// nothing is running
					} else if (responseCode == 200) {
						// app is running

						// Need to get real URL after a redirect
						// http://stackoverflow.com/a/10286025/594751
						String lastUrl = dialServer.getAppsUrl();
						if (handler.lastRedirectedUri != null) {
							lastUrl = handler.lastRedirectedUri.toString();
							Log.d(LOG_TAG, "lastUrl=" + lastUrl);
						}

						String response = EntityUtils.toString(httpResponse.getEntity());
						Log.d(LOG_TAG, "get response=" + response);
						parseXml(new StringReader(response));

						Header[] headers = httpResponse.getAllHeaders();
						for (int i = 0; i < headers.length; i++) {
							Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
						}

						// stop the app instance
						HttpDelete httpDelete = new HttpDelete(lastUrl);
						httpResponse = defaultHttpClient.execute(httpDelete);
						if (httpResponse != null) {
							Log.d(LOG_TAG, "delete response code=" + httpResponse.getStatusLine().getStatusCode());
							response = EntityUtils.toString(httpResponse.getEntity());
							Log.d(LOG_TAG, "delete response=" + response);
						} else {
							Log.d(LOG_TAG, "no delete response");
						}
					}

				} else {
					Log.i(LOG_TAG, "no get response");
					return;
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "closeCurrentApp", e);
			}
		}
	}

	/**
	 * Do HTTP GET for app status to determine response code and response body
	 * 
	 * @param defaultHttpClient
	 * @param url
	 * @return
	 */
	private int getAppStatus(DefaultHttpClient defaultHttpClient, String url) {
		int responseCode = 200;
		try {
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
			if (httpResponse != null) {
				responseCode = httpResponse.getStatusLine().getStatusCode();
				Log.d(LOG_TAG, "get response code=" + responseCode);
				response = EntityUtils.toString(httpResponse.getEntity());
				Log.d(LOG_TAG, "get response=" + response);
			} else {
				Log.i(LOG_TAG, "no get response");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getAppStatus", e);
		}
		return responseCode;
	}

	private void parseXml(Reader reader) {
		try {
			InputSource inStream = new org.xml.sax.InputSource();
			inStream.setCharacterStream(reader);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			AppHandler appHandler = new AppHandler();
			xr.setContentHandler(appHandler);
			xr.parse(inStream);

			connectionServiceUrl = appHandler.getConnectionServiceUrl();
			state = appHandler.getState();
			protocol = appHandler.getProtocol();
		} catch (Exception e) {
			Log.e(LOG_TAG, "parse device description", e);
		}
	}

	/**
	 * Custom HTTP redirection handler to keep track of the redirected URL
	 * ChromeCast web server will redirect "/apps" to "/apps/YouTube" if that is
	 * the active/last app
	 * 
	 */
	public class CustomRedirectHandler extends DefaultRedirectHandler {

		public URI lastRedirectedUri;

		@Override
		public boolean isRedirectRequested(HttpResponse response, HttpContext context) {

			return super.isRedirectRequested(response, context);
		}

		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {

			lastRedirectedUri = super.getLocationURI(response, context);

			return lastRedirectedUri;
		}

	}

	// RampWebSocketListener callbacks
	public void onMessage(String message) {
		Log.d(LOG_TAG, "onMessage: message" + message);

		// TODO only respond based on interval
		// rampWebSocketClient.send("[\"cm\",{\"type\":\"pong\"}]");

		// ["cv",{"type":"activity","message":{"type":"timeupdate","activityId":"d82cede3-ec23-4f73-8abc-343dd9ca6dbb","state":{"mediaUrl":"http://192.168.0.50:8087/cast.webm","videoUrl":"http://192.168.0.50:8087/cast.webm","currentTime":20.985000610351562,"duration":null,"pause":false,"muted":false,"volume":1,"paused":false}}}]
		// Should really parse JSON, but only need the current time
		if (flingFrame.getDuration() == 0) {
			int start = message.indexOf("\"duration\":");
			if (start > 0) {
				int end = message.indexOf(",", start);
				String time = message.substring(start + 11, end);
				Log.d(LOG_TAG, "duration=" + time);
				if (!time.equals("null")) {
					flingFrame.setDuration((int) Float.parseFloat(time));
				}
			}
		}
		int start = message.indexOf("\"currentTime\":");
		if (start > 0) {
			int end = message.indexOf(",", start);
			String time = message.substring(start + 14, end);
			Log.d(LOG_TAG, "currentTime=" + time);
			flingFrame.updateTime((int) Float.parseFloat(time));
		}
	}

	public void onError(Exception ex) {
		Log.d(LOG_TAG, "onError: ex" + ex);
		ex.printStackTrace();

		started = false;
		closed = true;

		pongThread.interrupt();
	}

	public void onOpen(ServerHandshake handshake) {
		Log.d(LOG_TAG, "onOpen: handshake" + handshake);

		started = true;
		closed = false;

		if (pongThread != null) {
			pongThread.interrupt();
		}

		pongThread = new Thread(new Runnable() {
			public void run() {
				while (started && !closed) {
					try {
						rampWebSocketClient.send("[\"cm\",{\"type\":\"pong\"}]");
						try {
							Thread.sleep(3000); // pong every 3 seconds to keep
												// the app alive
						} catch (InterruptedException e) {
						}
					} catch (Exception e) {
						Log.e(LOG_TAG, "pongThread", e);
					}
				}
			}
		});
		pongThread.start();
	}

	public void onClose(int code, String reason, boolean remote) {
		Log.d(LOG_TAG, "onClose: code" + code + ", reason=" + reason + ", remote=" + remote);

		closed = true;
		started = false;

		pongThread.interrupt();

		flingFrame.updateTime(0);
	}

	// Media playback controls
	public void play() {
		if (rampWebSocketClient != null) {
			rampWebSocketClient.send("[\"ramp\",{\"type\":\"PLAY\", \"cmd_id\":" + commandId + "}]");
			commandId++;
		}
	}

	public void play(int position) {
		if (rampWebSocketClient != null) {
			rampWebSocketClient.send("[\"ramp\",{\"type\":\"PLAY\", \"cmd_id\":" + commandId + ", \"position\":" + position + "}]");
			commandId++;
		}
	}

	public void pause() {
		if (rampWebSocketClient != null) {
			rampWebSocketClient.send("[\"ramp\",{\"type\":\"STOP\", \"cmd_id\":" + commandId + "}]");
			commandId++;
		}
	}

	public void stop() {
		// ChromeCast app stop behaves like pause
		/*
		 * if (rampWebSocketClient != null) {
		 * rampWebSocketClient.send("[\"ramp\",{\"type\":\"STOP\", \"cmd_id\":"
		 * + commandId + "}]"); commandId++; }
		 */
		// Close the current app
		closeCurrentApp();
	}

	// Load media
	public void load(String url) {
		if (rampWebSocketClient != null) {
			if (app.equals(FlingFrame.CHROMECAST)) {
				rampWebSocketClient
						.send("[\"cv\",{\"type\":\"launch_service\",\"message\":{\"action\":\"launch\",\"activityType\":\"video_playback\",\"activityId\":\""
								+ activityId + "\",\"senderId\":\"" + senderId
								+ "\",\"receiverId\":\"local:1\",\"disconnectPolicy\":\"continue\",\"initParams\":{\"mediaUrl\":\"" + url
								+ "\",\"videoUrl\":\"" + url + "\",\"currentTime\":0,\"duration\":0,\"pause\":false,\"muted\":false,\"volume\":1}}}]");
			} else {
				rampWebSocketClient.send("[\"ramp\",{\"title\":\"Video\",\"src\":\"" + url + "\",\"type\":\"LOAD\",\"cmd_id\":" + commandId
						+ ",\"autoplay\":true}]");
			}
			commandId++;
		}
	}

	// Web socket status
	public boolean isStarted() {
		return started;
	}

	public boolean isClosed() {
		return closed;
	}
}
