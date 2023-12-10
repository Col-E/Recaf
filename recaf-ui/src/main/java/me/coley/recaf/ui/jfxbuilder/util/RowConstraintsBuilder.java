package me.coley.recaf.ui.jfxbuilder.util;

import javafx.geometry.VPos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

public class RowConstraintsBuilder {

	private final RowConstraints constraints = new RowConstraints();

	private RowConstraintsBuilder() {}

	public static RowConstraintsBuilder columnConstrains() {
		return new RowConstraintsBuilder();
	}

	public RowConstraintsBuilder fillHeight(boolean value) {
		constraints.setFillHeight(value);
		return this;
	}

	public RowConstraintsBuilder valignment(VPos value) {
		constraints.setValignment(value);
		return this;
	}

	public RowConstraintsBuilder vgrow(Priority value) {
		constraints.setVgrow(value);
		return this;
	}

	public RowConstraintsBuilder v(VPos vAlignment, Priority vGrow) {
		constraints.setValignment(vAlignment);
		constraints.setVgrow(vGrow);
		return this;
	}

	public RowConstraintsBuilder maxHeight(double value) {
		constraints.setMaxHeight(value);
		return this;
	}

	public RowConstraintsBuilder minHeight(double value) {
		constraints.setMinHeight(value);
		return this;
	}

	public RowConstraintsBuilder clampHeight(double min, double max) {
		constraints.setMinHeight(min);
		constraints.setMaxHeight(max);
		return this;
	}

	public RowConstraintsBuilder percentHeight(double value) {
		constraints.setPercentHeight(value);
		return this;
	}

	public RowConstraintsBuilder prefHeight(double value) {
		constraints.setPrefHeight(value);
		return this;
	}

	public RowConstraints build() {
		return constraints;
	}
}
