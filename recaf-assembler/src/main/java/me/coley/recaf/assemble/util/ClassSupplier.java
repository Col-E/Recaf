package me.coley.recaf.assemble.util;

import javassist.CtClass;

import java.util.Iterator;

/**
 * Class supplier for expression compilation.
 *
 * @author Matt Coley
 */
public interface ClassSupplier {
	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Class bytecode.
	 *
	 * @throws ClassNotFoundException
	 * 		When the given class could not be found.
	 */
	byte[] getClass(String name) throws ClassNotFoundException;

	/**
	 * @param declaringClass
	 * 		Class the type is being used in.
	 * @param type
	 * 		Qualified class name. May not contain package separator chars (.) and imply a class
	 * 		that is supposed to be read from an import.
	 *
	 * @return Internal name of the target type.
	 */
	default String resolveFromImported(CtClass declaringClass, String type) {
		// Check if passed type is already internal format
		int internalPackageIndex = type.lastIndexOf('/');
		if (internalPackageIndex > 0)
			return type;
		// TODO: To support inner classes we'll need to do what we did in the
		//   javaparser type resolver, where we start replacing the last '/' with '$'
		//   until there is a match.
		int packageIndex = type.lastIndexOf('.');
		if (packageIndex == -1) {
			// TODO: In a workspace listener:
			//  - onLoad: register the packages to the default class pool + standard java ones
			//  - onClose: clear the packages

			// This works for now since all types must be fully expressed ("com.example.Foo" instead of "Foo")
			// Anything else thus must be a default import, which is the first imported package
			for (Iterator<String> it = declaringClass.getClassPool().getImportedPackages(); it.hasNext(); ) {
				String packageName = it.next();
				String tmp = packageName.replace('.', '/') + '/' + type.replace('.', '$');
				try {
					if (getClass(tmp) != null) {
						type = tmp;
						break;
					}
				} catch (ClassNotFoundException ignored) {
				}
			}
		} else {
			type = type.replace('.', '/');
		}
		return type;
	}
}
