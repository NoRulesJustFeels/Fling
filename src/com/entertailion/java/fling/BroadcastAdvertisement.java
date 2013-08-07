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

import java.net.InetAddress;

/*
 HTTP/1.1 200 OK
 USN: uuid:d17c2986-4624-3f2c-93a5-fe3ef0a0ec9c::urn:dial-multiscreen-org:service:dial:1
 LOCATION: http://192.168.0.51:47944/dd.xml
 BOOTID.UPNP.ORG: 1287126024
 ST: urn:dial-multiscreen-org:service:dial:1
 CACHE-CONTROL: max-age=1800
 EXT
 */

/**
 * DIAL Broadcast Advertisement
 * 
 * @author leon_nicholls
 * 
 */

public final class BroadcastAdvertisement {

	private final String location;
	private final InetAddress ipAddress;
	private final int port;

	BroadcastAdvertisement(String location, InetAddress ipAddress, int port) {
		this.location = location;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public String getLocation() {
		return location;
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}
}
