package me.coley.recaf.control.headless;

import me.coley.recaf.command.impl.Disassemble;
import me.coley.recaf.command.impl.LoadWorkspace;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.objectweb.asm.*;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Collectors;

import static me.coley.recaf.util.Log.*;

/**
 * JLine tie-in for the headless controller. Allows tab-completion on supported terminals.
 *
 * @author Matt
 */
public class JLineAdapter implements Opcodes {
	private HeadlessController controller;
	private final Consumer<String> handler;
	private Terminal terminal;
	private LineReader reader;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param handler
	 * 		Command input handler.
	 *
	 * @throws IOException
	 * 		Thrown when the terminal cannot be built.
	 */
	JLineAdapter(HeadlessController controller, Consumer<String> handler)throws IOException {
		this.controller = controller;
		this.handler = handler;
		setupJLine();
		checkWorkspace();
	}

	private void setupJLine() throws IOException {
		terminal = TerminalBuilder.builder().build();
		reader = setupCompletionReader(terminal, controller.getLookup());
	}

	private void checkWorkspace() {
		// Ensure a workspace is open
		if(controller.getWorkspace() == null) {
			info("Please input the path to a java program (class, jar) " + "or workspace " +
					"file (json).\nSee documentation below:\n");
			usage(controller.get(LoadWorkspace.class));
		}
	}

	/**
	 * Handle input with JLine.
	 */
	void loop() {
		// Prompt & run commands from user input
		while(controller.isRunning()) {
			try {
				String line = reader.readLine("\n$ ", null, (MaskingCallback) null, null);
				ParsedLine pl = reader.getParser().parse(line, 0);
				handler.accept(pl.line());
			} catch(UserInterruptException e) {
				// Ignore
			} catch(EndOfFileException e) {
				return;
			}
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
	 * @param terminal
	 * 		Terminal to add tab-completion to.
	 * @param lookup
	 * 		Map containing commands.
	 *
	 * @return Reader with tab-completion.
	 */
	private static LineReader setupCompletionReader(Terminal terminal, Map<String, Class<?>> lookup) {
		// Filter root level commands
		Collection<Class<?>> commands =	lookup.entrySet().stream()
				.filter(e -> !e.getKey().contains(" "))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
		// Pass dummy to pico for tab-completion
		CommandLine cmd = new CommandLine(SubContainerGenerator.generate(commands));
		return LineReaderBuilder.builder()
				.terminal(terminal)
				.completer(new PicocliJLineCompleter(cmd.getCommandSpec()))
				.parser(new DefaultParser())
				.build();
	}

	/**
	 * Use the JLine nano tool to modify the disassembled code.
	 *
	 * @param result
	 * 		Disassemble output wrapper.
	 */
	void handleDisassemble(Disassemble.Result result) {
		JLineEditor editor = new JLineEditor(terminal);
		editor.open(result);
	}

	static {
		// Disable native JLine logging
		java.util.logging.Logger.getLogger("org.jline").setLevel(Level.OFF);
	}
}
