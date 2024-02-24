package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Base to work off-of to create {@link MenuBuilder} types.
 *
 * @author Matt Coley
 */
public class ContextMenuBuilder {
	private final ItemSink sink;
	private final ContextMenu menu;

	/**
	 * @param source
	 * 		Context source to pass to menu builders.
	 */
	public ContextMenuBuilder(@Nonnull ContextSource source) {
		this(new ContextMenu(), source);
	}

	/**
	 * @param menu
	 * 		Menu to append content to.
	 * @param source
	 * 		Context source to pass to menu builders.
	 */
	public ContextMenuBuilder(@Nonnull ContextMenu menu, @Nonnull ContextSource source) {
		this.menu = menu;
		sink = new ItemSink(menu.getItems(), source);
	}

	/**
	 * @return Menu target.
	 */
	@Nonnull
	public ContextMenu getMenu() {
		return menu;
	}

	/**
	 * @param workspace
	 * 		Target workspace.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public WorkspaceMenuBuilder
	forWorkspace(@Nonnull Workspace workspace) {
		return new WorkspaceMenuBuilder(null, sink, workspace);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Target resource.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public ResourceMenuBuilder
	forResource(@Nonnull Workspace workspace,
				@Nonnull WorkspaceResource resource) {
		return new ResourceMenuBuilder(null, sink, workspace, resource);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Target bundle.
	 * @param <B>
	 * 		Bundle type.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public <B extends Bundle<?>> BundleMenuBuilder<B>
	forBundle(@Nonnull Workspace workspace,
			  @Nonnull WorkspaceResource resource,
			  @Nonnull B bundle) {
		return new BundleMenuBuilder<>(null, sink, workspace, resource, bundle);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Target directory path.
	 * @param <B>
	 * 		Bundle type.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public <B extends Bundle<?>> DirectoryMenuBuilder<B>
	forDirectory(@Nonnull Workspace workspace,
				 @Nonnull WorkspaceResource resource,
				 @Nonnull B bundle,
				 @Nonnull String directoryName) {
		return new DirectoryMenuBuilder<>(null, sink, workspace, resource, bundle, directoryName);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Target info object.
	 * @param <B>
	 * 		Bundle type.
	 * @param <I>
	 * 		Info type.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public <B extends Bundle<?>, I extends Info> InfoMenuBuilder<B, I>
	forInfo(@Nonnull Workspace workspace,
			@Nonnull WorkspaceResource resource,
			@Nonnull B bundle,
			@Nonnull I info) {
		return new InfoMenuBuilder<>(null, sink, workspace, resource, bundle, info);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Declaring class info object.
	 * @param member
	 * 		Target member.
	 * @param <B>
	 * 		Bundle type.
	 * @param <I>
	 * 		Class info type.
	 * @param <M>
	 * 		Member type.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public <B extends Bundle<?>, I extends ClassInfo, M extends ClassMember> MemberMenuBuilder<B, I, M>
	forMember(@Nonnull Workspace workspace,
			  @Nonnull WorkspaceResource resource,
			  @Nonnull B bundle,
			  @Nonnull I info,
			  @Nonnull M member) {
		return new MemberMenuBuilder<>(null, sink, workspace, resource, bundle, info, member);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		Item declared as the annotation target.
	 * @param annotation
	 * 		Target annotation.
	 *
	 * @return Menu builder.
	 */
	@Nonnull
	public AnnotationMenuBuilder
	forAnnotation(@Nonnull Workspace workspace,
				  @Nonnull WorkspaceResource resource,
				  @Nonnull ClassBundle<? extends ClassInfo> bundle,
				  @Nonnull Annotated annotated,
				  @Nonnull AnnotationInfo annotation) {
		return new AnnotationMenuBuilder(null, sink, workspace, resource, bundle, annotated, annotation);
	}
}
