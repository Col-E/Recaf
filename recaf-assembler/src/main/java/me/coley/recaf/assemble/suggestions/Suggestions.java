package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.util.ProgramClass;
import me.coley.recaf.util.ClasspathUtil;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.InstructionGroup;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static me.coley.recaf.util.ClasspathUtil.Tree;


public class Suggestions {

	private final Function<String, ProgramClass> mapper;
	private final Tree systemClasses = ClasspathUtil.getSystemClasses();
	private final Tree classes;
	public final static TreeSet<String> instructions = new TreeSet<>(ParseInfo.actions.keySet());

	/**
	 * @param classes
	 * 		a tree of current user classes
	 * @param mapper
	 * 		a function that maps a class name to a {@link ProgramClass} instance
	 */
	public Suggestions(Tree classes, Function<String, ProgramClass> mapper) {
		this.classes = classes;
		this.mapper = mapper;
	}

	/**
	 * Supply suggestions for a given group and partial parameters.
	 *
	 * @param group
	 * 		Group to suggest for.
	 *
	 * @return Suggestions for the given group.
	 */
	public SuggestionResult getSuggestion(Group group) {
		switch (group.type) {
			case INSTRUCTION:
				return getInstructionSuggestion(group);
		}
		// return empty suggestion result
		return new SuggestionResult("", Stream.empty());
	}

	private SuggestionResult getInstructionSuggestion(Group group) {
		InstructionGroup instruction = (InstructionGroup) group;
		Group[] children = instruction.children;
		switch (instruction.content()) {
			case "new":
				return startsWith(children[0] == null ? "" : children[0].content());
		}
		return new SuggestionResult("", Stream.empty());
	}

	private SuggestionResult startsWith(String partial) {
		return new SuggestionResult(
				partial,
				Stream.concat(
						searchIn(classes, partial),
						searchIn(systemClasses, partial)
				)
		);
	}

	private Stream<String> searchIn(Tree root, String partial) {
		return root.getAllLeaves()
				.map(Tree::getFullValue)
				.filter(x -> x.startsWith(partial));
	}
}
