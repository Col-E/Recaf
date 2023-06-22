package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Java file extension that keeps track of the compiled bytecode.
 *
 * @author Matt Coley
 */
public class VirtualJavaFileObject extends SimpleJavaFileObject {
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final String content;

	/**
	 * @param className
	 * 		Name of class.
	 * @param content
	 * 		Content of source file to compile.
	 */
	public VirtualJavaFileObject(String className, String content) {
		super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
				Kind.SOURCE);
		this.content = content;
	}

	/**
	 * @return {@code true} when {@link #getBytecode()} has content.
	 */
	public boolean hasOutput() {
		return baos.size() > 0;
	}

	/**
	 * @return Compiled bytecode of class.
	 */
	@Nonnull
	public byte[] getBytecode() {
		return baos.toByteArray();
	}

	/**
	 * @return Class source code.
	 */
	@Nonnull
	public String getSource() {
		return content;
	}

	@Override
	public final OutputStream openOutputStream() {
		return baos;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return content;
	}
}