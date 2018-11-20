package me.coley.recaf.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Asm;
import me.coley.recaf.config.impl.ConfCFR;

public class CFRPipeline {
	private final ClassNode cn;
	private final MethodNode mn;

	public CFRPipeline(ClassNode cn) {
		this(cn, null);
	}

	public CFRPipeline(ClassNode cn, MethodNode mn) {
		this.cn = cn;
		this.mn = mn;
	}

	public String decompile() {
		// Get options to use for decompilation
		Map<String, String> options = ConfCFR.instance().toStringMap();
		if (mn != null) {
			// If a methodnode is provided, only decompile the method
			options.put("methodname", mn.name);
		}
		// Setup driver
		Lookup lookup = new Lookup(cn);
		SourceInput source = new SourceInput(lookup);
		SinkFactory sink = new SinkFactory();
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(source)
				.withOutputSink(sink)
				.withOptions(options)
				.build();
		// Decompile
		driver.analyse(Collections.singletonList(cn.name));
		String decompilation = sink.getDecompilation();
		// Cut out watermark (Option to do so still includes it?)
		if (decompilation.startsWith("/")) {
			decompilation = decompilation.substring(decompilation.indexOf("*/") + 3);
		}
		return decompilation;
	}

	public String getTitlePostfix() {
		return mn == null ? cn.name : mn.name;
	}

	private static class SinkFactory implements OutputSinkFactory {
		private String decompile = "Failed to get CFR output";

		@Override
		public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
			return Arrays.asList(SinkClass.STRING);
		}

		@Override
		public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
			switch (sinkType) {
			case EXCEPTION:
				return sinkable -> {
					Logging.error("CFR: " + sinkable);
				};
			case JAVA:
				return sinkable -> {
					decompile = sinkable.toString();
				};
			case PROGRESS:
				return sinkable -> {
					Logging.info("CFR: " + sinkable);
				};
			default:
				break;
			}
			return ignore -> {};
		}

		public String getDecompilation() {
			return decompile;
		}
	};

	private static class SourceInput implements ClassFileSource {
		/**
		 * Lookup assistor for inner classes and other references.
		 */
		private final Lookup resources;

		private SourceInput(Lookup resources) {
			this.resources = resources;
		}

		@Override
		public void informAnalysisRelativePathDetail(String s, String s1) {}

		@Override
		public Collection<String> addJar(String s) {
			throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
		}

		@Override
		public String getPossiblyRenamedPath(String s) {
			return s;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String pathOrName) throws IOException {
			pathOrName = pathOrName.substring(0, pathOrName.length() - ".class".length());
			return Pair.make(resources.get(pathOrName), pathOrName);
		}
	}

	/**
	 * Lookup helper for CFR since it requests extra data <i>(Other classes)</i>
	 * for more accurate decompilation.
	 * 
	 * @author Matt
	 */
	private static class Lookup {
		private final ClassNode target;

		private Lookup(ClassNode target) {
			this.target = target;
		}

		public byte[] get(String path) {
			// Load target node from instance.
			if (target != null && path.equals(target.name)) {
				try {
					return Asm.getBytes(target);
				} catch (Exception e) {
					Logging.error(e);
				}
			}
			// Try to load other classes from the virtual file system.
			try {
				return Input.get().getFile(path);
			} catch (IOException e) {}
			// Try to load them from memory.
			
			try {
				Class<?> clazz = Class.forName(path.replace("/", "."), false, ClassLoader.getSystemClassLoader());
				ClassNode node = Asm.getNode(clazz);
				return Asm.getBytes(node);
			} catch (Exception e) {}
			// Failed to fetch class.
			Logging.fine("CFR: Decompile 'get' failed: " + path);
			return null;
		}

	}

}
