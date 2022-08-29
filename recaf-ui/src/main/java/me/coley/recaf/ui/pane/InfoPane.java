package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import me.coley.recaf.BuildConfig;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.control.SubLabeled;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.DesktopUtil;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import javax.tools.ToolProvider;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel that shows system information.
 *
 * @author Matt Coley
 */
public class InfoPane extends GridPane {
	private static final Logger logger = Logging.get(InfoPane.class);
	private static final int SEP_SIZE = 2;

	/**
	 * New info pane.
	 */
	public InfoPane() {
		Path baseDir = Directories.getBaseDirectory();
		// Grid config
		setVgap(4);
		setHgap(5);
		setPadding(new Insets(15));
		setAlignment(Pos.CENTER);
		int r = 0;
		// System
		addRow(r++, new SubLabeled(Lang.getBinding("help.system"),
				Lang.getBinding("help.system.sub")));
		addRow(r++, new Label("Name"), new Label(System.getProperty("os.name")));
		addRow(r++, new Label("Architecture"), new Label(System.getProperty("os.arch")));
		addRow(r++, new Label("Processors"), new Label(String.valueOf(Runtime.getRuntime().availableProcessors())));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Java
		addRow(r++, new SubLabeled(Lang.getBinding("help.java"), Lang.getBinding("help.java.sub")));
		addRow(r++, new Label("Version"), new Label(System.getProperty("java.version")));
		addRow(r++, new Label("VM name"), new Label(System.getProperty("java.vm.name")));
		addRow(r++, new Label("VM vendor"), new Label(System.getProperty("java.vm.vendor")));
		addRow(r++, new Label("Home"), new Label(System.getProperty("java.home")));
		addRow(r++, new Label("Supports compiler"), new Label(Boolean.toString(
				ToolProvider.getSystemJavaCompiler() != null
		)));
		addRow(r++, new Label("Supports attach"), new Label(Boolean.toString(
				ClasspathUtil.classExists("com.sun.tools.attach.VirtualMachine")
		)));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// JavaFX
		addRow(r++, new SubLabeled(Lang.getBinding("help.javafx"),
				Lang.getBinding("help.javafx.sub")));
		addRow(r++, new Label("Version"), new Label(System.getProperty("javafx.version")));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Recaf
		addRow(r++, new SubLabeled(Lang.getBinding("help.recaf"), Lang.getBinding("help.recaf.sub")));
		addRow(r++, new Label("Version"), new Label(BuildConfig.VERSION));
		addRow(r++, new Label("Build"), new Label(BuildConfig.GIT_SHA.substring(0, 7) + " " + BuildConfig.GIT_DATE));
		addRow(r++, new Label("Settings directory"), new Label(baseDir.toAbsolutePath().toString()));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Copy
		addRow(r, new ActionButton(Lang.getBinding("help.copy"), () -> {
			Clipboard clip = Clipboard.getSystemClipboard();
			ClipboardContent content = new ClipboardContent();
			content.putString(buildClipboard());
			clip.setContent(content);
		}), new ActionButton(Lang.getBinding("help.opendir"), () -> {
			try {
				DesktopUtil.showDocument(baseDir.toUri());
			} catch (Exception ex) {
				logger.error("Failed to open Recaf directory", ex);
			}
		}));
	}

	@Override
	public void addRow(int rowIndex, Node... children) {
		super.addRow(rowIndex, children);
		if (children[0].getClass() == Label.class) {
			children[0].getStyleClass().add("b");
			children[1].getStyleClass().add("monospace");
		}
	}

	/**
	 * Converts the UI to markdown text.
	 * Section headers are declared with {@link SubLabeled}.
	 * Section Key/Value pairs are declared with two consecutive {@link Label}.
	 *
	 * @return Markdown string containing all UI elements.
	 */
	public String buildClipboard() {
		// Data collection
		Map<String, Map<String, String>> data = new LinkedHashMap<>();
		// Current section title/map
		String currentSection = null;
		Map<String, String> currentMap = null;
		// Current section key
		boolean labelIsKey = true;
		String currentKey = null;
		for (Node node : getChildren()) {
			// header
			if (node.getClass() == SubLabeled.class) {
				if (currentMap != null) {
					data.put(currentSection, currentMap);
				}
				SubLabeled header = (SubLabeled) node;
				currentSection = header.getPrimaryText();
				currentMap = new LinkedHashMap<>();
			}
			// items (key:value), one follows the other
			else if (node.getClass() == Label.class) {
				String text = ((Label) node).getText();
				if (labelIsKey) {
					currentKey = text;
				} else {
					currentMap.put(currentKey, text);
				}
				// Swap since we do KEY -> VALUE, KEY -> VALUE
				labelIsKey = !labelIsKey;
			}
		}
		// Put to string
		StringBuilder sb = new StringBuilder();
		data.forEach((section, map) -> {
			sb.append("**").append(section).append("**\n")
					.append("| ").append(String.join(" | ", map.keySet())).append(" |\n")
					.append("| ").append(map.keySet().stream()
							.map(s -> "--------").collect(Collectors.joining(" | "))).append(" |\n")
					.append("| `").append(String.join("` | `", map.values())).append("` |\n\n");
		});
		return sb.toString();
	}
}
