package me.coley.recaf;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import me.coley.recaf.util.Misc;

public class Options {
	/**
	 * File to store config in.
	 */
	private final static File optionsFile = new File("rcoptions.json");
	/**
	 * Show confirmation prompt on doing potentially dangerous things.
	 */
	public boolean confirmDeletions = true;
	/**
	 * Show extra jump information.
	 */
	public boolean opcodeShowJumpHelp = true;
	/**
	 * Simplify descriptor displays on the opcode list.
	 */
	public boolean opcodeSimplifyDescriptors = true;
	/**
	 * Display variable's signature in the opcode edit window for variable
	 * opcodes. Allows editing of signatures <i>(Generic types)</i> and
	 * significantly increases the edit window size.
	 */
	public boolean showVariableSignatureInTable;
	/**
	 * Flags for reading in classes.
	 */
	public int classFlagsInput = ClassReader.EXPAND_FRAMES;
	/**
	 * Flags for writing classes.
	 */
	public int classFlagsOutput = ClassWriter.COMPUTE_FRAMES;
	/**
	 * Max length for text in ldc opcodes to be displayed.
	 */
	public int ldcMaxLength = 125;

	public Options() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				save();
			}
		});
	}

	/**
	 * Load from configuration.
	 */
	public void load() {
		if (!optionsFile.exists()) {
			return;
		}
		try {
			JsonObject json = Json.parse(Misc.readFile(optionsFile.getAbsolutePath())).asObject();
			for (Field field : Options.class.getDeclaredFields()) {
				String name = field.getName();
				JsonValue value = json.get(name);
				if (value == null) {
					continue;
				}
				field.setAccessible(true);
				if (value.isBoolean()) {
					field.set(this, value.asBoolean());
				} else if (value.isNumber()) {
					field.set(this, value.asInt());
				} else if (value.isString()) {
					field.set(this, value.asString());
				}
			}
		} catch (Exception e) {
			// TODO: Propper logging
			e.printStackTrace();
		}
	}

	/**
	 * Save current settings to configuration.
	 */
	public void save() {
		try {
			if (!optionsFile.exists()) {
				optionsFile.createNewFile();
			}
			JsonObject json = Json.object();
			for (Field field : Options.class.getDeclaredFields()) {
				field.setAccessible(true);
				String name = field.getName();
				Object value = field.get(this);
				if (value instanceof Boolean) {
					json.set(name, (boolean) value);
				} else if (value instanceof Integer) {
					json.set(name, (int) value);
				} else if (value instanceof String) {
					json.set(name, (String) value);
				}
			}
			StringWriter w = new StringWriter();
			json.writeTo(w, WriterConfig.PRETTY_PRINT);
			Misc.writeFile(optionsFile.getAbsolutePath(), w.toString());
		} catch (Exception e) {
			// TODO: Propper logging
			e.getMessage();
		}
	}

}
