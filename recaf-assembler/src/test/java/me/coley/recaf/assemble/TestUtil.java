package me.coley.recaf.assemble;

import me.coley.recaf.assemble.parser.BytecodeLexer;
import me.coley.recaf.assemble.parser.BytecodeParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Test utility methods.
 */
public class TestUtil {
	protected static BytecodeParser parser(String s) {
		CharStream is = CharStreams.fromString(s);
		BytecodeLexer lexer = new BytecodeLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		return new BytecodeParser(stream);
	}
}
