package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.recaf.ui.control.hex.HexView;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the class-file content of a given inclusive range <i>({@link #getStart()} - {@link #getEnd()})</i>.
 *
 * @author Matt Coley
 */
public class ClassOffsetInfo {
	private final ClassFile cf;
	private final ClassOffsetInfoType type;
	private final Object value;
	// Range
	private final int start;
	private final int end;
	// Nesting
	private final List<ClassOffsetInfo> children = new ArrayList<>();
	private ClassOffsetInfo parent;

	/**
	 * @param cf
	 * 		Target class file,
	 * @param type
	 * 		Type of info.
	 * @param value
	 * 		Value of type in the class.
	 * @param start
	 * 		Offset range start <i>(inclusive)</i>.
	 * @param end
	 * 		Offset range end <i>(inclusive)</i>.
	 */
	public ClassOffsetInfo(ClassFile cf, ClassOffsetInfoType type, Object value, int start, int end) {
		this.cf = cf;
		this.type = type;
		this.value = value;
		this.start = start;
		this.end = end;
	}

	/**
	 * @param parent
	 * 		Parent to assign to current info.
	 */
	public void setParent(ClassOffsetInfo parent) {
		this.parent = parent;
		if (!parent.children.contains(this))
			parent.children.add(this);
	}

	/**
	 * @return Target class file.
	 */
	public ClassFile getClassFile() {
		return cf;
	}

	/**
	 * @return Type of info.
	 */
	public ClassOffsetInfoType getType() {
		return type;
	}

	/**
	 * @return Value of type in the class.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @return Offset range start <i>(inclusive)</i>.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return Offset range end <i>(inclusive)</i>.
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @return Child offsets <i>(Further breakdown of current range)</i>.
	 */
	public List<ClassOffsetInfo> getChildren() {
		return children;
	}

	/**
	 * @return Parent info.
	 */
	public ClassOffsetInfo getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "ClassOffsetInfo{" +
				"type=" + type.name() +
				", start=" + HexView.offsetStr(start) +
				", end=" + HexView.offsetStr(end) +
				'}';
	}
}
