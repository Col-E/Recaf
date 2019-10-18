package me.coley.recaf.ui.controls.tree;

import javafx.scene.Group;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;

import java.util.Arrays;

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
	public Group createGraphic() {
		Group g = new Group();
		String ext = getLocalName().toLowerCase();
		if(ext.contains(".")) {
			ext = ext.substring(ext.lastIndexOf(".") + 1);
			if(Arrays.asList("txt", "mf", "properties").contains(ext))
				g.getChildren().add(new IconView("icons/text.png"));
			else if(Arrays.asList("json", "xml", "html", "css", "js").contains(ext))
				g.getChildren().add(new IconView("icons/text-code.png"));
			else if(Arrays.asList("png", "gif", "jpeg", "jpg", "bmp").contains(ext))
				g.getChildren().add(new IconView("icons/image.png"));
		}
		if(g.getChildren().isEmpty())
			g.getChildren().add(new IconView("icons/binary.png"));
		return g;
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