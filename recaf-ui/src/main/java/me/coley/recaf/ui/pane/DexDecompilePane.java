package me.coley.recaf.ui.pane;

import com.google.common.base.Strings;
import jadx.api.JadxArgs;
import jadx.api.impl.NoOpCodeCache;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.decompile.jadx.dexcompat.DexClassData;
import me.coley.recaf.ui.behavior.SaveResult;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Decompiler UI for {@link me.coley.recaf.code.DexClassInfo}.
 *
 * @author Matt Coley
 * @see DecompilePane Standard Java alternative.
 */
public class DexDecompilePane extends CommonDecompilePane {
	/**
	 * Create and set up the panel.
	 */
	public DexDecompilePane() {
		super();
	}


	@Override
	protected Supplier<String> supplyDecompile() {
		return () -> {
			DexClassInfo cls = (DexClassInfo) getCurrentClassInfo();
			String name = cls.getName();
			// JadX setup
			JadxArgs args = new JadxArgs();
			args.setShowInconsistentCode(true);
			args.setCodeCache(new NoOpCodeCache());
			RootNode root = new RootNode(args);
			root.initClassPath();
			root.initPasses();
			// Load input
			ILoadResult result = new ILoadResult() {
				@Override
				public void visitClasses(Consumer<IClassData> consumer) {
					// TODO: Mapping from one dex input format to another is... a lot of work.
					//    Using Jadx's dex and smali input plugins are not compatible with our in-memory design.
					//    At this point is it more worth-while to simply investigate in DEX -> Java bytecode mapping?
					//    instead of mapping only into a single supported decompiler (JadX)?
					consumer.accept(new DexClassData(cls.getClassDef()));
				}

				@Override
				public void visitResources(Consumer<IResourceData> consumer) {

				}

				@Override
				public boolean isEmpty() {
					return false;
				}

				@Override
				public void close() {
					// no-op
				}
			};
			root.loadClasses(Collections.singletonList(result));
			root.runPreDecompileStage();
			// Find and return decompilation if found
			ClassNode clazz = root.resolveClass(name.replace('/', '.'));
			if (clazz != null) {
				String decompiled = clazz.decompile().getCodeStr();
				if (Strings.isNullOrEmpty(decompiled))
					return "// Jadx failed to decompile: " + name + "\n/*\n" +
							clazz.getDisassembledCode() +
							"\n*/";
				return decompiled;
			}
			return "// Jadx failed to generate model for: " + name;
		};
	}

	@Override
	protected boolean checkDecompilePreconditions() {
		// No conditions needed
		return true;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public SaveResult save() {
		// Android decompilation is read-only
		return SaveResult.IGNORED;
	}
}
