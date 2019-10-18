package me.coley.recaf.ui.controls.tree;

import javafx.scene.Group;
import javafx.scene.Node;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;

/**
 * Item to represent classes.
 *
 * @author Matt
 */
public class ClassItem extends DirectoryItem {
	private final String name;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local item name.
	 * @param name
	 * 		Full class name.
	 */
	public ClassItem(JavaResource resource, String local, String name) {
		super(resource, local);
		this.name = name;
	}

	/**
	 * @return Contained class name.
	 */
	public String getClassName() {
		return name;
	}

	/**
	 * @return Path to icon based on class type.
	 */
	public Node createGraphic() {
		Group g = new Group();
		//
		int access = new ClassReader(resource().getClasses().get(name)).getAccess();
		String base = "icons/class/class.png";
		if(AccessFlag.isEnum(access))
			base = "icons/class/enum.png";
		else if(AccessFlag.isAnnotation(access))
			base = "icons/class/annotation.png";
		else if(AccessFlag.isInterface(access))
			base = "icons/class/interface.png";
		g.getChildren().add(new IconView(base));
		//
		if(AccessFlag.isFinal(access) && !AccessFlag.isEnum(access))
			g.getChildren().add(new IconView("icons/modifier/final.png"));
		if(AccessFlag.isAbstract(access) && !AccessFlag.isInterface(access))
			g.getChildren().add(new IconView("icons/modifier/abstract.png"));
		if(AccessFlag.isBridge(access) || AccessFlag.isSynthetic(access))
			g.getChildren().add(new IconView("icons/modifier/synthetic.png"));
		//
		return g;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof ClassItem) {
			ClassItem c = (ClassItem) o;
			return name.compareTo(c.name);
		}
		return 1;
	}
}