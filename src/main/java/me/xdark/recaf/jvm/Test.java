package me.xdark.recaf.jvm;

import me.coley.recaf.command.impl.LoadWorkspace;
import me.coley.recaf.workspace.Workspace;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import me.xdark.recaf.jvm.classloading.WorkspaceClassLoader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;

public final class Test {

	public static void main(String[] args) throws Throwable {
		LoadWorkspace load = new LoadWorkspace();
		load.input = new File("c:\\Users\\User\\Desktop\\Test.class");
		Workspace workspace = load.call();
		Compiler compiler = new StandardCompiler();
		VirtualMachine vm = new VirtualMachine(workspace, compiler);
		ClassLoader loader = new WorkspaceClassLoader(workspace, vm, compiler, null);
	}
}
