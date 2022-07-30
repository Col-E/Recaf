package me.coley.recaf.decompile.jadx;

import jadx.api.JadxArgs;
import jadx.api.impl.NoOpCodeCache;
import jadx.core.Jadx;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.JavaLoadResult;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.constant.CpClass;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jadx decompiler implementation.
 *
 * @author Matt Coley
 */
public class JadxDecompiler extends Decompiler {
	private static final Logger logger = Logging.get(JadxDecompiler.class);
	private final JadxArgs args = createDefaultArgs();
	private final RootNode root = new RootNode(args);
	private final Object lock = new Object();
	private volatile boolean initialized;

	/**
	 * New Jadx decompiler instance.
	 */
	public JadxDecompiler() {
		super("Jadx", Jadx.getVersion());
		ThreadUtil.run(() -> {
			root.initClassPath();
			root.initPasses();
			synchronized (lock) {
				initialized = true;
				lock.notifyAll();
			}
		});
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		if (!initialized) {
			synchronized (lock) {
				while (!initialized) {
					try {
						lock.wait();
					} catch (InterruptedException ignored) {
						Thread.currentThread().interrupt();
						return null;
					}
				}
			}
		}
		String name = classInfo.getName();
		// Remove old data
		clearOldClassList();

		// Populate inputs
		List<JavaClassReader> readers = new ArrayList<>();
		addTargetClass(readers, classInfo);
		addReferencedClasses(readers, workspace, classInfo);

		// Generate result and load the data into Jadx
		JavaLoadResult result = new JavaLoadResult(readers, null);
		root.loadClasses(Collections.singletonList(result));
		root.runPreDecompileStage();

		// Find and return decompilation if found
		ClassNode clazz = root.resolveClass(name.replace('/', '.'));
		if (clazz != null) {
			String decompiled = clazz.decompile().getCodeStr();
			if (StringUtil.isNullOrEmpty(decompiled))
				return "// Jadx failed to decompile: " + name + "\n/*\n" +
						clazz.getDisassembledCode() +
						"\n*/";
			return decompiled;
		}
		return "// Jadx failed to generate model for: " + name;
	}

	private void clearOldClassList() {
		try {
			List<?> classList = ReflectUtil.quietGet(root, RootNode.class.getDeclaredField("classes"));
			classList.clear();
		} catch (Exception e) {
			logger.error("Failed to clear old class list");
		}
	}

	private void addTargetClass(List<JavaClassReader> readers, ClassInfo classInfo) {
		int id = classInfo.getName().hashCode();
		byte[] code = applyPreInterceptors(classInfo.getValue());
		readers.add(new JavaClassReader(id, classInfo.getName(), code));
	}

	private void addReferencedClasses(List<JavaClassReader> readers, Workspace workspace, ClassInfo classInfo) {
		if (workspace == null)
			return;
		try {
			ClassFileReader cfr = new ClassFileReader();
			ClassFile classFile = cfr.read(classInfo.getValue());
			classFile.getPool().stream()
					.filter(cp -> cp instanceof CpClass)
					.map(cp -> classFile.getPool().getUtf(((CpClass) cp).getIndex()))
					.forEach(referencedName -> {
						// Skip target class
						if (referencedName.equals(classFile.getName()))
							return;
						// Add referenced class if available
						ClassInfo refClass = workspace.getResources().getClass(referencedName);
						if (refClass != null) {
							int id = referencedName.hashCode();
							byte[] code = applyPreInterceptors(refClass.getValue());
							readers.add(new JavaClassReader(id, referencedName, code));
						}
					});
		} catch (InvalidClassException e) {
			logger.error("Failed to populate referenced classes", e);
		}
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		// TODO: Utilize Jadx arguments
		return Collections.emptyMap();
	}

	private static JadxArgs createDefaultArgs() {
		JadxArgs args = new JadxArgs();
		// Flag decompiles code even if the semantics are not 100% correct.
		// Without it, the output yields disassembly instead.
		args.setShowInconsistentCode(true);
		// We do not want code to be cached. This prevents our edits from appearing.
		args.setCodeCache(new NoOpCodeCache());
		return args;
	}
}
