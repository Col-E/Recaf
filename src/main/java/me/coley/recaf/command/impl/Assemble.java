package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.completion.WorkspaceNameCompletions;
import me.coley.recaf.metadata.Comments;
import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.util.ClassUtil;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import picocli.CommandLine;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.Recaf;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Command for assembling a method.
 *
 * @author Matt
 */
@CommandLine.Command(name = "assemble", description = "Assemble a method.")
public class Assemble extends ControllerCommand implements Callable<Assemble.Result> {
	@CommandLine.Parameters(index = "0",  description = "The class containing the method",
			completionCandidates = WorkspaceNameCompletions.class)
	public String className;
	@CommandLine.Parameters(index = "1",  description = "Method definition, name and descriptor. " +
			"For example 'method()V'", completionCandidates = WorkspaceNameCompletions.class)
	public String methodDef;
	@CommandLine.Parameters(index = "2", description = "File to load bytecode from")
	public File input;
	@CommandLine.Option(names = { "--debug" }, description = "Compile with debug info.", defaultValue = "true")
	public boolean debug = true;

	/**
	 * @return Assembly wrapper.
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, cannot find class/method</li></ul>
	 * 		<ul><li>LineParseException, cannot compile bytecode</li></ul>
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
		// Get info - need method access
		ClassReader reader = getWorkspace().getClassReader(className);
		ClassNode node = ClassUtil.getNode(reader, ClassReader.SKIP_FRAMES);
		String name = methodDef.substring(0, descStart);
		String desc = methodDef.substring(descStart);
		MethodNode method = null;
		int methodIndex = -1;
		for (int i = 0; i < node.methods.size(); i++) {
			MethodNode mn = node.methods.get(i);
			if(mn.name.equals(name) && mn.desc.equals(desc)) {
				method = mn;
				methodIndex = i;
				break;
			}
		}
		if (method == null)
			throw new IllegalStateException("No method '" + methodDef + "' found in '" + className + "'");
		// Assemble method
		String code;
		try {
			code = FileUtils.readFileToString(input, UTF_8);
		} catch(IOException ex) {
			throw new IllegalStateException("Could not read from '" + input + "'");
		}
		ParseResult<RootAST> result = Parse.parse(code);
		MethodAssembler assembler = new MethodAssembler(className, getController());
		MethodNode generated = assembler.compile(result);
		// Replace method
		MethodNode old = node.methods.get(methodIndex);
		Comments.removeComments(old);
		ClassUtil.copyMethodMetadata(old, generated);
		node.methods.set(methodIndex, generated);
		//Finalize changes
		Workspace workspace = Recaf.getCurrentWorkspace();
		ClassWriter cw = workspace.createWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor visitor = cw;
		for (ClassVisitorPlugin visitorPlugin : PluginsManager.getInstance()
				.ofType(ClassVisitorPlugin.class)) {
			visitor = visitorPlugin.intercept(visitor);
		}
		node.accept(visitor);
		byte[] value = cw.toByteArray();
		workspace.getPrimary().getClasses().put(node.name, value);		
		// Return wrapper
		return new Result(node, generated);
	}

	/**
	 * Assemble command result wrapper.
	 */
	public static class Result {
		private final ClassNode owner;
		private final MethodNode method;

		private Result(ClassNode owner, MethodNode method) {
			this.owner =owner;
			this.method = method;
		}

		/**
		 * @return Class that holds the method.
		 */
		public ClassNode getOwner() {
			return owner;
		}

		/**
		 * @return The assembled method.
		 */
		public MethodNode getMethod() {
			return method;
		}
	}
}
