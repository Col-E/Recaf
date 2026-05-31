package software.coley.recaf.util.android;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue.Type;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.coley.recaf.util.android.AndroidXmlUtil.formatBinaryValue;
import static software.coley.recaf.util.android.AndroidXmlUtil.formatResourceId;

/**
 * Tests for {@link AndroidXmlUtil}.
 */
class AndroidXmlUtilTest {
	@Test
	void formatsReferences() {
		int appResId = 0x7F020123;
		AndroidRes resources = mock(AndroidRes.class);
		when(resources.getResName(appResId)).thenReturn("layout/main");

		// References are: @path/to/resource (raw-hex-id)
		assertEquals("@layout/main (0x" + Integer.toHexString(appResId).toUpperCase() + ")", formatBinaryValue(resources, value(Type.REFERENCE, appResId)));

		int frameworkAttrId = AndroidRes.getAndroidBase().getResId("attr/theme");
		assertNotEquals(-1, frameworkAttrId);
		assertEquals("?android:attr/theme (" + formatResourceId(frameworkAttrId) + ")",
				formatBinaryValue(AndroidRes.getEmpty(), value(Type.ATTRIBUTE, frameworkAttrId)));
	}

	@Test
	void formatsStringsAndScalars() {
		AndroidRes resources = mock(AndroidRes.class);
		when(resources.getString(7)).thenReturn("res/layout/main.xml");

		// Strings and scalars are just their decoded value.
		assertEquals("res/layout/main.xml", formatBinaryValue(resources, value(Type.STRING, 7)));
		assertEquals("123", formatBinaryValue(resources, value(Type.INT_DEC, 123)));
		assertEquals("1.5", formatBinaryValue(resources, value(Type.FLOAT, Float.floatToIntBits(1.5f))));
		assertEquals("false", formatBinaryValue(resources, value(Type.INT_BOOLEAN, 0)));
		assertEquals("empty", formatBinaryValue(resources, value(Type.NULL, 1)));
	}

	@Test
	void formatsComplexValuesAndColors() {
		AndroidRes resources = mock(AndroidRes.class);

		// Complex values are formatted as their decoded value with the appropriate unit.
		assertEquals("24dp", formatBinaryValue(resources, value(Type.DIMENSION, (24 << 8) | 0x01)));
		assertEquals("50%", formatBinaryValue(resources, value(Type.FRACTION, 0x4010)));
		assertEquals("#11223344", formatBinaryValue(resources, value(Type.INT_COLOR_ARGB8, 0x11223344)));
		assertEquals("#123", formatBinaryValue(resources, value(Type.INT_COLOR_RGB4, 0x123)));
	}

	/**
	 * @param type
	 * 		Value type to encode.
	 * @param data
	 * 		Data to encode.
	 *
	 * @return Encoded resource value.
	 */
	@Nonnull
	private static BinaryResourceValue value(@Nonnull Type type, int data) {
		ByteBuffer buffer = ByteBuffer.allocate(BinaryResourceValue.SIZE).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) BinaryResourceValue.SIZE);
		buffer.put((byte) 0);
		buffer.put(type.code());
		buffer.putInt(data);
		buffer.flip();
		return BinaryResourceValue.create(buffer);
	}
}
