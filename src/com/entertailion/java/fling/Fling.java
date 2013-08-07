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

import javax.swing.WindowConstants;

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
		Log.d(LOG_TAG, "createAndShowGUI");
		flingFrame = new FlingFrame();
		// change the default app icon; might not work for all platforms
		URL url = ClassLoader
				.getSystemResource("com/entertailion/java/fling/resources/logo.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		flingFrame.setIconImage(img);
		flingFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		flingFrame.setSize(420, 250);
		flingFrame.setLocationRelativeTo(null);
		flingFrame.setVisible(true);
	}

}
