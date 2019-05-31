package me.coley.recaf.ui.component.editor;

import java.util.HashMap;
import java.util.Map;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;

/**
 * Editor for editing class compiled version.
 * 
 * @author Matt
 */
public class VersionEditor extends StagedCustomEditor<Integer> {
	public VersionEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		ComboBox<JavaVersion> combo = new ComboBox<>(FXCollections.observableArrayList(JavaVersion.values()));
		combo.valueProperty().addListener((ov, prev, current) -> setValue(Integer.valueOf(current.version)));
		combo.setValue(JavaVersion.get(getValue()));
		return combo;
	}

	/**
	 * Enumeration of supported java versions.
	 * 
	 * @author Matt
	 */
	private enum JavaVersion {
		// @formatter:off
			JAVA_1_1(Opcodes.V1_1,  "Java 1.1"),
			JAVA_1_2(Opcodes.V1_2,  "Java 1.2"),
			JAVA_1_3(Opcodes.V1_3,  "Java 1.3"),
			JAVA_1_4(Opcodes.V1_4,  "Java 1.4"),
			JAVA_1_5(Opcodes.V1_5,  "Java 1.5"),
			JAVA_1_6(Opcodes.V1_6,  "Java 1.6"),
			JAVA_1_7(Opcodes.V1_7,  "Java 1.7"),
			JAVA_1_8(Opcodes.V1_8,  "Java 1.8"),
			JAVA_9  (Opcodes.V9,    "Java 9"),
			JAVA_10 (Opcodes.V10,   "Java 10"),
			JAVA_11 (Opcodes.V11,   "Java 11"),
			JAVA_12 (Opcodes.V12,   "Java 12"),
			JAVA_13 (Opcodes.V13,   "Java 13");
		// @formatter:on

		private final int version;
		private final String name;

		JavaVersion(int version, String name) {
			this.version = version;
			this.name = name;
		}

		public String getVersionNumber() {
			int minor = (version >> 16) & 0xFFFF;
			int major = version & 0xFFFF;
			return major + "." + minor;
		}

		@Override
		public String toString() {
			return name + " (" + getVersionNumber() + ")";
		}

		/**
		 * Map of java version values to the enum representation.
		 */
		private static final Map<Integer, JavaVersion> lookup = new HashMap<>();

		/**
		 * Lookup in {@link #lookup version-to-enum} map.
		 */
		public static JavaVersion get(int version) {
			return lookup.get(version);
		}

		static {
			// populate lookup map
			for (JavaVersion version : values()) {
				lookup.put(version.version, version);
			}
		}
	}
}