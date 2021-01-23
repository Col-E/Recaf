package me.coley.recaf.compile.javac;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;

/**
 * File manager extension for handling updates to java file object's output stream.
 * Additionally, registers inner classes as new files.
 *
 * @author Matt Coley
 */
public class VirtualFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final VirtualUnitMap unitMap;

	/**
	 * @param unitMap
	 * 		Class input map.
	 * @param fallback
	 * 		Fallback manager.
	 */
	public VirtualFileManager(VirtualUnitMap unitMap, JavaFileManager fallback) {
		super(fallback);
		this.unitMap = unitMap;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String name, JavaFileObject.Kind
			kind, FileObject sibling) throws IOException {
		String internal = name.replace('.', '/');
		VirtualJavaFileObject obj = unitMap.getFile(internal);
		// Unknown class, assumed to be an inner class.
		// add to the unit map so it can be fetched.
		if (obj == null) {
			// TODO: Double check that it uses "Outer$Inner" pattern instead of "Outer.Inner"
			obj = new VirtualJavaFileObject(internal, null);
			unitMap.addFile(internal, obj);
		}
		return obj;
	}
}