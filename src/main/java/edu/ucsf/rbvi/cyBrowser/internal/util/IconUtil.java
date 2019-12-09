package edu.ucsf.rbvi.cyBrowser.internal.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import javax.swing.UIManager;

public abstract class IconUtil {
	
	public static final String BROWSER_LAYER_1 = "a";
	public static final String BROWSER_LAYER_2 = "b";
	
	public static final String[] BROWSER_ICON = new String[] { BROWSER_LAYER_1, BROWSER_LAYER_2 };
	public static final Color[] BROWSER_ICON_COLORS = new Color[] { Color.WHITE, UIManager.getColor("CyColor.complement(+1)") };
	
	private static Font iconFont;

	static {
		try {
			iconFont = Font.createFont(Font.TRUETYPE_FONT, IconUtil.class.getResourceAsStream("/fonts/cyBrowser.ttf"));
		} catch (FontFormatException e) {
			throw new RuntimeException();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static Font getIconFont(float size) {
		return iconFont.deriveFont(size);
	}

	private IconUtil() {
		// ...
	}
}

