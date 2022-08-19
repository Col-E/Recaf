package me.coley.recaf.code;

public class InnerClassInfo {
	private final String className;
	private final String name;
	private final String outerName;
	private final String innerName;
	private final int access;

	public InnerClassInfo(String className, String name, String outerName, String innerName, int access) {
		this.className = className;
		this.name = name;
		this.outerName = outerName;
		this.innerName = innerName;
		this.access = access;
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name;
	}

	public String getOuterName() {
		return outerName;
	}

	public String getInnerName() {
		return innerName;
	}

	public int getAccess() {
		return access;
	}
}
