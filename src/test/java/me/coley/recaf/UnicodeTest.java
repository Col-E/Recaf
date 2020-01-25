package me.coley.recaf;

import me.coley.recaf.util.UnicodeUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unicode unescape tests.
 *
 * @author xxDark
 */
public class UnicodeTest {

	@Test
	public void testUnescape() {
		String unicode = "\u0048\u0065\u006c\u006c\u006f\u002c \u0057\u006f\u0072\u006c\u0064";
		String decoded = UnicodeUtil.unescape(unicode);
		assertEquals("Hello, World", decoded);
	}

	@Test
	public void testNoUnescape() {
		String control = "\n\r\b\f\t";
		String decoded = UnicodeUtil.unescape(control);
		assertEquals(control, decoded);
	}
}
