package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.util.ProgramClass;
import me.coley.recaf.util.ClasspathUtil;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Keyword;
import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.groups.InstructionGroup;
import org.objectweb.asm.tree.InsnList;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.coley.recaf.util.ClasspathUtil.Tree;


public class Suggestions {

	private final Function<String, ProgramClass> mapper;
	private final Tree systemClasses = ClasspathUtil.getSystemClasses();
	private final Tree classes;
	public final static TreeSet<String> instructions = new TreeSet<>(ParseInfo.actions.keySet());

	/**
	 * @param classes a tree of current user classes
	 * @param mapper a function that maps a class name to a {@link ProgramClass} instance
	 */
	public Suggestions(Tree classes, Function<String, ProgramClass> mapper) {
		this.classes = classes;
		this.mapper = mapper;
	}

	/**
	 * Supply suggestions for a given group and partial parameters.
	 * @param group Group to suggest for.
	 * @return Suggestions for the given group.
	 */
	public Set<String> getSuggestion(Group group) {
		switch (group.type) {
			case INSTRUCTION:
				return getInstructionSuggestion(group);
		}
		// return empty set
		return Collections.emptySet();
	}

	private Set<String> getInstructionSuggestion(Group group) {
		Set<String> suggestions = new HashSet<>();
		InstructionGroup instruction = (InstructionGroup) group;
		Group[] children = instruction.children;
		switch (instruction.content()) {
			case "new": return startsWith(children[0] == null ? "" : children[0].content());
		}
		return suggestions;
	}

	/**
	 * Filter a string list to only those that start with the given string.
	 * @param partial String to match.
	 * @return Filtered list.
	 */
	public Set<String> startsWith(String partial) {
		String path = partial.substring(0, partial.lastIndexOf('/'));
		String search = partial.substring(partial.lastIndexOf('/') + 1);
		TreeSet<String> suggestions = new TreeSet<>();
		Tree matchTree = classes.visitPath(path);
		if(matchTree == null)
			return startsWithSystem(partial); // alternatively search through system classes
		List<Tree> matches = classes.visitPath(path).getAllLeaves();
		if(matches.isEmpty())
			return startsWithSystem(partial); // alternatively search through system classes
		for (Tree match : matches) {
			String value = match.getValue();
			if(value.startsWith(search)) suggestions.add(value);
		}
		return suggestions;
	}

	public Set<String> startsWithSystem(String partial) {
		String path = partial.substring(0, partial.lastIndexOf('/'));
		String search = partial.substring(partial.lastIndexOf('/') + 1);
		TreeSet<String> suggestions = new TreeSet<>();
		Tree matchTree = systemClasses.visitPath(path);
		if(matchTree == null)
			return Collections.emptySet();
		List<Tree> matches = systemClasses.visitPath(path).getAllLeaves();
		if(matches.isEmpty())
			return Collections.emptySet();
		for (Tree match : matches) {
			String value = match.getValue();
			if(value.startsWith(search)) suggestions.add(match.getFullValue());
		}
		return suggestions;
	}

}
