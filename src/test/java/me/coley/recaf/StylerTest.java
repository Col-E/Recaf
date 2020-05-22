package me.coley.recaf;

import me.coley.recaf.ui.controls.text.model.LanguageStyler;
import me.coley.recaf.ui.controls.text.model.Languages;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression tests to assert the odd load-order-based errors with the language styler don't resurface.
 * Sometimes the order of which language is used would somehow effect the regex system.
 * It was incredibly odd, and the fix <i>seems</i> to be remove {@code \B} usage.
 *
 * @author Matt
 */
public class StylerTest {
	private static final Map<String, String> langToExample = new HashMap<>();
	static {
		langToExample.put("css", ".type {\n\tfont-family: Arial;\n}");
		langToExample.put("java", "class Name {\n\tstatic int i = 0;\n}");
		langToExample.put("json", "{ \"key\": \"val\", \"list\": [ { }  ] }");
		langToExample.put("xml", "<tag>value</tag>");
		langToExample.put("mf", "Manifest-Version: 1.0\nMain-Class: me.coley.recaf.Recaf\nCan-Redefine-Classes: true");
		langToExample.put("properties", "locale=en_US\nwriter1=file\nwriter1.file=#{user.home}/rclog.txt");
		langToExample.put("bytecode", "DEFINE static main([Ljava/lang/String; args)V\nRETURN");
	}

	@ParameterizedTest
	@MethodSource("generateCombinations")
	public void testStyleLoadOrder(String first, String second) {
		try {
			LanguageStyler styler = new LanguageStyler(Languages.find(first));
			styler.computeStyle(langToExample.get(first));
			//
			styler = new LanguageStyler(Languages.find(second));
			styler.computeStyle(langToExample.get(second));
		} catch(Throwable ex) {
			// If the system regresses to how it used to fail, it will throw a NPE
			// when the styler tries to use Matcher.find()
			fail(ex);
		}
	}

	public static Stream<Arguments> generateCombinations() {
		List<Arguments> pairs = new ArrayList<>();
		for (String first : langToExample.keySet())
			for (String second : langToExample.keySet())
				pairs.add(Arguments.of(first, second));
		return pairs.stream();
	}
}
