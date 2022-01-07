package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import me.coley.recaf.Recaf;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.SubLabeled;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;

import javax.tools.ToolProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Panel that shows system information.
 *
 * @author Matt
 */
public class SysInfoPane extends GridPane {
	private static final int SEP_SIZE = 2;

	/**
	 * Create info pane.
	 */
	public SysInfoPane() {
		// Grid config
		setVgap(4);
		setHgap(5);
		setPadding(new Insets(15));
		setAlignment(Pos.CENTER);
		int r = 0;
		// System
		addRow(r++, new SubLabeled(translate("ui.about.system"),
				translate("ui.about.system.sub")));
		addRow(r++, new Label("Name"), new Label(System.getProperty("os.name")));
		addRow(r++, new Label("Architecture"), new Label(System.getProperty("os.arch")));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Java
		addRow(r++, new SubLabeled(translate("ui.about.java"), translate("ui.about.java.sub")));
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
		addRow(r++, new SubLabeled(translate("ui.about.javafx"),
				translate("ui.about.javafx.sub")));
		addRow(r++, new Label("Version"), new Label(System.getProperty("javafx.version")));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Recaf
		addRow(r++, new SubLabeled(translate("ui.about.recaf"), translate("ui.about.recaf.sub")));
		addRow(r++, new Label("Version"), new Label(Recaf.VERSION));
		addRow(r++, new Label("Settings directory"), new Label(Recaf.getDirectory().toFile().getAbsolutePath()));
		add(new Separator(), 0, (SEP_SIZE - 1) + r, 2, SEP_SIZE);
		r += (SEP_SIZE + 1);
		// Copy
		addRow(r, new ActionButton(translate("ui.about.copy"), () -> {
			Clipboard clip = Clipboard.getSystemClipboard();
			ClipboardContent content = new ClipboardContent();
			content.putString(buildClipboard());
			clip.setContent(content);
		}), new ActionButton(translate("ui.about.opendir"), () -> {
			try {
				UiUtil.showDocument(Recaf.getDirectory().toUri());
			} catch(Exception ex) {
				Log.error(ex, "Failed to open Recaf directory");
			}
		}));
	}

	@Override
	public void addRow(int rowIndex, Node... children) {
		super.addRow(rowIndex, children);
		if(children[0].getClass() == Label.class) {
			children[0].getStyleClass().add("bold");
			children[1].getStyleClass().add("monospaced");
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
		for(Node node : getChildren()) {
			// header
			if(node.getClass() == SubLabeled.class) {
				if(currentMap != null) {
					data.put(currentSection, currentMap);
				}
				SubLabeled header = (SubLabeled) node;
				currentSection = header.getPrimaryText();
				currentMap = new LinkedHashMap<>();
			}
			// items (key:value), one follows the other
			else if(node.getClass() == Label.class) {
				String text = ((Label) node).getText();
				if(labelIsKey) {
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
