package me.coley.recaf.asm;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;

/**
 * TODO: Plugin system, allow users to run scripts on input <i>(Ex: Onload &gt;-
 * run decrypt strings plugin)</i>.
 *
 * @author Matt
 */
public class PluginClassReader extends ClassReader {

	public PluginClassReader(InputStream is) throws IOException {
		super(is);
	}

}
