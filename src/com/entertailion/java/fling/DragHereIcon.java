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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/*
 * UI for drag-and-drop
 * 
 * http://stackoverflow.com/questions/10751001/java-application-with-fancy-drag-drop
 */
public class DragHereIcon implements Icon {
	private static final String LOG_TAG = "DragHereIcon";
	private int size = 80;
	private float a = 4f;
	private float b = 8f;
	private int r = 16;
	private int f = size / 4;
	private Font font = new Font("Monospace", Font.PLAIN, size / 2);
	private FontRenderContext frc = new FontRenderContext(null, true, true);
	private Shape s = new TextLayout("\u21E9", font, frc).getOutline(null);
	private Color linec = Color.GRAY;

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.translate(x, y);

		g2.setStroke(new BasicStroke(a));
		g2.setPaint(linec);
		g2.draw(new RoundRectangle2D.Float(a, a, size - 2 * a - 1, size - 2 * a - 1, r, r));

		// draw bounding box
		g2.setStroke(new BasicStroke(b));
		g2.setColor(UIManager.getColor("Panel.background"));
		g2.drawLine(1 * f, 0 * f, 1 * f, 4 * f);
		g2.drawLine(2 * f, 0 * f, 2 * f, 4 * f);
		g2.drawLine(3 * f, 0 * f, 3 * f, 4 * f);
		g2.drawLine(0 * f, 1 * f, 4 * f, 1 * f);
		g2.drawLine(0 * f, 2 * f, 4 * f, 2 * f);
		g2.drawLine(0 * f, 3 * f, 4 * f, 3 * f);

		// draw arrow
		g2.setPaint(linec);
		Rectangle2D b = s.getBounds();
		Point2D.Double p = new Point2D.Double(b.getX() + b.getWidth() / 2d, b.getY() + b.getHeight() / 2d);
		AffineTransform toCenterAT = AffineTransform.getTranslateInstance(size / 2d - p.getX(), size / 2d - p.getY());
		g2.fill(toCenterAT.createTransformedShape(s));
		g2.translate(-x, -y);
		g2.dispose();
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}

	/**
	 * Display a custom icon with file drag-and-drop support
	 * 
	 * @param flingFrame
	 *            the parent frame
	 * @return
	 */
	public static JComponent makeUI(final FlingFrame flingFrame) {
		JLabel label = new JLabel(new DragHereIcon());
		label.setText("<html>Drag <b>Media</b> Here");
		label.setVerticalTextPosition(SwingConstants.BOTTOM);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setForeground(Color.GRAY);
		label.setFont(new Font("Monospace", Font.PLAIN, 24));
		JPanel p = new JPanel();
		p.add(label);
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		new FileDrop(p, new FileDrop.Listener() {
			public void filesDropped(java.io.File[] files) {
				if (files != null && files.length > 0) {
					try {
						String file = files[0].getCanonicalPath();
						Log.d(LOG_TAG, file);
						Properties systemProperties = System.getProperties();
						systemProperties.setProperty(EmbeddedServer.CURRENT_FILE, file); // EmbeddedServer.serveFile

						flingFrame.sendMediaUrl(file);
					} catch (IOException e) {
					}
				}
			} // end filesDropped
		}); // end FileDrop.Listener
		return p;
	}

}