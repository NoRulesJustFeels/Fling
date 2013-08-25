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
 * DIAL protocol servers
 * 
 * @author leon_nicholls
 */
public class DialServer {
	private String location;
	private InetAddress ipAddress;
	private int port;
	private String appsUrl;
	private String friendlyName;
	private String uuid;
	private String manufacturer;
	private String modelName;
	
	public DialServer() {
		
	}

	public DialServer(String friendlyName, InetAddress ipAddress) {
		this("http://"+ipAddress.getHostAddress()+":8008/ssdp/device-desc.xml", ipAddress, 8008, "http://"+ipAddress.getHostAddress()+":8008/apps/", friendlyName, "uuid:74842a31-3226-1429-5dfb-29d6443e5695", "Google Inc.", "Eureka Dongle");
	}

	public DialServer(String location, InetAddress ipAddress, int port, String appsUrl, String friendlyName, String uuid, String manufacturer, String modelName) {
		this.location = location;
		this.ipAddress = ipAddress;
		this.port = port;
		this.appsUrl = appsUrl;
		this.friendlyName = friendlyName;
		this.uuid = uuid;
		this.manufacturer = manufacturer;
		this.modelName = modelName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAppsUrl() {
		return appsUrl;
	}

	public void setAppsUrl(String appsUrl) {
		this.appsUrl = appsUrl;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public DialServer clone() {
		return new DialServer(location, ipAddress, port, appsUrl, friendlyName, uuid, manufacturer, modelName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DialServer)) {
			return false;
		}
		DialServer that = (DialServer) obj;
		return equal(this.ipAddress, that.ipAddress) && (this.port == that.port);
	}

	private static <T> boolean equal(T obj1, T obj2) {
		if (obj1 == null) {
			return obj2 == null;
		}
		return obj1.equals(obj2);
	}

	@Override
	public String toString() {
		return getFriendlyName() + " / " + getIpAddress().getHostAddress();
	}

}
