package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.completion.WorkspaceNameCompletions;
import me.coley.recaf.parse.bytecode.Disassembler;
import me.coley.recaf.util.ClassUtil;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Command for disassembling a method.
 *
 * @author Matt
 */
@CommandLine.Command(name = "disassemble", description = "Disassemble a method.")
public class Disassemble extends ControllerCommand implements Callable<Disassemble.Result> {
	@CommandLine.Parameters(index = "0",  description = "The class containing the method",
			completionCandidates = WorkspaceNameCompletions.class)
	public String className;
	@CommandLine.Parameters(index = "1",  description = "Method definition, name and descriptor. " +
			"For example 'method()V'", completionCandidates = WorkspaceNameCompletions.class)
	public String methodDef;
	@CommandLine.Option(names = { "--destination" }, description = "File to write disassembled code to.")
	public File destination;

	/**
	 * @return Disassembly wrapper.
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, cannot find class/method</li></ul>
	 */
	@Override
	public Result call() throws Exception {
		if(className == null || className.isEmpty())
			throw new IllegalStateException("No class specified");
		if(!getWorkspace().getPrimary().getClasses().containsKey(className))
			throw new IllegalStateException("No class by the name '" + className +
					"' exists in the primary resource");
		int descStart = methodDef.indexOf("(");
		if (descStart == -1)
			throw new IllegalStateException("Invalid method def '" + methodDef + "'");
		// Get method
		ClassReader reader = getWorkspace().getClassReader(className);
		ClassNode node = ClassUtil.getNode(reader, ClassReader.SKIP_FRAMES);
		String name = methodDef.substring(0, descStart);
		String desc = methodDef.substring(descStart);
		MethodNode method = null;
		for (MethodNode mn : node.methods)
			if(mn.name.equals(name) && mn.desc.equals(desc)) {
				method = mn;
				break;
			}
		if (method == null)
			throw new IllegalStateException("No method '" + methodDef + "' found in '" + className + "'");
		// Disassemble
		Result result = new Result(node, method, destination);
		if (destination != null)
			FileUtils.write(destination, result.disassembled, UTF_8);
		return result;
	}

	/**
	 * Disassemble command result wrapper.
	 */
	public static class Result {
		private final ClassNode owner;
		private final MethodNode method;
		private final String disassembled;
		private final File destination;

		private Result(ClassNode owner, MethodNode method, File destination) {
			this.owner =owner;
			this.method = method;
			this.destination = destination;
			Disassembler disassembler = new Disassembler();
			disassembled = disassembler.disassemble(method);
		}

		/**
		 * @return Class that holds the method.
		 */
		public ClassNode getOwner() {
			return owner;
		}

		/**
		 * @return The method that's been disassembled.
		 */
		public MethodNode getMethod() {
			return method;
		}

		/**
		 * @return The disassembled method code.
		 */
		public String getDisassembled() {
			return disassembled;
		}

		/**
		 * @return The file to store the disassembled code in.
		 */
		public File getDestination() {
			return destination;
		}
	}
}
