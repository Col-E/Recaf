package software.coley.recaf.services.workspace.processors;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.ThrowableProperty;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.workspace.WorkspaceProcessor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Objects;

/**
 * Workspace processor that marks {@link ClassInfo} values that inherit from {@link Throwable}
 * as having a {@link ThrowableProperty}. This allows instant look-ups for if a class is throwable,
 * by bypassing repeated calls to {@link InheritanceGraph}.
 *
 * @author Matt Coley
 */
@Dependent
public class ThrowablePropertyAssigningProcessor implements WorkspaceProcessor, ResourceJvmClassListener, ResourceAndroidClassListener {
	private static final String THROWABLE = "java/lang/Throwable";
	private final InheritanceGraph inheritanceGraph;

	@Inject
	public ThrowablePropertyAssigningProcessor(@Nonnull InheritanceGraphService graphService) {
		this.inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
	}

	@Override
	public void processWorkspace(@Nonnull Workspace workspace) {
		inheritanceGraph.getVertex(THROWABLE).allChildren().forEach(vertex -> {
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
		InheritanceVertex vertex = inheritanceGraph.getVertex(cls.getName());
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
