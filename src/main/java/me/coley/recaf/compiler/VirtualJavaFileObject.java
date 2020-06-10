package me.coley.recaf.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;

/**
 * Java file extension that keeps track of the compiled bytecode.
 *
 * @author Matt
 */
public class VirtualJavaFileObject extends SimpleJavaFileObject {
	/**
	 * Output to contain compiled bytcode.
	 */
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	/**
	 * Content of source file to compile.
	 */
	private final String content;

	/**
	 * @param className
	 * 		Name of class.
	 * @param content
	 * 		Class source content.
	 */
	public VirtualJavaFileObject(String className, String content) {
		super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
				Kind.SOURCE);
		this.content = content;
	}

	/**
	 * @return Compiled bytecode of class.
	 */
	public byte[] getBytecode() {
		return baos.toByteArray();
	}

	/**
	 * @return Class source code.
	 */
	public String getSource() {
		return content;
	}

	@Override
	public final OutputStream openOutputStream() throws IOException {
		return baos;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return content;
	}
}