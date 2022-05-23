package me.coley.recaf.ui.context;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Base context menu builder for methods and fields.
 *
 * @author Matt Coley
 */
public abstract class MemberContextBuilder extends DeclarableContextBuilder {
	/**
	 * @return Type defining the member.
	 */
	public abstract CommonClassInfo getOwnerInfo();

	/**
	 * @param info
	 * 		Class information about selected item's defining class.
	 *
	 * @return Builder.
	 */
	public abstract MemberContextBuilder setOwnerInfo(CommonClassInfo info);

	/**
	 * @return {@code true} when the containing class is a {@link DexClassInfo}.
	 */
	protected boolean isOwnerDexClass() {
		return getOwnerInfo() instanceof DexClassInfo;
	}

	/**
	 * @return {@code true} when the containing class is a {@link ClassInfo}.
	 */
	protected boolean isOwnerJvmClass() {
		return getOwnerInfo() instanceof ClassInfo;
	}

	@Override
	public Resource findContainerResource() {
		String name = getOwnerInfo().getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForClass(name);
		if (resource == null)
			resource = workspace.getResources().getContainingForDexClass(name);
		if (resource == null)
			logger.warn("Could not find container resource for class {}", name);
		return resource;
	}
}
