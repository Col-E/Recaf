package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.JasmToAstTransformer;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.ParserContext;
import java.util.LinkedList;

/**
 * Test utility methods.
 */
public class TestUtil {
	protected static ParserContext parser(String s) {
		Parser parser = new Parser();
		return new ParserContext(new LinkedList<>(parser.tokenize("<test>", s)), parser);
	}

	protected static Unit generateSilent(String s) {
		try {
			return generate(s);
		} catch (Throwable e) {
			return null;
		}
	}

	protected static Unit generate(String s) throws Throwable {
		ParserContext ctx = parser(s);
		JasmToAstTransformer transformer = new JasmToAstTransformer(ctx.parse());
		return transformer.generateUnit();
	}
}
