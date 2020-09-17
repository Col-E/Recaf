package me.coley.recaf.mapping;

import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static me.coley.recaf.util.Log.*;

/**
 * Enigma mappings file implementation.
 *
 * @author Matt
 */
public class EnigmaMappings extends FileMappings {
	private static final String FAIL = "Invalid Enigma mappings, ";

	/**
	 * Constructs mappings from a given file.
	 *
	 * @param path
	 * 		A path to a file containing enigma styled mappings.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	EnigmaMappings(Path path, Workspace workspace) throws IOException {
		super(path, workspace);
	}

	@Override
	protected Map<String, String> parse(String text, Workspace workspace) {
		Map<String, String> map = new HashMap<>();
		String[] lines = StringUtil.splitNewline(text);
		int line = 0;
		Stack<String> currentClass = new Stack<>();
		for(String lineStr : lines) {
			line++;
			String lineStrTrim = lineStr.trim();
			int strIndent = lineStr.indexOf(lineStrTrim) + 1;
			String[] args = lineStrTrim.split(" ");
			String type = args[0];
			try {
				switch(type) {
					case "CLASS":
						if (lineStr.matches("\\s+.+")) {
							// Check for indentation, implies the class is an inner
							currentClass.add(args[1]);
						} else {
							// Root level class
							currentClass.clear();
							currentClass.add(args[1]);
						}
						// Not all classes need to be renamed if they have child elements that are renamed
						if (args.length >= 3) {
							String renamedClass = args[2];
							map.put(currentClass.peek(), renamedClass);
							// Map inners as well
							String prefix = currentClass.peek() + "$";
							workspace.getPrimaryClassNames().stream()
									.filter(n -> n.startsWith(prefix))
									.forEach(n -> map.put(n, renamedClass + n.substring(currentClass.peek().length())));
						}
						break;
					case "FIELD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size()) {
							currentClass.pop();
						}
						// Parse field
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");
						String currentField = args[1];
						String renamedField = args[2];
						String fieldType = args[3];
						map.put(currentClass.peek() + "." + currentField, renamedField);
						// Field references may not be based on the direct class they are declared in.
						// A child class may refer to a parent class member, using the child class as an owner.
						// However, once a child class introduces a shadowing field name, we want to stop introducing
						// children as owners for this mapping run.
						workspace.getHierarchyGraph()
						.getAllDescendantsWithBreakCondition(currentClass.peek(),
								n -> ClassUtil.containsField(workspace.getClassReader(n), currentField, fieldType))
						.forEach(childOwner -> map.put(childOwner + "." + currentField, renamedField));
						break;
					case "METHOD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size()) {
							currentClass.pop();
						}
						// Parse method
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");
						String currentMethod = args[1];
						if (currentMethod.equals("<init>"))
							continue;
						// Not all methods need to be renamed if they have child arg elements that are renamed
						if (args.length >= 4) {
							String renamedMethod = args[2];
							String methodType = args[3];
							// Method references should be renamed for the entier hierarchy
							workspace.getHierarchyGraph().getHierarchyNames(currentClass.peek())
									.forEach(hierarchyMember -> map.put(hierarchyMember + "." + currentMethod + methodType, renamedMethod));
						}
						break;
					case "ARG":
						// Do nothing, mapper does not support arg names
						break;
					default:
						trace("Unknown Engima mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch(IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
			}
		}
		return map;
	}
}
