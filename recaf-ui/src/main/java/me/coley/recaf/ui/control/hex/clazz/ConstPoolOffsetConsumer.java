package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.ConstantPoolConstants;
import me.coley.cafedude.classfile.constant.ConstPoolEntry;
import me.coley.cafedude.classfile.constant.CpUtf8;

import static me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType.*;

/**
 * Extracts offset information about a class's constant pool.
 *
 * @author Matt Coley
 */
public class ConstPoolOffsetConsumer extends ClassOffsetConsumer implements ConstantPoolConstants {
	/**
	 * @param cf
	 * 		Target class file to parse.
	 */
	public ConstPoolOffsetConsumer(ClassFile cf) {
		super(cf);
		// The constant pool will always start at the offset of 10
		offset = 10;
		ClassOffsetInfo i = null;
		for (ConstPoolEntry entry : cp) {
			switch (entry.getTag()) {
				case UTF8:
					// Size: u2_utf_len, utf_chars
					consume(2 + getUtfLen(((CpUtf8) entry).getText()), CP_UTF8, entry);
					break;
				case INTEGER:
					// Size: u4_int
					consume(4, CP_INTEGER, entry);
					break;
				case FLOAT:
					// Size: u4_float
					consume(4, CP_FLOAT, entry);
					break;
				case LONG:
					// Size: u8_long
					consume(8, CP_LONG, entry);
					break;
				case DOUBLE:
					// Size: u8_double
					consume(8, CP_DOUBLE, entry);
					break;
				case CLASS:
					// Size: u2_utf_index
					consume(2, CP_CLASS, entry);
					break;
				case STRING:
					// Size: u2_utf_index
					consume(2, CP_STRING, entry);
					break;
				case FIELD_REF:
					// Size: u2_class_index / u2_name_type_index
					consume(4, CP_FIELD_REF, entry);
					break;
				case METHOD_REF:
					// Size: u2_class_index / u2_name_type_index
					consume(4, CP_METHOD_REF, entry);
					break;
				case INTERFACE_METHOD_REF:
					// Size: u2_class_index / u2_name_type_index
					consume(4, CP_INTERFACE_METHOD_REF, entry);
					break;
				case NAME_TYPE:
					// Size: u2_name_utf_index / u2_desc_utf_index
					consume(4, CP_NAME_TYPE, entry);
					break;
				case METHOD_HANDLE:
					// Size: u1_ref_kind / u2_ref_index
					consume(3, CP_METHOD_HANDLE, entry);
					break;
				case METHOD_TYPE:
					// Size: u2_utf_desc_index
					consume(2, CP_METHOD_TYPE, entry);
					break;
				case DYNAMIC:
					// Size: u2_bsm_attr_index / u2_name_type_index
					consume(4, CP_DYNAMIC, entry);
					break;
				case INVOKE_DYNAMIC:
					// Size: u2_bsm_attr_index / u2_name_type_index
					consume(4, CP_INVOKE_DYNAMIC, entry);
					break;
				case MODULE:
					// Size: u2_utf_name_index
					consume(2, CP_MODULE, entry);
					break;
				case PACKAGE:
					// Size: u2_utf_name_index
					consume(2, CP_PACKAGE, entry);
					break;
				default:
					throw new IllegalStateException("Unknown CP entry tag: " + entry.getTag());
			}
		}
		// Put back to inclusive range for last item
		offset--;
	}

	@Override
	public ClassOffsetInfo consume(int size, ClassOffsetInfoType type, Object value) {
		// +1 here is for the const_pool_entry tag, which is u1
		return super.consume(size + 1, type, value);
	}

	/**
	 * @return CP end offset.
	 */
	public int end() {
		return offset;
	}

	/**
	 * Pulled from {@link java.io.DataOutputStream#writeUTF(String)}.
	 *
	 * @param text
	 * 		Text to write.
	 *
	 * @return UTF length of text.
	 */
	private static int getUtfLen(String text) {
		int strlen = text.length();
		int utflen = 0;
		int c;
		for (int i = 0; i < strlen; i++) {
			c = text.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}
		return utflen;
	}
}
