package me.coley.recaf.ui.controls;

import javafx.scene.control.Label;
import me.coley.recaf.config.FieldWrapper;

import java.time.Instant;

/**
 * Label for displaying times.
 *
 * @author Matt
 */
public class TimestampLabel extends Label {
	/**
	 * @param field
	 * 		Time field, assumed to be a long.
	 */
	public TimestampLabel(FieldWrapper field) {
		super(getTimeString(field.get()));
	}

	private static <T extends Number> String getTimeString(T time) {
		return Instant.ofEpochMilli(time.longValue()).toString();
	}
}
