package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.IdentityRenamerFactory;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;
import org.jetbrains.java.decompiler.struct.IDecompiledData;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

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
		if (level != null) {
			logger.setSeverity(IFernflowerLogger.Severity.valueOf(level.toUpperCase(Locale.ENGLISH)));
		}
		structContext = new StructContextDecorator(saver, this, new LazyLoader(provider));
		classProcessor = new ClassesProcessor(structContext);
		int threadCount = 1;
		DecompilerContext context = new DecompilerContext(
				properties, threadCount, logger, structContext, classProcessor,
				new PoolInterceptor(), new IdentityRenamerFactory());
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
	 * @throws IndexOutOfBoundsException
	 * 		Thrown if FernFlower can't read the class.
	 * 		<i>(IE: It fails on newer Java class files)</i>
	 */
	public void addWorkspace(Workspace workspace) throws IOException, ReflectiveOperationException {
		structContext.addWorkspace(workspace);
	}

	/**
	 * Analyze classes in the workspace.
	 */
	public void analyze() throws IOException, InterruptedException {
		classProcessor.loadClasses(null);
		// The threading model with FF makes no damn sense and there is NO documentaiton.
		// Uuggggghhhhhhh....
		// Sorry, you get shit performance on a single thread until I find some docs.
		/*
		DecompilerContext root = DecompilerContext.getCurrentContext();
		ExecutorService pool = Executors.newWorkStealingPool();
		for (StructClass cl : structContext.getClasses().values())
			pool.submit(() -> {
				try {
					DecompilerContext.cloneContext(root);
					classProcessor.processClass(cl);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			});
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.SECONDS);
		 */
		for (StructClass cl : structContext.getClasses().values())
			try {
				classProcessor.processClass(cl);
			} catch (Throwable t) {
				t.printStackTrace();
			}
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
		if (node.type != ClassesProcessor.ClassNode.CLASS_ROOT) {
			return null;
		} else {
			return entryName.substring(0, entryName.lastIndexOf(".class")) + ".java";
		}
	}

	@Override
	public boolean processClass(StructClass structClass) {
		try {
			classProcessor.processClass(structClass);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public String getClassContent(StructClass cl) {
		TextBuffer buffer;
		synchronized (this) {
			// FernFlower does some wonky thread-local behavior.... just.... ugh, why?
			buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
		}
		String name = cl.qualifiedName;
		try {
			Object banner = DecompilerContext.getProperty(IFernflowerPreferences.BANNER);
			if (banner != null && !banner.toString().trim().isEmpty())
				buffer.append(banner.toString() + "\n");
			ClassesProcessor.ClassNode node = classProcessor.getMapRootClasses().get(name);
			// Why are we changing the node type?
			// Because the ClassesProcessor ignores classes with non-root types.
			//
			// Treat standard inner classes as root classes.
			if (node.type == ClassesProcessor.ClassNode.CLASS_MEMBER)
				node.type = ClassesProcessor.ClassNode.CLASS_ROOT;
			// Treat anonymous classes as root classes.
			// - Apply name so it doesn't output "public class null extends whatever"
			if (node.type == ClassesProcessor.ClassNode.CLASS_ANONYMOUS) {
				node.type = ClassesProcessor.ClassNode.CLASS_ROOT;
				node.simpleName = name.substring(name.lastIndexOf("/") + 1);
			}
			classProcessor.writeClass(cl, buffer);
		} catch (Throwable t) {
			DecompilerContext.getLogger().writeMessage("Class " + name + " couldn't be fully decompiled.", t);
			// Put exception into output so users know it failed.
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			buffer.append("/*\n" + sw.toString() + "\n*/");
		}
		return buffer.toString();
	}
}