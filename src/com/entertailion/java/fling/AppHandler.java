package com.entertailion.java.fling;

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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser for DIAL app responses
 * 
 * @author leon_nicholls
 * 
 */
public class AppHandler extends DefaultHandler {

	private static final String LOG_TAG = "AppHandler";

	private String connectionServiceUrl;
	private String state;
	private String protocol;

	// Current characters being accumulated
	private StringBuffer chars = new StringBuffer();

	public AppHandler() {
	}

	@Override
	public void endDocument() throws SAXException {
		// Nothing
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		chars.delete(0, chars.length());

	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if ("connectionSvcURL".equals(qName)) {
			connectionServiceUrl = chars.toString();
		} else if ("state".equals(qName)) {
			state = chars.toString();
		} else if ("protocol".equals(qName)) {
			protocol = chars.toString();
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		chars.append(new String(ch, start, length));
	}

	public String getConnectionServiceUrl() {
		return connectionServiceUrl;
	}

	public void setConnectionServiceUrl(String connectionServiceUrl) {
		this.connectionServiceUrl = connectionServiceUrl;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}