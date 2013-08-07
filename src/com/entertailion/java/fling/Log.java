/*
 * Copyright (C) 2012 ENTERTAILION, LLC. All rights reserved.
 * Copyright (C) 2012 ENTERTAILION, LLC. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Map Android style logging to Java style logging
 * 
 * @author leon_nicholls
 */
public class Log {
	private static Logger Log = Logger.getLogger("anymote");

	public static void e(String tag, String message, Throwable e) {
		Log.log(Level.SEVERE, tag + ": " + message, e);
	}

	public static void e(String tag, String message) {
		Log.log(Level.SEVERE, tag + ": " + message);
	}

	public static void i(String tag, String message, Throwable e) {
		Log.log(Level.INFO, tag + ": " + message, e);
	}

	public static void i(String tag, String message) {
		Log.log(Level.INFO, tag + ": " + message);
	}

	public static void d(String tag, String message, Throwable e) {
		Log.log(Level.CONFIG, tag + ": " + message, e);
	}

	public static void d(String tag, String message) {
		Log.log(Level.CONFIG, tag + ": " + message);
		System.out.println(tag + ": " + message);  // TODO
	}

	public static void v(String tag, String message, Throwable e) {
		Log.log(Level.FINEST, tag + ": " + message, e);
	}

	public static void v(String tag, String message) {
		Log.log(Level.FINEST, tag + ": " + message);
	}

	public static void w(String tag, String message, Throwable e) {
		Log.log(Level.WARNING, tag + ": " + message, e);
	}

	public static void w(String tag, String message) {
		Log.log(Level.WARNING, tag + ": " + message);
	}
}
