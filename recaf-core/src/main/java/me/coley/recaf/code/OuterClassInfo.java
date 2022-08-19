package me.coley.recaf.code;

public class OuterClassInfo {
	private final String className;
	private final String owner;
	private final String name;
	private final String descriptor;

	public OuterClassInfo(String className, String owner, String name, String descriptor) {
		this.className = className;
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	public String getClassName() {
		return className;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}
}
