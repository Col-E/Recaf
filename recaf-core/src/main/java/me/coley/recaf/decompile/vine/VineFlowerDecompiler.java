package me.coley.recaf.decompile.vine;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.InnerClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * VineFlower decompiler implementation.
 *
 * @author Matt Coley
 */
public class VineFlowerDecompiler extends Decompiler {
	private static final Logger logger = Logging.get(VineFlowerDecompiler.class);
	private final IFernflowerLogger ffLogger = new VineLogger();
	private final IResultSaver noOpSaver = new NoOpSaver();
	private final Map<String, Object> fernFlowerProperties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
	private final ThreadLocal<String> target = new ThreadLocal<>();
	private final ThreadLocal<String> result = new ThreadLocal<>();

	public VineFlowerDecompiler() {
		super("VineFlower", "1.9.3");
		fernFlowerProperties.put("ind", "    ");
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		try {
			// TODO: For batch decompile, do not call this multiple times.
			//  Create an alternative version that passes the full workspace source with 'addSource'
			//  then record outputs
			target.set(classInfo.getName());
			Fernflower fernflower = new Fernflower(noOpSaver, fernFlowerProperties, ffLogger);
			fernflower.addSource(new SingleContextSource(workspace, classInfo));
			fernflower.addLibrary(new FullContextSource(workspace));
			fernflower.decompileContext();
			fernflower.clearContext();
			String decompiled = result.get();
			if (decompiled == null)
				return "// Failed to decompile: " + classInfo.getName();
			return decompiled;
		} catch (Exception e) {
			logger.error("VineFlower encountered an error when decompiling", e);
			return "// " + StringUtil.traceToString(e).replace("\n", "\n// ");
		} finally {
			target.set(null);
			result.set(null);
		}
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		// TODO: When we update the decompile-options, we should update 'fernFlowerProperties'
		return new HashMap<>();
	}

	private class SingleContextSource extends BaseContextSource {
		private final ClassInfo info;

		protected SingleContextSource(Workspace workspace, ClassInfo info) {
			super(workspace);
			this.info = info;
		}

		@Override
		public Entries getEntries() {
			// TODO: Bug in QF/VF makes it so that 'addLibrary' doesn't yield inner info for a class provided with 'addSource'
			//  So for now until this is fixed upstream we will also supply inners here.
			//  This will make QF/VF decompile each inner class separately as well, but its the best fix for now without
			//  too much of a perf hit.
			List<Entry> entries = new ArrayList<>();
			entries.add(new Entry(info.getName(), Entry.BASE_VERSION));
			for (InnerClassInfo innerClass : info.getInnerClasses())
				entries.add(new Entry(innerClass.getName(), Entry.BASE_VERSION));
			return new Entries(entries, Collections.emptyList(), Collections.emptyList());
		}
	}

	private class FullContextSource extends BaseContextSource {
		protected FullContextSource(Workspace workspace) {
			super(workspace);
		}

		@Override
		public Entries getEntries() {
			List<Entry> entries = workspace.getResources().getClasses()
					.map(c -> new Entry(c.getName(), Entry.BASE_VERSION))
					.collect(Collectors.toList());
			return new Entries(entries, Collections.emptyList(), Collections.emptyList());
		}
	}

	private abstract class BaseContextSource implements IContextSource {
		protected final Workspace workspace;

		protected BaseContextSource(Workspace workspace) {
			this.workspace = workspace;
		}

		@Override
		public String getName() {
			return "recaf";
		}

		@Override
		public InputStream getInputStream(String resource) {
			String name = resource.substring(0, resource.length() - IContextSource.CLASS_SUFFIX.length());
			ClassInfo info = workspace.getResources().getClass(name);
			if (info == null) return InputStream.nullInputStream();
			return new ByteArrayInputStream(applyPreInterceptors(info.getValue()));
		}

		@Override
		public IOutputSink createOutputSink(IResultSaver saver) {
			return new IOutputSink() {
				@Override
				public void begin() {
					// no-op
				}

				@Override
				public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
					if (target.get().equals(qualifiedName))
						result.set(content);
				}

				@Override
				public void acceptDirectory(String directory) {
					// no-op
				}

				@Override
				public void acceptOther(String path) {
					// no-op
				}

				@Override
				public void close() {
					// no-op
				}
			};
		}
	}

	/**
	 * We save results via {@link BaseContextSource#createOutputSink(IResultSaver)} so this can remain empty.
	 */
	private static class NoOpSaver implements IResultSaver {
		@Override
		public void saveFolder(String path) {
			// no-op
		}

		@Override
		public void copyFile(String source, String path, String entryName) {
			// no-op
		}

		@Override
		public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
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

	private static class VineLogger extends IFernflowerLogger {
		@Override
		public void writeMessage(String message, Severity severity) {
			logger.trace("VineFlower: {}", message);
		}

		@Override
		public void writeMessage(String message, Severity severity, Throwable t) {
			if (t instanceof ThreadDeath) {
				// VineFlower catches all throwable types
				// We must propagate thread death ourselves
				ReflectUtil.propagate(t);
			}
			logger.error("VineFlower: {}", message, t);
		}
	}
}