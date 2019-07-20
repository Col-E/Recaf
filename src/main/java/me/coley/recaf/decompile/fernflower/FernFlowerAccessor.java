package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.*;
import org.jetbrains.java.decompiler.main.extern.*;
import org.jetbrains.java.decompiler.struct.*;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.io.IOException;
import java.util.*;

/**
 * FernFlower accessor. Modified from {@link org.jetbrains.java.decompiler.main.Fernflower} to
 * allow fileless decompilation.
 *
 * @author Matt
 */
public class FernFlowerAccessor implements IDecompiledData {
	private final StructContextDecorator structContext;
	private final ClassesProcessor classProcessor;

	/**
	 * Constructs a FernFlower decompiler instance.
	 *
	 * @param provider
	 * 		Class file provider.
	 * @param saver
	 * 		Decompilation output saver <i>(Unused/noop)</i>
	 * @param properties
	 * 		FernFlower options.
	 * @param logger
	 * 		FernFlower logger instance.
	 */
	public FernFlowerAccessor(IBytecodeProvider provider, IResultSaver saver, Map<String, Object>
			properties, IFernflowerLogger logger) {
		String level = (String) properties.get(IFernflowerPreferences.LOG_LEVEL);
		if(level != null) {
			logger.setSeverity(IFernflowerLogger.Severity.valueOf(level.toUpperCase(Locale.ENGLISH)));
		}
		structContext = new StructContextDecorator(saver, this, new LazyLoader(provider));
		classProcessor = new ClassesProcessor(structContext);
		DecompilerContext context = new DecompilerContext(properties, logger, structContext,
				classProcessor, null);
		DecompilerContext.setCurrentContext(context);
	}

	/**
	 * @param workspace
	 * 		Recaf workspace to pull classes from.
	 *
	 * @throws IOException
	 * 		Thrown if a class cannot be read.
	 * @throws ReflectiveOperationException
	 * 		Thrown if the parent loader could not be fetched.
	 */
	public void addWorkspace(Workspace workspace) throws IOException, ReflectiveOperationException {
		structContext.addWorkspace(workspace);
	}

	/**
	 * Analyze classes in the workspace.
	 */
	public void analyze() {
		classProcessor.loadClasses(null);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Decompilation of the class.
	 */
	public String decompile(String name) {
		StructClass clazz = structContext.getClass(name);
		if (clazz == null)
			throw new IllegalArgumentException("FernFlower could not find \"" + name + "\"");
		return getClassContent(clazz);
	}

	@Override
	public String getClassEntryName(StructClass cl, String entryName) {
		ClassesProcessor.ClassNode node = classProcessor.getMapRootClasses().get(cl.qualifiedName);
		if(node.type != ClassesProcessor.ClassNode.CLASS_ROOT) {
			return null;
		} else {
			return entryName.substring(0, entryName.lastIndexOf(".class")) + ".java";
		}
	}

	@Override
	public String getClassContent(StructClass cl) {
		try {
			TextBuffer buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
			buffer.append(DecompilerContext.getProperty(IFernflowerPreferences.BANNER).toString());
			classProcessor.writeClass(cl, buffer);
			return buffer.toString();
		} catch(Throwable t) {
			DecompilerContext.getLogger().writeMessage("Class " + cl.qualifiedName +
					" couldn't be fully decompiled.", t);
			return null;
		}
	}
}