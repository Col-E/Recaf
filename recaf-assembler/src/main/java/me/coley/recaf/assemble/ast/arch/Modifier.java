package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a {@link java.lang.reflect.Modifier} of a {@link Definition}.
 *
 * @author Matt Coley
 */
public class Modifier extends BaseElement implements Named {
	private static final Map<String, Modifier> nameMap = new HashMap<>();
	private final String name;
	private final int value;

	/**
	 * @param name
	 * 		Name of modifier.
	 * @param value
	 * 		Actual value of modifier.
	 */
	private Modifier(String name, int value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String print(PrintContext context) {
		return context.fmtKeyword(name);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return Actual value of modifier.
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @param text
	 * 		Modifier name.
	 *
	 * @return Instance by name, or {@code null} if no modifier by the name exists.
	 */
	public static Modifier byName(String text) {
		text = text.toLowerCase();
		return nameMap.get(text);
	}

	static {
		nameMap.put("public", new Modifier("PUBLIC", Opcodes.ACC_PUBLIC));
		nameMap.put("private", new Modifier("PRIVATE", Opcodes.ACC_PRIVATE));
		nameMap.put("protected", new Modifier("PROTECTED", Opcodes.ACC_PROTECTED));
		nameMap.put("static", new Modifier("STATIC", Opcodes.ACC_STATIC));
		nameMap.put("final", new Modifier("FINAL", Opcodes.ACC_FINAL));
		nameMap.put("synchronized", new Modifier("SYNCHRONIZED", Opcodes.ACC_SYNCHRONIZED));
		nameMap.put("super", new Modifier("SUPER", Opcodes.ACC_SUPER));
		nameMap.put("bridge", new Modifier("BRIDGE", Opcodes.ACC_BRIDGE));
		nameMap.put("volatile", new Modifier("VOLATILE", Opcodes.ACC_VOLATILE));
		nameMap.put("varargs", new Modifier("VARARGS", Opcodes.ACC_VARARGS));
		nameMap.put("transient", new Modifier("TRANSIENT", Opcodes.ACC_TRANSIENT));
		nameMap.put("native", new Modifier("NATIVE", Opcodes.ACC_NATIVE));
		nameMap.put("interface", new Modifier("INTERFACE", Opcodes.ACC_INTERFACE));
		nameMap.put("abstract", new Modifier("ABSTRACT", Opcodes.ACC_ABSTRACT));
		nameMap.put("strictfp", new Modifier("STRICTFP", Opcodes.ACC_STRICT));
		nameMap.put("synthetic", new Modifier("SYNTHETIC", Opcodes.ACC_SYNTHETIC));
		nameMap.put("annotation-interface", new Modifier("ANNOTATION-INTERFACE", Opcodes.ACC_ANNOTATION));
		nameMap.put("enum", new Modifier("ENUM", Opcodes.ACC_ENUM));
		nameMap.put("module", new Modifier("MODULE", Opcodes.ACC_MODULE));
		nameMap.put("open", new Modifier("OPEN", Opcodes.ACC_OPEN));
		nameMap.put("transitive", new Modifier("TRANSITIVE", Opcodes.ACC_TRANSITIVE));
		nameMap.put("static-phase", new Modifier("STATIC-PHASE", Opcodes.ACC_STATIC_PHASE));
		nameMap.put("mandated", new Modifier("MANDATED", Opcodes.ACC_MANDATED));
	}
}
