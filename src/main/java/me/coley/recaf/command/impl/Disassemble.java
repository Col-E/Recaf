package me.coley.recaf.command.impl;

import me.coley.recaf.command.completion.WorkspaceNameCompletions;
import me.coley.recaf.parse.assembly.Disassembler;
import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.util.ClassUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Command for disassembling a method.
 *
 * @author Matt
 */
@CommandLine.Command(name = "disassemble", description = "Disassemble and a method.")
public class Disassemble extends WorkspaceCommand implements Callable<Disassemble.Result> {
	@CommandLine.Parameters(index = "0",  description = "The class containing the method",
			completionCandidates = WorkspaceNameCompletions.class)
	public String className;
	@CommandLine.Parameters(index = "1",  description = "Method defintiion, name and descriptor. " +
			"For example 'method()V'", completionCandidates = WorkspaceNameCompletions.class)
	public String methodDef;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, cannot find class/method</li></ul>
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Result call() throws Exception {
		if(className == null || className.isEmpty())
			throw new IllegalStateException("No class specified");
		if(!workspace.getPrimary().getClasses().containsKey(className))
			throw new IllegalStateException("No class by the name '" + className +
					"' exists in the primary resource");
		int descStart = methodDef.indexOf("(");
		if (descStart == -1)
			throw new IllegalStateException("Invalid method def '" + methodDef + "'");
		// Get method
		ClassReader reader = workspace.getClassReader(className);
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
		return new Result(node, method);
	}

	public static class Result {
		private ClassNode owner;
		private MethodNode method;
		private String disassembled;

		private Result(ClassNode owner, MethodNode method) throws LineParseException {
			this.owner =owner;
			this.method = method;
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
	}
}
