package me.coley.recaf.control.headless;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.MetaCommand;
import me.coley.recaf.command.impl.*;
import me.coley.recaf.control.Controller;
import me.coley.recaf.parse.bytecode.parser.NumericParser;
import me.coley.recaf.search.SearchCollector;
import me.coley.recaf.search.SearchResult;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.RegexUtil;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static me.coley.recaf.util.Log.*;

/**
 * Command line controller.
 *
 * @author Matt
 */
public class HeadlessController extends Controller {
	private final Map<String, Class<?>> lookup = new HashMap<>();
	private final Map<Class<?>, Consumer<?>> handlers = new HashMap<>();
	private final Path script;
	private boolean running = true;
	private JLineAdapter jline;

	/**
	 * @param workspace
	 * 		Initial workspace path. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 * @param script
	 * 		Script to run. May be {@code null}. If not {@code null} the commands will be executed
	 * 		then Recaf will terminate.
	 */
	public HeadlessController(Path workspace, Path script) {
		super(workspace);
		this.script = script;
	}

	@Override
	public void run() {
		super.run();
		// Start
		if(script != null) {
			// Script means no user input
			if(getWorkspace() == null)
				throw new IllegalArgumentException("No workspace was provided");
			//Parse script
			List<String> lines;
			try {
				lines = Files.readAllLines(script, StandardCharsets.UTF_8);
			} catch(IOException ex) {
				throw new IllegalArgumentException("Script file could not be read: " + script, ex);
			}
			// Run script
			for(String line : lines)
				handle(line);
		} else {
			// Interactive input
			try {
				jline = new JLineAdapter(this, this::handle);
				jline.loop();
			} catch(IOException ex) {
				throw new IllegalStateException("Failed to initialize terminal");
			}
		}
	}

	/**
	 * Handle user input string.
	 *
	 * @param in
	 * 		Line of input.
	 */
	private void handle(String in) {
		debug("Command: " + in);
		// Fetch command class
		int argsOffset = 1;
		// Split by
		String[] split = RegexUtil.wordSplit(in);
		if (split.length == 0)
			return;
		String name = split[0];
		Class<?> key = getClass(name);
		if (key == null) {
			error("No such command: '" + name + "'");
			return;
		}
		// Check for subcommand
		if(key.getDeclaredClasses().length > 0 && split.length > 1) {
			String tmpName = name + " " + split[argsOffset];
			Class<?> tmpKey = getClass(tmpName);
			if(tmpKey != null) {
				key = tmpKey;
				argsOffset++;
			}
		}
		Callable<?> command = get(key);
		String[] args = Arrays.copyOfRange(split, argsOffset, split.length);
		// Picocli command handling
		CommandLine cmd = new CommandLine(command);
		cmd.registerConverter(Number.class, s -> new NumericParser().visit(0, s).getValue());
		try {
			// Verify command can execute
			if (command instanceof ControllerCommand) {
				ControllerCommand wsCommand = (ControllerCommand)command;
				wsCommand.setController(this);
				wsCommand.verify();
			}
			// Have picocli auto-populate annotated fields.
			cmd.parseArgs(args);
			// Meta commands should be fed command info after field population for some reason... odd
			if (command instanceof MetaCommand)
				((MetaCommand) command).setContext(cmd);
			// Give help command access to all other commands
			if (command instanceof Help)
				for (Map.Entry<String, Class<?>> subCommEntry : lookup.entrySet())
					if (!subCommEntry.getKey().contains(" "))
						cmd.addSubcommand(new CommandLine(get(subCommEntry.getValue())));
			// Invoke the command
			cmd.setExecutionResult(command.call());
			// Handle result
			if (handlers.containsKey(key))
				handlers.get(key).accept(cmd.getExecutionResult());
		} catch (CommandLine.ParameterException ex) {
			// Raised from invalid user input, show usage and error.
			error(ex.getMessage() + "\nSee 'help " + name + "' for usage.");
			//ex.printStackTrace();
		} catch (Exception ex) {
			// Raised from callable command
			error(ex, "Command '{}' threw an exception", name);
		}
	}

	/**
	 * @return Interactive terminal session is still active.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @return Map of command names to their classes.
	 */
	public Map<String, Class<?>> getLookup() {
		return lookup;
	}

	/**
	 * @param name
	 * 		Command name.
	 * @param <R>
	 * 		Return type of callable.
	 * @param <T>
	 * 		Callable implementation.
	 *
	 * @return Class of command.
	 */
	@SuppressWarnings("unchecked")
	private <R, T extends Callable<R>> Class<T> getClass(String name) {
		return (Class<T>) lookup.get(name);
	}

	@Override
	protected <R, T extends Callable<R>> void register(Class<T> clazz) {
		super.register(clazz);
		// Add name lookup
		CommandLine.Command comm =  clazz.getDeclaredAnnotation(CommandLine.Command.class);
		if (comm == null)
			throw new IllegalStateException("Callable class does not have required command annotation: " +
					clazz.getName());
		String name = comm.name();
		lookup.put(name, clazz);
		// Add name lookup for sub-commands
		for(Class<?> subclass : comm.subcommands()) {
			comm = subclass.getDeclaredAnnotation(CommandLine.Command.class);
			if(comm == null)
				throw new IllegalStateException("Callable class does not have required command annotation: " +
						subclass.getName());
			String subname = comm.name();
			lookup.put(name + " " + subname, subclass);
		}
	}

	/**
	 * @param clazz
	 * 		Command class.
	 * @param consumer
	 * 		Consumer that handles the return value of the command.
	 * @param <R>
	 * 		Return type of callable.
	 * @param <T>
	 * 		Callable implementation.
	 */
	private <R, T extends Callable<R>> void registerHandler(Class<T> clazz, Consumer<R> consumer) {
		handlers.put(clazz, consumer);
	}

	@Override
	public boolean setup() {
		boolean success = super.setup();
		//
		Consumer<SearchCollector> printResults = r -> {
			for (SearchResult res : r.getAllResults())
				info("{}\n{}", res.getContext(), res.toString());
		};
		//
		registerHandler(Disassemble.class, v -> {
			// Interactive if JLine is active and no external output is given
			// - Intent is that external output implies potential external scripting
			if (jline == null || v.getDestination() != null)
				info(v.getDisassembled());
			else
				jline.handleDisassemble(v);
		});
		registerHandler(LoadWorkspace.class, this::setWorkspace);
		registerHandler(Decompile.class, Log::info);
		registerHandler(Search.ClassInheritance.class, printResults);
		registerHandler(Search.ClassName.class, printResults);
		registerHandler(Search.Member.class, printResults);
		registerHandler(Search.ClassUsage.class, printResults);
		registerHandler(Search.MemberUsage.class, printResults);
		registerHandler(Search.Text.class, printResults);
		registerHandler(Search.Value.class, printResults);
		registerHandler(Search.Disass.class, printResults);
		registerHandler(Quit.class, v -> running = false);
		return success;
	}
}
