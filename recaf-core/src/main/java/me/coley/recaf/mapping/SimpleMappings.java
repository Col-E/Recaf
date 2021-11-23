package me.coley.recaf.mapping;

import me.coley.recaf.util.EscapeUtil;

import java.util.Map;

import static me.coley.recaf.util.EscapeUtil.unescape;

/**
 * Simple mappings file implementation where the old/new names are split by a space.
 * The input format of the mappings is based on the format outlined by
 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
 * <br>
 * Differences include:
 * <ul>
 *     <li>Support for {@code #comment} lines</li>
 *     <li>Support for unicode escape sequences ({@code \\uXXXX})</li>
 *     <li>Support for fields specified by their name <i>and descriptor</i></li>
 * </ul>
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
public class SimpleMappings extends MappingsAdapter {
	/**
	 * New simple instance.
	 */
	public SimpleMappings() {
		super("Simple", true, true);
	}

	@Override
	public void parse(String mappingText) {
		String[] lines = mappingText.split("[\n\r]");
		// # Comment
		// BaseClass TargetClass
		// BaseClass.baseField targetField
		// BaseClass.baseField baseDesc targetField
		// BaseClass.baseMethod(BaseMethodDesc) targetMethod
		for (String line : lines) {
			// Skip comments and empty lines
			if (line.trim().startsWith("#") || line.trim().isEmpty())
				continue;
			String[] args = line.split(" ");
			String oldBaseName = EscapeUtil.unescape(args[0]);
			if (args.length >= 3) {
				// Descriptor qualified field format
				String desc = unescape(args[1]);
				String targetName = unescape(args[2]);
				int dot = oldBaseName.lastIndexOf('.');
				String oldClassName = oldBaseName.substring(0, dot);
				String oldFieldName = oldClassName.substring(dot + 1);
				addField(oldClassName, oldFieldName, desc, targetName);
			} else {
				String newName = EscapeUtil.unescape(args[1]);
				int dot = oldBaseName.lastIndexOf('.');
				if (dot > 0) {
					// Indicates a member
					String oldClassName = oldBaseName.substring(0, dot);
					String oldIdentifier = oldBaseName.substring(dot + 1);
					int methodDescStart = oldIdentifier.lastIndexOf("(");
					if (methodDescStart > 0) {
						// Method descriptor part of ID, split it up
						String methodName = oldIdentifier.substring(0, methodDescStart);
						String methodDesc = oldIdentifier.substring(methodDescStart);
						addMethod(oldClassName, methodName, methodDesc, newName);
					} else {
						// Likely a field without linked descriptor
						addField(oldClassName, oldIdentifier, newName);
					}
				} else {
					addClass(oldBaseName, newName);
				}

			}
		}
	}
}
