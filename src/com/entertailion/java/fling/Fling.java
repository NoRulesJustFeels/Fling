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

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;

import javax.swing.UIManager;
import javax.swing.WindowConstants;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * Main class for Fling app. Fling media to ChromeCast devices using RAMP
 * protocol
 * 
 * @see https://github.com/entertailion/Fling
 * @author leon_nicholls
 * 
 */
public class Fling {
	private static final String LOG_TAG = "Fling";

	public static final String VERSION = "0.5";

	private static FlingFrame flingFrame;

	private static final String VLC_MAC = "/Applications/VLC.app/Contents/MacOS/lib";
	private static final String VLC_WINDOWS1 = "C:\\Program Files\\VideoLAN\\VLC";
	private static final String VLC_WINDOWS2 = "C:\\Program Files (x86)\\VideoLAN\\VLC﻿";

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Log.d(LOG_TAG, "Fling version " + VERSION);
		// VLC wrapper for Java:
		// http://www.capricasoftware.co.uk/projects/vlcj/index.html
		try {
			Log.d(LOG_TAG, System.getProperty("os.name"));
			if (System.getProperty("os.name").startsWith("Mac")) {
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_MAC);
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} else if (System.getProperty("os.name").startsWith("Windows")) {
				File vlcDirectory1 = new File(VLC_WINDOWS1);
				File vlcDirectory2 = new File(VLC_WINDOWS2);
				if (vlcDirectory1.exists()) {
					Log.d(LOG_TAG, "Found VLC at " + VLC_WINDOWS1);
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_WINDOWS1);
				} else if (vlcDirectory2.exists()) {
					Log.d(LOG_TAG, "Found VLC at " + VLC_WINDOWS2);
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_WINDOWS2);
				}
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} else {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			}
		} catch (Throwable ex) {
			// Try for other OS's
			try {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} catch (Throwable ex2) {
				Log.d(LOG_TAG, "VLC not available");
			}
		}

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Create the main window frame
	 */
	public static void createAndShowGUI() {
		Log.d(LOG_TAG, "set to system default LaF");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			System.out.println("Cannot find system look and feel, setting to metal.");
		}
		Log.d(LOG_TAG, "createAndShowGUI");
		flingFrame = new FlingFrame();
		// change the default app icon; might not work for all platforms
		URL url = ClassLoader.getSystemResource("com/entertailion/java/fling/resources/logo.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		flingFrame.setIconImage(img);
		flingFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		// flingFrame.setSize(420, 250);
		flingFrame.setSize(420, 275); // with scrubber
		flingFrame.setLocationRelativeTo(null);
		flingFrame.setVisible(true);
	}

}
