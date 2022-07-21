package me.coley.recaf.assemble.util;

import java.util.List;

public interface ProgramClass {

	String getName();
	String getSuperName();

	<T extends ProgramField> List<T> getFields();
	<T extends ProgramMethod> List<T> getMethods();

}
