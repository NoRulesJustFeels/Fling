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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Custom HTTP server to send video file to ChromeCast device media player;
 * support range requests
 * 
 * @author leon_nicholls
 * 
 */
public class EmbeddedServer extends HttpServer {
	private static final String LOG_TAG = "EmbeddedServer";
	public static final int HTTP_PORT = 8080;
	public static final String CURRENT_FILE = "current.file";
	private static final int BUFFER_SIZE = 1024 * 500;

	public EmbeddedServer(int port) throws IOException {
		super(port);
	}

	public Response serve(String uri, String method, Properties header,
			Properties parms, Properties files) {
		try {
			Log.d(LOG_TAG, method + " '" + uri + "' ");

			return serveFile(uri, header, new File("."), true, parms);
		} catch (Throwable e) {
			Log.e(LOG_TAG, "serve", e);
		}
		return new Response("200", "text/plain", "Ok");
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	public Response serveFile(String uri, Properties header, File homeDir,
			boolean allowDirectoryListing, Properties parms) {
		Response res = null;

		// Get MIME type from file name extension, if possible
		String mime = null;
		int dot = uri.lastIndexOf('.');
		if (dot >= 0) {
			String extension = uri.substring(dot + 1).toLowerCase();
			mime = (String) theMimeTypes.get(extension);
		}
		if (mime == null)
			mime = MIME_DEFAULT_BINARY;

		try {
			if (res == null) {
				// Support (simple) skipping:
				long startFrom = 0;
				long endAt = -1;
				String range = header.getProperty("range");
				if (range != null) {
					if (range.startsWith("bytes=")) {
						range = range.substring("bytes=".length());
						int minus = range.indexOf('-');
						try {
							if (minus > 0) {
								startFrom = Long.parseLong(range.substring(0,
										minus));
								endAt = Long.parseLong(range
										.substring(minus + 1));
							}
						} catch (NumberFormatException nfe) {
						}
					}
				}
				// Change return code and add Content-Range header when skipping
				// is requested
				Properties systemProperties = System.getProperties();
				File file = new File(
						systemProperties
								.getProperty(EmbeddedServer.CURRENT_FILE));
				long fileLen = file.length();
				if (range != null && startFrom >= 0) {
					if (startFrom >= fileLen) {
						res = new Response(HTTP_RANGE_NOT_SATISFIABLE,
								MIME_PLAINTEXT, "");
						res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
					} else {
						if (endAt < 0)
							endAt = fileLen - 1;
						long newLen = endAt - startFrom + 1;
						if (newLen < 0)
							newLen = 0;

						final long dataLen = newLen;
						InputStream reqIs = new BufferedInputStream(
								new FileInputStream(file), BUFFER_SIZE);
						reqIs.skip(startFrom);

						res = new Response(HTTP_PARTIALCONTENT, mime, reqIs);
						res.addHeader("Content-Length", "" + dataLen);
						res.addHeader("Content-Range", "bytes " + startFrom
								+ "-" + endAt + "/" + fileLen);
					}
				} else {
					InputStream reqIs = new BufferedInputStream(
							new FileInputStream(file), BUFFER_SIZE);
					res = new Response(HTTP_OK, mime, reqIs);
					res.addHeader("Content-Length", "" + fileLen);
				}
			}
		} catch (Exception e) {
			res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
					"FORBIDDEN: Reading file failed.");
		}

		res.addHeader("Accept-Ranges", "bytes"); // Announce that the file
													// server accepts partial
													// content requestes
		return res;
	}
}
