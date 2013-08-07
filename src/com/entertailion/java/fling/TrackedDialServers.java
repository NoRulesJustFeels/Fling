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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Keep track of the found DIAL servers
 * 
 * @author leon_nicholls
 */
public class TrackedDialServers implements Iterable<DialServer> {
	private static final String LOG_TAG = "TrackedDialServers";

	private final Map<InetAddress, DialServer> serversByAddress;
	private final SortedSet<DialServer> servers;
	private DialServer[] serverArray;

	private static Comparator<DialServer> COMPARATOR = new Comparator<DialServer>() {
		public int compare(DialServer remote1, DialServer remote2) {
			if (remote1.getFriendlyName() != null
					&& remote2.getFriendlyName() != null) {
				int result = remote1.getFriendlyName().compareToIgnoreCase(
						remote2.getFriendlyName());
				if (result != 0) {
					return result;
				}
			}
			return remote1.getIpAddress().getHostAddress()
					.compareTo(remote2.getIpAddress().getHostAddress());
		}
	};

	TrackedDialServers() {
		serversByAddress = new HashMap<InetAddress, DialServer>();
		servers = new TreeSet<DialServer>(COMPARATOR);
	}

	public boolean add(DialServer DialServer) {
		InetAddress address = DialServer.getIpAddress();
		if (!serversByAddress.containsKey(address)) {
			serversByAddress.put(address, DialServer);
			servers.add(DialServer);
			serverArray = null;
			return true;
		}
		return false;
	}

	public int size() {
		return servers.size();
	}

	public DialServer get(int index) {
		return getServerArray()[index];
	}

	private DialServer[] getServerArray() {
		if (serverArray == null) {
			serverArray = servers.toArray(new DialServer[0]);
		}
		return serverArray;
	}

	public Iterator<DialServer> iterator() {
		return servers.iterator();
	}

	public DialServer findDialServer(DialServer DialServer) {
		DialServer byIp = serversByAddress.get(DialServer.getIpAddress());
		if (byIp != null
				&& byIp.getFriendlyName().equals(DialServer.getFriendlyName())) {
			return byIp;
		}

		for (DialServer server : servers) {
			Log.d(LOG_TAG, "New server: " + server);
			if (DialServer.getFriendlyName().equals(server.getFriendlyName())) {
				return server;
			}
		}
		return byIp;
	}

	public TrackedDialServers clone() {
		TrackedDialServers trackedServers = new TrackedDialServers();
		for (DialServer server : servers) {
			trackedServers.add(server.clone());
		}
		return trackedServers;
	}
}