package me.coley.recaf.decompile.quilt;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.IdentityRenamerFactory;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.IDecompiledData;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.java.decompiler.util.TextBuffer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

/**
 * QuiltFlower decompiler implementation.
 *
 * @author Matt Coley
 */
public class QuiltFlowerDecompiler extends Decompiler implements IDecompiledData, IBytecodeProvider, IResultSaver {
	private static final Logger logger = Logging.get(QuiltFlowerDecompiler.class);
	private final Map<String, Object> fernFlowerProperties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
	private final ThreadLocal<String> targetClass = new ThreadLocal<>();
	private final ThreadLocal<String> targetDecompiled = new ThreadLocal<>();
	private final ThreadLocal<ClassesProcessor> processor = new ThreadLocal<>();
	private final LazyLoader loader = new LazyLoader(this);

	public QuiltFlowerDecompiler() {
		super("QuiltFlower", "1.8.1");
		fernFlowerProperties.put("ind", "    ");
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		try {
			StructContext structContext = new StructContext(this, this, loader);
			ClassesProcessor classesProcessor = new ClassesProcessor(structContext);
			processor.set(classesProcessor);
			IFernflowerLogger ffLogger = new IFernflowerLogger() {
				@Override
				public void writeMessage(String message, Severity severity) {
					logger.trace("QuiltFlower: {}", message);
				}

				@Override
				public void writeMessage(String message, Severity severity, Throwable t) {
					logger.error("QuiltFlower: {}", message, t);
				}
			};
			DecompilerContext decompilerContext = new DecompilerContext(fernFlowerProperties, ffLogger, structContext, classesProcessor,
					null, s -> new IdentityRenamerFactory());
			// Reset input
			String name = classInfo.getName();
			targetClass.set(classInfo.getName());
			targetDecompiled.set(null);
			// Update the thread-local decompiler context
			DecompilerContext.setCurrentContext(decompilerContext);
			// FF wants some odd naming convention where '.class' suffix always exists
			String qualified = (name + ".class");
			// Load in the data
			byte[] code = applyInterceptors(classInfo.getValue());
			structContext.addData(name, qualified, code, true);
			classesProcessor.loadClasses(null);
			// Decompile
			structContext.saveContext();
			structContext.close();
			// Cleanup and yield value
			processor.set(null);
			String decompiled = targetDecompiled.get();
			if (decompiled == null)
				return "// Failed to decompile: " + name;
			return decompiled;
		} catch (Exception e) {
			processor.set(null);
			logger.error("QuiltFlower encountered an error when decompiling", e);
			return "// " + StringUtil.traceToString(e).replace("\n", "\n// ");
		}
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		// TODO: When we update the decompile-options, we should update 'fernFlowerProperties'
		return new HashMap<>();
	}

	@Override
	public String getClassEntryName(StructClass cl, String entryName) {
		ClassesProcessor proc = processor.get();
		if (proc == null)
			return null;
		ClassesProcessor.ClassNode node = proc.getMapRootClasses().get(cl.qualifiedName);
		if (node == null || node.type != ClassesProcessor.ClassNode.CLASS_ROOT) {
			return null;
		} else {
			return cl.qualifiedName;
		}
	}

	@Override
	public String getClassContent(StructClass cl) {
		try {
			ClassesProcessor proc = processor.get();
			if (proc == null)
				throw new IllegalStateException("Thread local ClassesProcessor not available!");
			TextBuffer buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
			proc.writeClass(cl, buffer);
			return buffer.convertToStringAndAllowDataDiscard();
		} catch (Throwable t) {
			DecompilerContext.getLogger().writeMessage("Class " + cl.qualifiedName + " couldn't be fully decompiled.", t);
			return null;
		}
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		// For some reason 'qualifiedName' is the internal class name.
		if (qualifiedName.equals(targetClass.get()))
			targetDecompiled.set(content);
	}

	@Override
	public byte[] getBytecode(String externalPath, String internalPath) {
		// Does not seem to be used
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveFolder(String path) {
		// no-op
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
		// no-op
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
		// no-op
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
		// no-op
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
		// no-op
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		// no-op
	}

	@Override
	public void closeArchive(String path, String archiveName) {
		// no-op
	}
}
