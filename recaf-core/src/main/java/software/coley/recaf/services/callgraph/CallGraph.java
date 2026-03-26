package software.coley.recaf.services.callgraph;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.collect.MultiMap;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Represents method calls as a navigable graph.
 *
 * @author Amejonah
 * @author Matt Coley
 * @see MethodVertex
 */
public class CallGraph {
	private static final DebuggingLogger logger = Logging.get(CallGraph.class);
	private final ExecutorService threadPool = ThreadPoolFactory.newFixedThreadPool("call-graph", 1, true);
	private final ObservableBoolean isReady = new ObservableBoolean(false);
	private final CallGraphUpdater updater;
	private final ListenerHost listener = new ListenerHost();
	private final Workspace workspace;
	private boolean initialized;

	/**
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	public CallGraph(@Nonnull Workspace workspace) {
		this.workspace = workspace;
		updater = new CallGraphUpdater(workspace);
	}

	/**
	 * @return {@code true} when {@link #initialize()} has been called.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Observable boolean tracking the state of the call-graph's parsing of the current workspace.
	 */
	@Nonnull
	public ObservableBoolean isReady() {
		return isReady;
	}

	/**
	 * @param classInfo
	 * 		Class to wrap.
	 *
	 * @return Wrapper for easy {@link MethodVertex} management for the class.
	 */
	@Nonnull
	public ClassMethodsContainer getClassMethodsContainer(@Nonnull ClassInfo classInfo) {
		return updater.getClassMethodsContainer(classInfo);
	}

	/**
	 * @param method
	 * 		Method to get vertex of.
	 *
	 * @return Vertex of method. Can be {@code null} if the method member does not
	 * define its {@link MethodMember#getDeclaringClass() declaring class}.
	 */
	@Nullable
	public MethodVertex getVertex(@Nonnull MethodMember method) {
		return updater.getVertex(method);
	}

	/**
	 * Initialize the graph.
	 */
	public void initialize() {
		if (initialized) return;
		initialized = true;

		workspace.addWorkspaceModificationListener(listener);
		for (WorkspaceResource resource : workspace.getAllResources(false))
			resource.addListener(listener);

		CompletableFuture.runAsync(() -> {
			for (WorkspaceResource resource : workspace.getAllResources(false))
				visitResourceClasses(resource, updater::visitClass);
		}, threadPool).whenComplete((unused, t) -> {
			if (t == null) {
				isReady.setValue(true);
			} else {
				logger.error("Call graph initialization failed", t);
				isReady.setValue(false);
			}
		});
	}

	/**
	 * Visit all classes in the resource and its children, applying the consumer to each class.
	 *
	 * @param resource
	 * 		Resource to visit.
	 * @param consumer
	 * 		Visitor to apply to each class.
	 */
	private static void visitResourceClasses(@Nonnull WorkspaceResource resource,
	                                         @Nonnull Consumer<ClassInfo> consumer) {
		resource.classBundleStreamRecursive().forEach(bundle -> visitBundleClasses(bundle, consumer));
	}

	/**
	 * Visit all classes in the bundle, applying the consumer to each class.
	 *
	 * @param bundle
	 * 		Bundle to visit.
	 * @param consumer
	 * 		Visitor to apply to each class.
	 */
	private static void visitBundleClasses(@Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                       @Nonnull Consumer<ClassInfo> consumer) {
		for (ClassInfo classInfo : bundle.values())
			consumer.accept(classInfo);
	}

	/**
	 * @return Map of classes that could not be resolved, to method declarations observed being made to them.
	 */
	@Nonnull
	@VisibleForTesting
	MultiMap<String, MethodRef, ?> getUnresolvedDeclarations() {
		return updater.getUnresolvedDeclarations();
	}

	private class ListenerHost implements WorkspaceModificationListener, ResourceJvmClassListener, ResourceAndroidClassListener {
		@Override
		public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			library.addListener(this);
			visitResourceClasses(library, updater::visitClass);
		}

		@Override
		public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			visitResourceClasses(library, updater::removeClass);
			library.removeListener(this);
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			updater.visitClass(cls);
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                          @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
			updater.updateClass(oldCls, newCls);
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			updater.removeClass(cls);
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			updater.visitClass(cls);
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
		                          @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
			updater.updateClass(oldCls, newCls);
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			updater.removeClass(cls);
		}
	}
}
