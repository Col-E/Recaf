package me.coley.recaf.ui.control.parameterinput.util;

import javafx.geometry.HPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

public class ColumnConstraintsBuilder {

	private final ColumnConstraints constraints = new ColumnConstraints();

	private ColumnConstraintsBuilder() {}

	public static ColumnConstraintsBuilder columnConstrains() {
		return new ColumnConstraintsBuilder();
	}

	public ColumnConstraintsBuilder fillWidth(boolean value) {
		constraints.setFillWidth(value);
		return this;
	}

	public ColumnConstraintsBuilder halignment(HPos value) {
		constraints.setHalignment(value);
		return this;
	}

	public ColumnConstraintsBuilder hgrow(Priority value) {
		constraints.setHgrow(value);
		return this;
	}

	public ColumnConstraintsBuilder h(HPos hAlignment, Priority hGrow) {
		constraints.setHalignment(hAlignment);
		constraints.setHgrow(hGrow);
		return this;
	}

	public ColumnConstraintsBuilder maxWidth(double value) {
		constraints.setMaxWidth(value);
		return this;
	}

	public ColumnConstraintsBuilder minWidth(double value) {
		constraints.setMinWidth(value);
		return this;
	}

	public ColumnConstraintsBuilder clampWidth(double min, double max) {
		constraints.setMinWidth(min);
		constraints.setMaxWidth(max);
		return this;
	}

	public ColumnConstraintsBuilder percentWidth(double value) {
		constraints.setPercentWidth(value);
		return this;
	}

	public ColumnConstraintsBuilder prefWidth(double value) {
		constraints.setPrefWidth(value);
		return this;
	}

	public ColumnConstraints build() {
		return constraints;
	}
}
