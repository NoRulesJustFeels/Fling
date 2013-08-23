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
import java.net.URL;

import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.UIManager;

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
	private static FlingFrame flingFrame;

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// http://www.capricasoftware.co.uk/projects/vlcj/index.html
		try {
			System.out.println(System.getProperty("os.name"));
			if (System.getProperty("os.name").startsWith("Mac")) {
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "/Applications/VLC.app/Contents/MacOS/lib");
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
			} else if (System.getProperty("os.name").startsWith("Windows 8") || System.getProperty("os.name").startsWith("Windows 7")) {
				System.out.println("Found Windows 7/8");
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Program Files\\VideoLAN\\VLC");
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
			} else if (System.getProperty("os.name").startsWith("Windows")) {
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Program Files (x86)\\VideoLAN\\VLC﻿");
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
			} else {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
			}
		} catch (Throwable ex) {
			// Try for other OS's
			try {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
			} catch (Throwable ex2) {
				System.out.println("VLC not available");
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
		} catch(Exception ex) {
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
		flingFrame.setSize(420, 250);
		// /flingFrame.setSize(420, 300); // with scrubber
		flingFrame.setLocationRelativeTo(null);
		flingFrame.setVisible(true);
	}

}
