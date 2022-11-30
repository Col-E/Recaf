package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.JasmToUnitTransformer;
import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.ParserContext;

import java.util.LinkedList;

/**
 * Test utility methods for JASM.
 */
public class JasmUtils {
	protected static final Keywords DEFAULT_KEYWORDS = new Keywords();

	protected static Unit createSilentUnit(Keywords keywords, String code) {
		try {
			return createUnit(keywords, code);
		} catch (Throwable e) {
			return null;
		}
	}

	protected static Unit createUnit(Keywords keywords, String code) throws Throwable {
		ParserContext ctx = createParser(keywords, code);
		JasmToUnitTransformer transformer = new JasmToUnitTransformer(ctx.parse());
		return transformer.generateUnit();
	}

	protected static ParserContext createParser(Keywords keywords, String code) {
		Parser parser = new Parser(keywords);
		return new ParserContext(new LinkedList<>(parser.tokenize("<test>", code)), parser);
	}
}
