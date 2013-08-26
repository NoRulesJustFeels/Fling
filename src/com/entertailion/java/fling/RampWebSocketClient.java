/*
 * Copyright (C) 2013 ENTERTAILION LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.entertailion.java.fling;

import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

/*
 * Web socket client for RAMP commands
 * 
 * @author leon_nicholls
 */
public class RampWebSocketClient extends WebSocketClient {

	private static final String LOG_TAG = "RampWebSocketClient";

	private RampWebSocketListener rampWebSocketListener;

	public RampWebSocketClient(URI uri, RampWebSocketListener rampWebSocketListener) {
		super(uri, new Draft_17());
		this.rampWebSocketListener = rampWebSocketListener;
		WebSocketImpl.DEBUG = true;
	}

	@Override
	public void onMessage(String message) {
		rampWebSocketListener.onMessage(message);
	}

	@Override
	public void onMessage(ByteBuffer blob) {
		getConnection().send(blob);
	}

	@Override
	public void onError(Exception ex) {
		rampWebSocketListener.onError(ex);
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		rampWebSocketListener.onOpen(handshake);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		rampWebSocketListener.onClose(code, reason, remote);
	}

	@Override
	public void send(String message) {
		Log.d(LOG_TAG, "message=" + message);
		super.send(message);
	}

}
