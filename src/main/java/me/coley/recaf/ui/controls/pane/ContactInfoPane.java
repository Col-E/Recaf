package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;

import java.io.IOException;
import java.net.URI;

/**
 * Panel that shows contact information.
 *
 * @author Matt
 */
public class ContactInfoPane extends GridPane {
	private static final int SEP_SIZE = 2;

	/**
	 * Create contact pane.
	 */
	public ContactInfoPane() {
		// Grid config
		setVgap(4);
		setHgap(5);
		setPadding(new Insets(15));
		setAlignment(Pos.CENTER);
		// System
		addRow(0, new Label("GitHub"), link("https://github.com/Col-E/Recaf", "icons/github.png"));
		addRow(1, new Label("Discord"), new Hyperlink("https://discord.gg/Bya5HaA", new IconView("icons/discord.png")));
	}

	private Node link(String url, String iconPath) {
		Hyperlink link = new Hyperlink(url, new IconView(iconPath));
		link.setOnAction(e -> {
			try {
				UiUtil.showDocument(URI.create(url));
			} catch(IOException | IllegalArgumentException ex) {
				Log.error(ex, "Failed to open URL");
			}
		});
		return link;
	}

	@Override
	public void addRow(int rowIndex, Node... children) {
		super.addRow(rowIndex, children);
		if(children[0].getClass() == Label.class) {
			children[0].getStyleClass().add("bold");
		}
	}
}
