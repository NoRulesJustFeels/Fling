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
 * XML parser for DIAL device description
 * 
 * @author leon_nicholls
 * 
 */
public class BroadcastHandler extends DefaultHandler {

	private static final String LOG_TAG = "BroadcastHandler";

	private DialServer dialServer;

	// Current characters being accumulated
	private StringBuffer chars = new StringBuffer();

	public BroadcastHandler() {
	}

	public DialServer getDialServer() {
		return dialServer;
	}

	@Override
	public void startDocument() throws SAXException {
		dialServer = new DialServer();
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
		if (qName.equals("friendlyName")) {
			dialServer.setFriendlyName(chars.toString());
		} else if (qName.equals("UDN")) {
			dialServer.setUuid(chars.toString());
		} else if (qName.equals("manufacturer")) {
			dialServer.setManufacturer(chars.toString());
		} else if (qName.equals("modelName")) {
			dialServer.setModelName(chars.toString());
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		chars.append(new String(ch, start, length));
	}

}