package me.coley.recaf.ui.controls.tree;

import javafx.scene.Node;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.workspace.JavaResource;

import static me.coley.recaf.util.UiUtil.getFileIcon;

/**
 * Item to represent resources.
 *
 * @author Matt
 */
public class ResourceItem extends DirectoryItem {
	private final String name;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local item name.
	 * @param name
	 * 		Full resource name.
	 */
	public ResourceItem(JavaResource resource, String local, String name) {
		super(resource, local);
		this.name = name;
	}

	/**
	 * @return Contained resource name.
	 */
	public String getResourceName() {
		return name;
	}

	/**
	 * @return Path to icon based on file extension.
	 */
	public Node createGraphic() {
		return new IconView(getFileIcon(getLocalName()));
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof ResourceItem) {
			ResourceItem c = (ResourceItem) o;
			return name.compareTo(c.name);
		}
		return 1;
	}
}