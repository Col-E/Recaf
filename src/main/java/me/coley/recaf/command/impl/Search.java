package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.MetaCommand;
import me.coley.recaf.command.completion.WorkspaceNameCompletions;
import me.coley.recaf.search.*;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.coley.recaf.util.Log.*;

/**
 * Unused command, see sub-commands.
 *
 * @author Matt
 */
@CommandLine.Command(name = "search", description = "Base search command.",
		subcommands = {
				Search.ClassName.class,
				Search.ClassInheritance.class,
				Search.Member.class,
				Search.ClassUsage.class,
				Search.MemberUsage.class,
				Search.Text.class,
				Search.Value.class,
				Search.Disass.class
		}
)
public class Search extends MetaCommand implements Callable<Void> {
	@Override
	public Void call() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("Sub-commands for search:");
		for (CommandLine sub : context.getSubcommands().values()) {
			String name =  sub.getCommandName();
			String[] descs = sub.getCommandSpec().usageMessage().description();
			String desc = descs.length > 0 ? descs[0] : "?";
			String args = sub.getCommandSpec().args().stream()
					.map(CommandLine.Model.ArgSpec::paramLabel).collect(Collectors.joining(" "));
			sb.append("\n - ").append(name).append(" ").append(args).append("\n\t").append(desc);
		}
		error(sb.toString());
		return null;
	}

	/**
	 * Command for searching for class declarations.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "class", description = "Find class definitions.")
	public static class ClassName extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The string matching mode.")
		public StringMatchMode mode;
		@CommandLine.Parameters(index = "1",  description = "The name to search for.",
				completionCandidates = WorkspaceNameCompletions.class)
		public String name;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.skipDebug().skipCode()
					.query(new ClassNameQuery(name, mode))
					.build();
		}
	}

	/**
	 * Command for searching for class inheritance.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "classtree", description = "Find classes extending the given name.")
	public static class ClassInheritance extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The class name to search for.",
				completionCandidates = WorkspaceNameCompletions.class)
		public String name;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.skipDebug().skipCode()
					.query(new ClassInheritanceQuery(getWorkspace(), name))
					.build();
		}
	}

	/**
	 * Command for searching for member declarations.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "member", description = "Find member definitions.")
	public static class Member extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The string matching mode.")
		public StringMatchMode mode;
		@CommandLine.Parameters(index = "1",  description = "The class containing the member.",
				completionCandidates = WorkspaceNameCompletions.class)
		public String owner;
		@CommandLine.Parameters(index = "2",  description = "The member name.")
		public String name;
		@CommandLine.Parameters(index = "3",  description = "The member descriptor.")
		public String desc;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.skipDebug().skipCode()
					.query(new MemberDefinitionQuery(owner, name, desc, mode))
					.build();
		}
	}

	/**
	 * Command for searching for member references.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "cref", description = "Find class references.")
	public static class ClassUsage extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The class name.",
				completionCandidates = WorkspaceNameCompletions.class)
		public String name;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.query(new ClassReferenceQuery(name))
					.build();
		}
	}

	/**
	 * Command for searching for member references.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "mref", description = "Find member references.")
	public static class MemberUsage extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The string matching mode.")
		public StringMatchMode mode;
		@CommandLine.Option(names = "--owner", description = "The class name.",
				completionCandidates = WorkspaceNameCompletions.class)
		public String owner;
		@CommandLine.Option(names = "--name", description = "The member name.")
		public String name;
		@CommandLine.Option(names = "--desc", description = "The member descriptor.")
		public String desc;

		@Override
		public SearchCollector call() throws Exception {
			if(owner == null && name == null && desc == null) {
				error("Please give at least one parameter.");
				return new SearchCollector(getWorkspace(), Collections.emptyList());
			}
			return SearchBuilder.in(getWorkspace())
					.skipDebug()
					.query(new MemberReferenceQuery(owner, name, desc, mode))
					.build();
		}
	}

	/**
	 * Command for searching for string constants.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "string", description = "Find strings.")
	public static class Text extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The string matching mode.")
		public StringMatchMode mode;
		@CommandLine.Parameters(index = "1", description = "The text to match.")
		public String text;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.skipDebug()
					.query(new StringQuery(text, mode))
					.build();
		}
	}

	/**
	 * Command for searching for value constants.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "value", description = "Find value constants.")
	public static class Value extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The value to search for.")
		public Number value;

		@Override
		public SearchCollector call() throws Exception {
			return SearchBuilder.in(getWorkspace())
					.skipDebug()
					.query(new ValueQuery(value))
					.build();
		}
	}

	/**
	 * Command for searching for disassembled method code.
	 *
	 * @author Matt
	 */
	@CommandLine.Command(name = "code", description = "Find code matches.")
	public static class Disass extends ControllerCommand implements Callable<SearchCollector> {
		@CommandLine.Parameters(index = "0",  description = "The string matching mode.")
		public StringMatchMode mode;
		@CommandLine.Parameters(index = "1", description = "The lines of code to match, separated by ':'.")
		public String text;

		@Override
		public SearchCollector call() throws Exception {
			// Skip debug is used here so that variable names don't interfere with searching.
			// Using pure indices instead like "ALOAD 4" instead of "ALOAD varName"
			// ... Although it will still always o "ALOAD this" where possible
			return SearchBuilder.in(getWorkspace())
					.skipDebug()
					.query(new InsnTextQuery(Arrays.asList(text.split(":")), mode))
					.build();
		}
	}
}
