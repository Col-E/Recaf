package me.coley.recaf.control.headless;

import me.coley.recaf.util.DefineUtil;
import org.objectweb.asm.*;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.util.Collection;

/**
 * Generator that creates a class containing subcommands for picocli.
 * Useful for tab-completion.
 *
 * @author Matt
 */
public class SubContainerGenerator implements Opcodes {
	/**
	 * @param commands
	 * 		List of sub-commands to support in the generated command.
	 *
	 * @return A picocli command instance that contains lists all the given commands as static
	 * sub-command references.
	 */
	public static Object generate(Collection<Class<?>> commands) {
		// Create a dummy command class that has all classes as subcommands
		byte[] code = createDummyCommand(commands);
		// Make an isntance of the dummy command
		try {
			return DefineUtil.create("me.coley.recaf.command.JLineCompat", code);
		} catch(Exception ex) {
			throw new IllegalStateException("Failed to generate JLine compatibility class!", ex);
		}
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
		AnnotationVisitor avCommand;
		// Extend runnable to support picocli signature.
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, "me/coley/recaf/command/JLineCompat", null, "java" +
				"/lang/Object", new String[]{"java/lang/Runnable"});
		// Setup the annotation
		avCommand = cw.visitAnnotation("Lpicocli/CommandLine$Command;", true);
		// Hidden so we don't see this generated class.
		// - Would be bad for tab completion
		avCommand.visit("hidden", Boolean.TRUE);
		avCommand.visit("name", "shell");
		AnnotationVisitor avDesc = avCommand.visitArray("description");
		avDesc.visit(null, "CLI implementation");
		avDesc.visitEnd();
		// Add our subcommands
		AnnotationVisitor avSubCommands = avCommand.visitArray("subcommands");
		for(Class<?> c : commands)
			avSubCommands.visit(null, Type.getType("L" + c.getName().replace(".", "/") + ";"));
		avSubCommands.visitEnd();
		avCommand.visitEnd();
		//
		cw.visitInnerClass("picocli/CommandLine$Command", "picocli/CommandLine", "Command",
				ACC_PUBLIC + ACC_STATIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE);
		// Create constructor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		// Create Runnable.run()
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
		cw.visitEnd();
		return cw.toByteArray();
	}
}
