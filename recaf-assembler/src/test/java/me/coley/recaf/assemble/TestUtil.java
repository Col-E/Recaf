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

	protected static Unit generate(String s) {
		ParserContext ctx = parser(s);
		try {
			JasmToAstTransformer transformer = new JasmToAstTransformer(ctx.parse());
			return transformer.generateUnit();
		} catch (Exception e) {
			return null;
		}
	}
}
