package me.coley.recaf.ui.control.menu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/**
 * MenuItem with an on-click and on-close runnable action.
 *
 * @author Wolfie / win32kbase
 */
public class ClosableActionMenuItem extends CustomMenuItem {
    public ClosableActionMenuItem(String text, Node graphic, Runnable action, Runnable onClose) {
        HBox item = new HBox();
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        item.setAlignment(Pos.CENTER);
        item.setSpacing(6);

        Label label = new Label(text);
        Button closeButton = new Button("x");
        closeButton.setOnAction(e -> {
            // This stops the input from 'bleeding' through to behind the button
            e.consume();
            onClose.run();
        });

        item.getChildren().addAll(closeButton, graphic, label);
        this.setContent(item);
        this.setOnAction(e -> action.run());
    }
}
