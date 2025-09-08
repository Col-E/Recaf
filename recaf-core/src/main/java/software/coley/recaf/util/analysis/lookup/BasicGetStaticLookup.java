package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ReValue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Basic implementation of {@link GetStaticLookup} for common static fields.
 *
 * @author Matt Coley
 */
public class BasicGetStaticLookup implements GetStaticLookup {
	private static final Map<String, ReValue> CONST_FIELDS = new HashMap<>();
	private static final Logger logger = Logging.get(BasicGetStaticLookup.class);

	@Nonnull
	@Override
	public ReValue get(@Nonnull FieldInsnNode field) {
		String key = getKey(field);
		ReValue value = CONST_FIELDS.get(key);
		if (value == null) {
			try {
				value = ReValue.ofType(Type.getType(field.desc), Nullness.UNKNOWN);
			} catch (IllegalValueException ex) {
				logger.error("Failed default value computation for: " + key, ex);
			}
		}
		return Objects.requireNonNull(value);
	}

	@Override
	public boolean hasLookup(@Nonnull FieldInsnNode field) {
		return CONST_FIELDS.containsKey(getKey(field));
	}

	@Nonnull
	private static String getKey(@Nonnull FieldInsnNode field) {
		return field.owner + "." + field.name;
	}

	static {
		try {
			CONST_FIELDS.put("java/lang/Byte.BYTES", ReValue.ofConstant(Byte.BYTES));
			CONST_FIELDS.put("java/lang/Byte.SIZE", ReValue.ofConstant(Byte.SIZE));
			CONST_FIELDS.put("java/lang/Byte.MIN_VALUE", ReValue.ofConstant(Byte.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Byte.MAX_VALUE", ReValue.ofConstant(Byte.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Short.BYTES", ReValue.ofConstant(Short.BYTES));
			CONST_FIELDS.put("java/lang/Short.SIZE", ReValue.ofConstant(Short.SIZE));
			CONST_FIELDS.put("java/lang/Short.MIN_VALUE", ReValue.ofConstant(Short.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Short.MAX_VALUE", ReValue.ofConstant(Short.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Integer.BYTES", ReValue.ofConstant(Integer.BYTES));
			CONST_FIELDS.put("java/lang/Integer.SIZE", ReValue.ofConstant(Integer.SIZE));
			CONST_FIELDS.put("java/lang/Integer.MIN_VALUE", ReValue.ofConstant(Integer.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Integer.MAX_VALUE", ReValue.ofConstant(Integer.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Long.BYTES", ReValue.ofConstant(Long.BYTES));
			CONST_FIELDS.put("java/lang/Long.SIZE", ReValue.ofConstant(Long.SIZE));
			CONST_FIELDS.put("java/lang/Long.MIN_VALUE", ReValue.ofConstant(Long.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Long.MAX_VALUE", ReValue.ofConstant(Long.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Float.BYTES", ReValue.ofConstant(Float.BYTES));
			CONST_FIELDS.put("java/lang/Float.SIZE", ReValue.ofConstant(Float.SIZE));
			CONST_FIELDS.put("java/lang/Float.MIN_VALUE", ReValue.ofConstant(Float.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Float.MAX_VALUE", ReValue.ofConstant(Float.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Float.MIN_EXPONENT", ReValue.ofConstant(Float.MIN_EXPONENT));
			CONST_FIELDS.put("java/lang/Float.MAX_EXPONENT", ReValue.ofConstant(Float.MAX_EXPONENT));
			CONST_FIELDS.put("java/lang/Float.MIN_NORMAL", ReValue.ofConstant(Float.MIN_NORMAL));
			CONST_FIELDS.put("java/lang/Float.NaN", ReValue.ofConstant(Float.NaN));
			CONST_FIELDS.put("java/lang/Float.NEGATIVE_INFINITY", ReValue.ofConstant(Float.NEGATIVE_INFINITY));
			CONST_FIELDS.put("java/lang/Float.POSITIVE_INFINITY", ReValue.ofConstant(Float.POSITIVE_INFINITY));
			CONST_FIELDS.put("java/lang/Double.BYTES", ReValue.ofConstant(Double.BYTES));
			CONST_FIELDS.put("java/lang/Double.SIZE", ReValue.ofConstant(Double.SIZE));
			CONST_FIELDS.put("java/lang/Double.MIN_VALUE", ReValue.ofConstant(Double.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Double.MAX_VALUE", ReValue.ofConstant(Double.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Double.MIN_EXPONENT", ReValue.ofConstant(Double.MIN_EXPONENT));
			CONST_FIELDS.put("java/lang/Double.MAX_EXPONENT", ReValue.ofConstant(Double.MAX_EXPONENT));
			CONST_FIELDS.put("java/lang/Double.MIN_NORMAL", ReValue.ofConstant(Double.MIN_NORMAL));
			CONST_FIELDS.put("java/lang/Double.NaN", ReValue.ofConstant(Double.NaN));
			CONST_FIELDS.put("java/lang/Double.NEGATIVE_INFINITY", ReValue.ofConstant(Double.NEGATIVE_INFINITY));
			CONST_FIELDS.put("java/lang/Double.POSITIVE_INFINITY", ReValue.ofConstant(Double.POSITIVE_INFINITY));
			CONST_FIELDS.put("java/lang/Character.BYTES", ReValue.ofConstant(Character.BYTES));
			CONST_FIELDS.put("java/lang/Character.SIZE", ReValue.ofConstant(Character.SIZE));
			CONST_FIELDS.put("java/lang/Character.MIN_RADIX", ReValue.ofConstant(Character.MIN_RADIX));
			CONST_FIELDS.put("java/lang/Character.MAX_RADIX", ReValue.ofConstant(Character.MAX_RADIX));
			CONST_FIELDS.put("java/lang/Character.MIN_VALUE", ReValue.ofConstant(Character.MIN_VALUE));
			CONST_FIELDS.put("java/lang/Character.MAX_VALUE", ReValue.ofConstant(Character.MAX_VALUE));
			CONST_FIELDS.put("java/lang/Character.UNASSIGNED", ReValue.ofConstant((byte) 0));
			CONST_FIELDS.put("java/lang/Character.UPPERCASE_LETTER", ReValue.ofConstant((byte) 1));
			CONST_FIELDS.put("java/lang/Character.LOWERCASE_LETTER", ReValue.ofConstant((byte) 2));
			CONST_FIELDS.put("java/lang/Character.TITLECASE_LETTER", ReValue.ofConstant((byte) 3));
			CONST_FIELDS.put("java/lang/Character.MODIFIER_LETTER", ReValue.ofConstant((byte) 4));
			CONST_FIELDS.put("java/lang/Character.OTHER_LETTER", ReValue.ofConstant((byte) 5));
			CONST_FIELDS.put("java/lang/Character.NON_SPACING_MARK", ReValue.ofConstant((byte) 6));
			CONST_FIELDS.put("java/lang/Character.ENCLOSING_MARK", ReValue.ofConstant((byte) 7));
			CONST_FIELDS.put("java/lang/Character.COMBINING_SPACING_MARK", ReValue.ofConstant((byte) 8));
			CONST_FIELDS.put("java/lang/Character.DECIMAL_DIGIT_NUMBER", ReValue.ofConstant((byte) 9));
			CONST_FIELDS.put("java/lang/Character.LETTER_NUMBER", ReValue.ofConstant((byte) 10));
			CONST_FIELDS.put("java/lang/Character.OTHER_NUMBER", ReValue.ofConstant((byte) 11));
			CONST_FIELDS.put("java/lang/Character.SPACE_SEPARATOR", ReValue.ofConstant((byte) 12));
			CONST_FIELDS.put("java/lang/Character.LINE_SEPARATOR", ReValue.ofConstant((byte) 13));
			CONST_FIELDS.put("java/lang/Character.PARAGRAPH_SEPARATOR", ReValue.ofConstant((byte) 14));
			CONST_FIELDS.put("java/lang/Character.CONTROL", ReValue.ofConstant((byte) 15));
			CONST_FIELDS.put("java/lang/Character.FORMAT", ReValue.ofConstant((byte) 16));
			CONST_FIELDS.put("java/lang/Character.PRIVATE_USE", ReValue.ofConstant((byte) 18));
			CONST_FIELDS.put("java/lang/Character.SURROGATE", ReValue.ofConstant((byte) 19));
			CONST_FIELDS.put("java/lang/Character.DASH_PUNCTUATION", ReValue.ofConstant((byte) 20));
			CONST_FIELDS.put("java/lang/Character.START_PUNCTUATION", ReValue.ofConstant((byte) 21));
			CONST_FIELDS.put("java/lang/Character.END_PUNCTUATION", ReValue.ofConstant((byte) 22));
			CONST_FIELDS.put("java/lang/Character.CONNECTOR_PUNCTUATION", ReValue.ofConstant((byte) 23));
			CONST_FIELDS.put("java/lang/Character.OTHER_PUNCTUATION", ReValue.ofConstant((byte) 24));
			CONST_FIELDS.put("java/lang/Character.MATH_SYMBOL", ReValue.ofConstant((byte) 25));
			CONST_FIELDS.put("java/lang/Character.CURRENCY_SYMBOL", ReValue.ofConstant((byte) 26));
			CONST_FIELDS.put("java/lang/Character.MODIFIER_SYMBOL", ReValue.ofConstant((byte) 27));
			CONST_FIELDS.put("java/lang/Character.OTHER_SYMBOL", ReValue.ofConstant((byte) 28));
			CONST_FIELDS.put("java/lang/Character.INITIAL_QUOTE_PUNCTUATION", ReValue.ofConstant((byte) 29));
			CONST_FIELDS.put("java/lang/Character.FINAL_QUOTE_PUNCTUATION", ReValue.ofConstant((byte) 30));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_UNDEFINED", ReValue.ofConstant((byte) -1));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT", ReValue.ofConstant((byte) 0));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT", ReValue.ofConstant((byte) 1));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC", ReValue.ofConstant((byte) 2));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER", ReValue.ofConstant((byte) 3));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR", ReValue.ofConstant((byte) 4));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR", ReValue.ofConstant((byte) 5));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_ARABIC_NUMBER", ReValue.ofConstant((byte) 6));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR", ReValue.ofConstant((byte) 7));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_NONSPACING_MARK", ReValue.ofConstant((byte) 8));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_BOUNDARY_NEUTRAL", ReValue.ofConstant((byte) 9));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR", ReValue.ofConstant((byte) 10));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_SEGMENT_SEPARATOR", ReValue.ofConstant((byte) 11));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_WHITESPACE", ReValue.ofConstant((byte) 12));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_OTHER_NEUTRALS", ReValue.ofConstant((byte) 13));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING", ReValue.ofConstant((byte) 14));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE", ReValue.ofConstant((byte) 15));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING", ReValue.ofConstant((byte) 16));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE", ReValue.ofConstant((byte) 17));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT", ReValue.ofConstant((byte) 18));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE", ReValue.ofConstant((byte) 19));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE", ReValue.ofConstant((byte) 20));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE", ReValue.ofConstant((byte) 21));
			CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE", ReValue.ofConstant((byte) 22));
			CONST_FIELDS.put("java/lang/Character.MIN_HIGH_SURROGATE", ReValue.ofConstant(Character.MIN_HIGH_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MAX_HIGH_SURROGATE", ReValue.ofConstant(Character.MAX_HIGH_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MIN_LOW_SURROGATE", ReValue.ofConstant(Character.MIN_LOW_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MAX_LOW_SURROGATE", ReValue.ofConstant(Character.MAX_LOW_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MIN_SURROGATE", ReValue.ofConstant(Character.MIN_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MAX_SURROGATE", ReValue.ofConstant(Character.MAX_SURROGATE));
			CONST_FIELDS.put("java/lang/Character.MIN_SUPPLEMENTARY_CODE_POINT", ReValue.ofConstant(Character.MIN_SUPPLEMENTARY_CODE_POINT));
			CONST_FIELDS.put("java/lang/Character.MIN_CODE_POINT", ReValue.ofConstant(Character.MIN_CODE_POINT));
			CONST_FIELDS.put("java/lang/Character.MAX_CODE_POINT", ReValue.ofConstant(Character.MAX_CODE_POINT));
			CONST_FIELDS.put("java/lang/Math.E", ReValue.ofConstant(Math.E));
			CONST_FIELDS.put("java/lang/Math.PI", ReValue.ofConstant(Math.PI));
			CONST_FIELDS.put("java/lang/StrictMath.E", ReValue.ofConstant(Math.E));
			CONST_FIELDS.put("java/lang/StrictMath.PI", ReValue.ofConstant(Math.PI));
			CONST_FIELDS.put("java/io/File.separator", ReValue.ofConstant(File.separator));
			CONST_FIELDS.put("java/io/File.separatorChar", ReValue.ofConstant(File.separatorChar));
			CONST_FIELDS.put("java/io/File.pathSeparator", ReValue.ofConstant(File.pathSeparator));
			CONST_FIELDS.put("java/io/File.pathSeparatorChar", ReValue.ofConstant(File.pathSeparatorChar));
		} catch (IllegalValueException ex) {
			logger.error("Failed creating value registry", ex);
		}
	}
}
