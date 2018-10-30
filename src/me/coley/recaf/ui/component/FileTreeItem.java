package me.coley.recaf.ui.component;

import java.util.*;
import java.util.function.Supplier;

import javafx.scene.control.*;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.*;

/**
 * Wrapper for TreeItem children set. Allows more file-system-like access.
 * 
 * @author Matt
 */
public class FileTreeItem extends TreeItem<String> implements Comparable<String> {
	// Split in case of cases like:
	// a/a/a.class
	// a/a/a/a.class
	private final Map<String, FileTreeItem> dirs = new TreeMap<>();
	private final Map<String, FileTreeItem> files = new TreeMap<>();
	private final Supplier<ClassNode> file;
	private final boolean isDir;

	// unused 'i' for differing constructors
	public FileTreeItem(Input input, String name, int i) {
		this.file = () -> input.getClass(name);
		setValue(name);
		isDir = false;
		get();
	}

	public FileTreeItem(String name) {
		this.file = () -> null;
		setValue(name);
		isDir = true;
	}

	public ClassNode get() {
		return file.get();
	}

	public FileTreeItem addDir(String name) {
		FileTreeItem fti = new FileTreeItem(name);
		dirs.put(name, fti);
		addOrdered(fti);
		return fti;
	}

	public void addFile(Input input, String part, String name) {
		FileTreeItem fti = new FileTreeItem(input, name, 0);
		files.put(part, fti);
		addOrdered(fti);
	}

	public void addOrdered(FileTreeItem fti) {
		try {
			int sizeD = dirs.size();
			int sizeF = files.size();
			int size = sizeD + sizeF;
			if (size == 0) {
				getChildren().add(fti);
				return;
			}
			if (fti.isDir) {
				FileTreeItem[] array = dirs.values().toArray(new FileTreeItem[0]);
				int index = Arrays.binarySearch(array, fti.getValue());
				if (index < 0) {
					index = (index * -1) - 1;
				}
				getChildren().add(index, fti);
			} else {
				FileTreeItem[] array = files.values().toArray(new FileTreeItem[0]);
				int index = Arrays.binarySearch(array, fti.getValue());
				if (index < 0) {
					index = (index * -1) - 1;
				}
				getChildren().add(sizeD + index, fti);
			}
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}

	public void remove(FileTreeItem item) {
		String name = Misc.trim(item.getValue(), "/");
		if (item.isDir) {
			dirs.remove(name);
		} else {
			files.remove(name);
		}
		getChildren().remove(item);
	}

	public FileTreeItem getDir(String name) {
		return dirs.get(name);
	}

	public boolean hasDir(String name) {
		return dirs.containsKey(name);
	}

	public FileTreeItem getFile(String name) {
		return files.get(name);
	}

	@Override
	public int compareTo(String s) {
		return getValue().compareTo(s);
	}

	public boolean isDirectory() {
		return isDir;
	}

	public Map<String, FileTreeItem> getDirectories() {
		return dirs;
	}

	public Map<String, FileTreeItem> getFiles() {
		return files;
	}
}