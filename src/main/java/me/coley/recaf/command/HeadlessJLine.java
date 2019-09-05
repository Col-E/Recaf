package me.coley.recaf.command;

import me.coley.recaf.command.impl.LoadWorkspace;
import me.coley.recaf.util.DefineUtil;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.objectweb.asm.*;
import org.tinylog.Logger;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * JLine tie-in for the headless controller. Allows tab-completion on supported terminals.
 *
 * @author Matt
 */
public class HeadlessJLine implements Opcodes {
	private HeadlessController controller;
	private final Consumer<String> handler;
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
	HeadlessJLine(HeadlessController controller, Consumer<String> handler)throws IOException {
		this.controller = controller;
		this.handler = handler;
		setupJLine();
		checkWorkspace();
	}

	private void setupJLine() throws IOException {
		Terminal terminal = TerminalBuilder.builder().build();
		reader = setupCompletionReader(terminal, controller.getLookup());
	}

	private void checkWorkspace() {
		// Ensure a workspace is open
		if(controller.getWorkspace() == null) {
			Logger.info("Please input the path to a java program (class, jar) " + "or workspace " +
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
		// Create a dummy command class that has all classes as subcommands
		byte[] code = createDummyCommand(commands);
		// Make an isntance of the dummy command
		Object obj = null;
		try {
			obj = DefineUtil.create("me.coley.recaf.command.JLineCompat", code);
		} catch(Exception ex) {
			throw new IllegalStateException("Failed to generate JLine compatibility class!", ex);
		}
		// Pass dummy to pico for tab-completion
		CommandLine cmd = new CommandLine(obj);
		return LineReaderBuilder.builder()
				.terminal(terminal)
				.completer(new PicocliJLineCompleter(cmd.getCommandSpec()))
				.parser(new DefaultParser())
				.build();
	}

	/**
	 * @param commands
	 * 		Collection of sub-commands to add to the generated class.
	 *
	 * @return A generated class used for the {@link PicocliJLineCompleter}
	 */
	private static byte[] createDummyCommand(Collection<Class<?>> commands) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		MethodVisitor mv;
		AnnotationVisitor av;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, "me/coley/recaf/command/JLineCompat", null, "java" +
				"/lang/Object", new String[]{"java/lang/Runnable"});
		{
			av = cw.visitAnnotation("Lpicocli/CommandLine$Command;", true);
			av.visit("hidden", Boolean.TRUE);
			av.visit("name", "shell");
			{
				AnnotationVisitor av1 = av.visitArray("description");
				av1.visit(null, "CLI implementation");
				av1.visitEnd();
			}
			{
				AnnotationVisitor av1 = av.visitArray("subcommands");
				for(Class<?> c : commands) {
					av1.visit(null, Type.getType("L" + c.getName().replace(".", "/") + ";"));
				}
				av1.visitEnd();
			}
			av.visitEnd();
		}

		cw.visitInnerClass("picocli/CommandLine$Command", "picocli/CommandLine", "Command",
				ACC_PUBLIC + ACC_STATIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(7, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "Lme/coley/recaf/command/JLineCompat;", null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(10, l0);
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang" +
					"/String;)V", false);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLineNumber(11, l1);
			mv.visitInsn(RETURN);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLocalVariable("this", "Lme/coley/recaf/command/JLineCompat;", null, l0, l2, 0);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		cw.visitEnd();
		return cw.toByteArray();
	}

	static {
		// Disable native JLine logging
		java.util.logging.Logger.getLogger("org.jline").setLevel(Level.OFF);
	}
}
