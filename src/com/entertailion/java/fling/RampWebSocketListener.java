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

import org.java_websocket.handshake.ServerHandshake;

/*
 * Callback for web socket events
 * 
 * @author leon_nicholls
 */
public interface RampWebSocketListener {
	public void onMessage(String message);

	public void onError(Exception ex);

	public void onOpen(ServerHandshake handshake);

	public void onClose(int code, String reason, boolean remote);
}
