package software.coley.recaf.workspace.processors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.ThrowableProperty;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.WorkspaceProcessor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Workspace processor that marks {@link ClassInfo} values that inherit from {@link Throwable}
 * as having a {@link ThrowableProperty}. This allows instant look-ups for if a class is throwable,
 * by bypassing repeated calls to {@link InheritanceGraph}.
 *
 * @author Matt Coley
 */
@WorkspaceScoped
public class ThrowablePropertyAssigningProcessor implements WorkspaceProcessor, ResourceJvmClassListener, ResourceAndroidClassListener {
	private static final String THROWABLE = "java/lang/Throwable";
	private final InheritanceGraph graph;

	@Inject
	public ThrowablePropertyAssigningProcessor(InheritanceGraph graph) {
		this.graph = graph;
	}

	@Override
	public void onWorkspaceOpened(@Nonnull Workspace workspace) {
		graph.getVertex(THROWABLE).allChildren().forEach(vertex -> {
			ClassInfo classInfo = vertex.getValue();
			ThrowableProperty.set(classInfo);
		});

		// Ensure future changes to workspace also get updated.
		ThrowablePropertyAssigningProcessor processor = this;
		workspace.addWorkspaceModificationListener(new WorkspaceModificationListener() {
			@Override
			public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				library.addListener(processor);
			}

			@Override
			public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				library.removeListener(processor);
			}
		});
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			resource.addListener(processor);
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Mark throwable types";
	}

	private void handle(@Nonnull ClassInfo cls) {
		InheritanceVertex vertex = graph.getVertex(cls.getName());
		if (vertex != null && vertex.hasParent(THROWABLE))
			ThrowableProperty.set(cls);
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource,
						   @Nonnull AndroidClassBundle bundle,
						   @Nonnull AndroidClassInfo cls) {
		handle(cls);
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource,
							  @Nonnull AndroidClassBundle bundle,
							  @Nonnull AndroidClassInfo oldCls,
							  @Nonnull AndroidClassInfo newCls) {
		handle(newCls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource,
							  @Nonnull AndroidClassBundle bundle,
							  @Nonnull AndroidClassInfo cls) {
		// no-op
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource,
						   @Nonnull JvmClassBundle bundle,
						   @Nonnull JvmClassInfo cls) {
		handle(cls);
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource,
							  @Nonnull JvmClassBundle bundle,
							  @Nonnull JvmClassInfo oldCls,
							  @Nonnull JvmClassInfo newCls) {
		handle(newCls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource,
							  @Nonnull JvmClassBundle bundle,
							  @Nonnull JvmClassInfo cls) {
		// no-op
	}
}
