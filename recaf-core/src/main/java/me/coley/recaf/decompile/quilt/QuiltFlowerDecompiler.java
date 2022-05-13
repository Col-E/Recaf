package me.coley.recaf.decompile.quilt;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.ReflectUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private final Map<String, Boolean> hasClassCache = new HashMap<>();
	private final LazyLoader loader = new LazyLoader(this);
	private final List<Map<?, ?>> inputCaches = new ArrayList<>();
	private final StructContext structContext;
	private final ClassesProcessor classProcessor;
	private final DecompilerContext decompilerContext;
	private Workspace currentWorkspace;

	public QuiltFlowerDecompiler() {
		super("QuiltFlower", "1.8.1");
		structContext = new StructContext(this, this, loader) {
			@Override
			public StructClass getClass(String name) {
				prefetchMissingClass(name);
				return super.getClass(name);
			}
		};
		classProcessor = new ClassesProcessor(structContext);
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
		fernFlowerProperties.put("ind", "    ");
		decompilerContext = new DecompilerContext(fernFlowerProperties, ffLogger, structContext, classProcessor,
				null, s -> new IdentityRenamerFactory());
		populateClearableCache();
	}

	@Override
	protected synchronized String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		try {
			currentWorkspace = workspace;
			// Reset input
			String name = classInfo.getName();
			targetClass.set(classInfo.getName());
			targetDecompiled.set(null);
			// Update the thread-local decompiler context
			DecompilerContext.setCurrentContext(decompilerContext);
			// Clear old content
			hasClassCache.clear();
			inputCaches.forEach(Map::clear);
			// Load in the class we want to decompile
			addClassToStructCtx(classInfo, true);
			classProcessor.loadClasses(null);
			// Decompile
			structContext.saveContext();
			structContext.close();
			// Cleanup and yield value
			currentWorkspace = null;
			String decompiled = targetDecompiled.get();
			if (decompiled == null)
				return "// Failed to decompile: " + name;
			return decompiled;
		} catch (Exception e) {
			currentWorkspace = null;
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
		ClassesProcessor.ClassNode node = classProcessor.getMapRootClasses().get(cl.qualifiedName);
		if (node == null || node.type != ClassesProcessor.ClassNode.CLASS_ROOT) {
			return null;
		} else {
			return cl.qualifiedName;
		}
	}

	@Override
	public String getClassContent(StructClass cl) {
		try {
			TextBuffer buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
			classProcessor.writeClass(cl, buffer);
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

	/**
	 * This exists because we want to clear inputs as described by {@link #populateClearableCache()}
	 * but when we do clear the inputs FF isn't exactly aware of this, and attempts to load data cause errors.
	 * <br>
	 * This is cursed, and should be replaced at the first opportunity.
	 *
	 * @param name
	 * 		Class to fetch.
	 */
	private void prefetchMissingClass(String name) {
		if (!hasClassCache.containsKey(name)) {
			boolean has = structContext.hasClass(name);
			if (!has && currentWorkspace != null) {
				ClassInfo info = currentWorkspace.getResources().getClass(name);
				try {
					addClassToStructCtx(info, false);
				} catch (Exception ignored) {
					// FF warns about a lot of classes, and seems to 'guess' about the location of some of them.
					// This becomes very noisy, so we just ignore this.
				}
			}
			hasClassCache.put(name, has);
		}
	}

	/**
	 * @param info
	 * 		Class to add to the FF context for decompilation.
	 * @param includeFlag
	 * 		Flag to include class in decompilation, vs for just usage as a reference.
	 *
	 * @throws IOException
	 * 		When FF could not parse the class.
	 */
	private void addClassToStructCtx(ClassInfo info, boolean includeFlag) throws IOException {
		String name = info.getName();
		// FF wants some odd naming convention where '.class' suffix always exists
		String qualified = (name + ".class");
		byte[] code = applyInterceptors(info.getValue());
		structContext.addData(name, qualified, code, includeFlag);
	}

	/**
	 * So, if we do not clear the cache FF seemingly wants to re-decompile everything
	 */
	private void populateClearableCache() {
		// We need to be able to clear cached inputs
		try {
			for (String key : new String[]{"units", "classes", "ownClasses"}) {
				Map<?, ?> map = ReflectUtil.quietGet(structContext, ReflectUtil.getDeclaredField(StructContext.class, key));
				if (map != null)
					inputCaches.add(map);
				else
					logger.error("Failed to find input map in StructContext: {}", key);
			}
		} catch (Exception ex) {
			logger.error("Failed to access QuiltFlower's context inputs!", ex);
		}
	}
}
