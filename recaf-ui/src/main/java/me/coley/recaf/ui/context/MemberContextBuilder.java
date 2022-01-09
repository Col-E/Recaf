package me.coley.recaf.ui.context;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
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
