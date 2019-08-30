package me.coley.recaf.command;

import me.coley.recaf.command.impl.*;
import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Command line controller.
 *
 * @author Matt
 */
public class HeadlessController extends Controller {
	private final Map<String, Class<?>> lookup = new HashMap<>();
	private final Map<Class<?>, Consumer<?>> handlers = new HashMap<>();
	private final File script;
	private boolean running = true;

	/**
	 * @param workspace
	 * 		Initial workspace file. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 * @param script
	 * 		Script to run. May be {@code null}. If not {@code null} the commands will be executed
	 * 		then Recaf will terminate.
	 */
	public HeadlessController(File workspace, File script) {
		super(workspace);
		this.script = script;
	}


	@Override
	public void run() {
		// Try to load passed workspace
		try {
			loadInitialWorkspace();
			if (getWorkspace() != null)
				Logger.info("Loaded workspace from: " + initialWorkspace);
		} catch(Exception ex) {
			Logger.error("Error loading workspace from file: " + initialWorkspace, ex);
		}
		// Start
		if(script != null) {
			// Script means no user input
			if(getWorkspace() == null)
				throw new IllegalArgumentException("No workspace was provided");
			//Parse script
			List<String> lines;
			try {
				lines = FileUtils.readLines(script, StandardCharsets.UTF_8);
			} catch(IOException ex) {
				throw new IllegalArgumentException("Script file could not be read: " + script, ex);
			}
			// Run script
			for(String line : lines)
				handle(line);
		} else {
			// Interactive input
			Scanner scanner = new Scanner(System.in);
			do {
				// Ensure a workspace is open
				if(getWorkspace() == null) {
					Logger.info("Please input the path to a java program (class, jar) or workspace file (json).\nSee documentation below:\n");
					usage(get(LoadWorkspace.class));
				}
				// Prompt & run commands from user input
				System.out.print("\n$ ");
				String in = scanner.nextLine();
				if (!in.isEmpty())
					handle(in);
			} while(running);
		}
	}

	/**
	 * Handle user input string.
	 *
	 * @param in
	 * 		Line of input.
	 */
	private void handle(String in) {
		// Fetch command class
		String name = in.trim().split(" ")[0];
		Class<?> key = getClass(name);
		if (key == null) {
			Logger.error("No such command: '" + name + "'");
			return;
		}
		Callable<?> command = get(key);
		String[] split = in.split(" ");
		String[] args = Arrays.copyOfRange(split, 1, split.length);
		// Picocli command handling
		CommandLine cmd = new CommandLine(command);
		try {
			// Verify command can execute
			if (command instanceof WorkspaceCommand) {
				WorkspaceCommand wsCommand = (WorkspaceCommand)command;
				wsCommand.setWorkspace(getWorkspace());
				wsCommand.verify();
			}
			// Have picocli auto-populate annotated fields.
			cmd.parseArgs(args);
			// Help command should be fed command info after field population for some reason... odd
			if (command instanceof Help) {
				for (Class<?> subCommKey : lookup.values())
					cmd.addSubcommand(new CommandLine(get(subCommKey)));
				((Help) command).context = cmd;
			}
			// Invoke the command
			cmd.setExecutionResult(command.call());
			// Handle result
			if (handlers.containsKey(key))
				handlers.get(key).accept(cmd.getExecutionResult());
		} catch (CommandLine.ParameterException ex) {
			// Raised from invalid user input, show usage and error.
			Logger.error(ex.getMessage() + "\nSee 'help " + name + "' for usage.");
			//ex.printStackTrace();
		} catch (Exception ex) {
			// Raised from callable command
			Logger.error(ex);
		}
	}

	/**
	 * Print usage of command.
	 *
	 * @param command
	 * 		Command to show usage of.
	 */
	private void usage(Callable<?> command) {
		CommandLine cmd = new CommandLine(command);
		cmd.usage(cmd.getOut());
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
	protected void setup() {
		super.setup();
		registerHandler(LoadWorkspace.class, this::setWorkspace);
		registerHandler(Quit.class, v -> running = false);
	}
}
