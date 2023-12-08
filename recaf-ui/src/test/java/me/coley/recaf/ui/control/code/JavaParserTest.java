package me.coley.recaf.ui.control.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import me.coley.recaf.ui.control.code.LanguageStyler.Section;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JavaParserTest {

	private final List<Section> styledSections = new ArrayList<>();
	private LanguageStyler languageStyler;

	public static List<String> generateDoubleCombinations() {
		List<String> combinations = new ArrayList<>();
		int length = 3;
		// Generate base numbers without exponent
		for (int i = 1; i <= length; i++) {
			String number = "5".repeat(i);
			combinations.add(number + ".0"); // Whole number
			for (int j = 1; j < i; j++) {
				// Insert decimal point at different positions
				String withDecimal = number.substring(0, j - 1) + "." + number.substring(j);
				combinations.add(withDecimal);
				if (j > 1) {//more than on digit in the whole part
					combinations.add(5 + "_" + withDecimal);
					combinations.add(5 + "__" + withDecimal);
					combinations.add(5 + "_" + 5 + "_" + withDecimal + "_" + 0 + "_" + 1);
				}
				combinations.add(withDecimal + "_" + 5);
				combinations.add(withDecimal + "_" + 0 + "_" + 1);
			}
		}
		// Generate numbers with exponent part
		List<String> withExponents = new ArrayList<>();
		for (String base : combinations) {
			// Add exponent variations to each base number
			for (int i = 1; i <= length; i++) {
				String exponent = "1".repeat(i);
				withExponents.add(base + "e" + exponent);
				withExponents.add(base + "e+" + exponent);
				withExponents.add(base + "e-" + exponent);
				withExponents.add(base + "e" + 1 + "_" + 0);
				withExponents.add(base + "e" + 1 + "__" + 0);
				withExponents.add(base + "e" + 1 + "_00_" + 1);
				withExponents.add(base + "e+" + 1 + "_" + 1);
				withExponents.add(base + "e-" + 0 + "_" + 1);
			}
		}
		combinations.addAll(withExponents);
		var endCharCombination = new ArrayList<String>();
		for (String combination : combinations) {
			endCharCombination.add(combination + "d");
			endCharCombination.add(combination + "D");
		}
		combinations.addAll(endCharCombination);
		return combinations;
	}

	@BeforeEach
	void setup() {
		styledSections.clear();
		languageStyler = new LanguageStyler(Languages.JAVA, new Styleable() {
			@Override
			public Collection<String> getStyleAtPosition(int pos) {
				return Collections.emptyList();
			}

			@Override
			public CompletableFuture<Void> onClearStyle() {
				styledSections.clear();
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public CompletableFuture<Void> onApplyStyle(int start, List<Section> sections) {
				styledSections.addAll(sections.stream().filter(s -> s.classes.contains("constant")).collect(Collectors.toList()));
				return CompletableFuture.completedFuture(null);
			}
		});
	}

	@Test
	public void testJavaConstantStyling_doubleConstant() {
		assertStyledSections(generateDoubleCombinations()
								 .stream()
								 .peek(validDoubleDeclaration -> languageStyler.styleCompleteDocument(" " + validDoubleDeclaration))
								 .toArray(String[]::new));
	}

	@Test
	public void testJavaConstantStyling() {
		languageStyler.styleCompleteDocument("public class ConstantsShowcase {\n"
												 + "\n"
												 + "    // Various constant declarations\n"
												 + "    public static final boolean FLAG = true;\n"
												 + "    public static final boolean FLAG_2 = false;\n"
												 + "    public static final int DECIMAL_CONSTANT = 42;\n"
												 + "    public static final long LONG_CONSTANT = 123456789l;\n"
												 + "    public static final int OCTAL_CONSTANT = 0757;\n"
												 + "    public static final int HEX_CONSTANT = 0x1a2b3c;\n"
												 + "    public static final int BINARY_CONSTANT = 0b101011;\n"
												 + "    public static final double FLOATING_POINT_CONSTANT = 3.14159;\n"
												 + "    public static final double EXPONENTIAL_CONSTANT = 1.234e2;\n"
												 + "    public static final float FLOAT_CONSTANT = 2.71828f;\n"
												 + "    public static final String GREETING = \"Hello, World!\";\n"
												 + "    public static final char CHAR_CONSTANT = 'A';\n"
												 + "    public static final char ESCAPED_CHAR_CONSTANT = '\\n';\n"
												 + "\n"
												 + "    /**\n"
												 + "    * This is a great javadoc header\n"
												 + "    **/\n"
												 + "    public static void main(String[] args) {\n"
												 + "        float x = .6F;\n"
												 + "        double t =.8D;\n"
												 + "        long l =0L;\n"
												 + "        while (false) {\n"
												 + "            if (true) args[-1] = \"'3'\";\n"
												 + "        }\n"
												 + "        // Using constants in calculations\n"
												 + "        int total = DECIMAL_CONSTANT + OCTAL_CONSTANT + HEX_CONSTANT + BINARY_CONSTANT;\n"
												 + "        double scientific = FLOATING_POINT_CONSTANT * EXPONENTIAL_CONSTANT;\n"
												 + "\n"
												 + "        // Using constants in output\n"
												 + "        System.out.println(GREETING);\n"
												 + "        System.out.println(\"FLAG is set to: \" + FLAG);\n"
												 + "        System.out.println(\"Total of integer constants: \" + total);\n"
												 + "        System.out.println(\"Scientific calculation result: \" + scientific);\n"
												 + "        System.out.println(\"Float constant value: \" + FLOAT_CONSTANT);\n"
												 + "        System.out.println(\"Character constants: \" + CHAR_CONSTANT + \" \" + ESCAPED_CHAR_CONSTANT);\n"
												 + "\n"
												 + "        // Control structures with constants\n"
												 + "        if (FLAG) {\n"
												 + "			double x = 0_.0;            "
												 + "System.out.println(\"The flag is true!\");\n"
												 + "        }\n"
												 + "\n"
												 + "        for (int i = 0; i < DECIMAL_CONSTANT; i++) {\n"
												 + "            System.out.println(\"Counting: \" + i);\n"
												 + "        }\n"
												 + "\n"
												 + "        // Edge cases and non-constants\n"
												 + "        String notAConstant = \"Variable string\";\n"
												 + "        int result = someMethod(HEX_CONSTANT, LONG_CONSTANT, null);\n"
												 + "        char[] charArray = {CHAR_CONSTANT, ESCAPED_CHAR_CONSTANT};\n"
												 +
												 "    }"
												 +
												 "\n"
												 +
												 "\n"
												 + "    /* this is a wonderful javadoc comment */\n"
												 + "    private static int someMethod(int hex, long constant) {\n"
												 + "        // Imagine some complex logic here that uses the constants provided\n"
												 + "return hex + (int)constant;\n"
												 + "    }\n"
												 + "}");

		assertStyledSections(new String[]{
			"true", "false", "42", "123456789l", "0757", "0x1a2b3c",
			"0b101011", "3.14159", "1.234e2", "2.71828f", "'A'", "'\\n'",
			".6F", ".8D", "0L", "false", "true", "1", "0", "null"});
	}

	private void assertStyledSections(String[] expectedStyledSections) {
		Assertions.assertEquals(String.join("\n", expectedStyledSections), styledSections.stream().map(s -> s.text).collect(Collectors.joining("\n")),
			"The declaration should have been styled !");
	}
}
