package me.coley.recaf.decompile.mapleir;

import me.coley.recaf.decompile.DecompileInterceptor;
import me.coley.recaf.workspace.Workspace;
import org.clyze.jphantom.util.FailableClassReader;
import org.mapleir.DefaultInvocationResolver;
import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassSource;
import org.mapleir.app.service.LocateableClassNode;
import org.mapleir.asm.ClassHelper;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

/**
 * MapleIR class source. Provides access to workspace clases.
 *
 * @author cts
 * @author bibl
 * @author Ghast
 */
public class MapleClassSource extends ApplicationClassSource {
	private final Workspace workspace;
	private final AnalysisContext context;

	public MapleClassSource(Workspace workspace) {
		super("phantom", new HashMap<>());
		this.workspace = workspace;

		IRCache irFactory = new IRCache(ControlFlowGraphBuilder::build);
		context = new BasicAnalysisContext.BasicContextBuilder()
				.setApplication(this)
				.setInvocationResolver(new DefaultInvocationResolver(this))
				.setCache(irFactory)
				.setApplicationContext(new SimpleApplicationContext(this))
				.setDataFlowAnalysis(new LiveDataFlowAnalysisImpl(irFactory))
				.build();
	}

	@Override
	public LocateableClassNode findClass(String inputPath) {
		final byte[] code = workspace.getResources().getClass(inputPath).getValue();
		final ClassReader reader = new FailableClassReader(code);

		final org.objectweb.asm.tree.ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.EXPAND_FRAMES);

		return new LocateableClassNode(this, ClassHelper.create(node), false);
	}

	/**
	 * @return Associated workspace of the class source.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * @return Associated MapleIR analysis cache.
	 */
	public AnalysisContext getContext() {
		return context;
	}
}
