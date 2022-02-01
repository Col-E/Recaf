package me.coley.recaf.ui.control.config;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Box;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

public class ConfigPos extends GridPane implements Unlabeled  {
    private static final PseudoClass pressedPseudoClass = PseudoClass.getPseudoClass("pressed");

    public ConfigPos(ConfigContainer instance, Field field) {
        this.setPadding(new Insets(6));
        this.setHgap(6);
        this.setVgap(6);

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Button button = new Button();
                // So we can set the button size without it being auto-sized
                button.setStyle("-fx-font-size: 0.1");
                button.setMinSize(16, 16);

                int ordinal = (y * 3) + x;
                button.setOnMouseReleased(e -> {
                    // Disable all buttons
                    this.getChildren().forEach(child -> child.pseudoClassStateChanged(pressedPseudoClass, false));

                    // Enable button
                    button.pseudoClassStateChanged(pressedPseudoClass, true);
                    // Set Pos enum value
                    ReflectUtil.quietSet(instance, field, Pos.values()[ordinal]);
                });

                this.add(button, x, y);
            }
        }

        Enum<?> posEnum = ReflectUtil.quietGet(instance, field);
        Button firstButton = (Button) this.getChildren().get(posEnum.ordinal());
        firstButton.pseudoClassStateChanged(pressedPseudoClass, true);
    }
}
