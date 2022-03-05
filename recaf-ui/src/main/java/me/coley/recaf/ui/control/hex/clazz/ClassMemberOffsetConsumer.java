package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.ClassMember;

import static me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType.*;

/**
 * Extracts offset information about a class member.
 *
 * @author Matt Coley
 */
public class ClassMemberOffsetConsumer extends ClassOffsetConsumer {
	/**
	 * @param field
	 *        {@code true} for parsing fields. {@code false} for methods.
	 * @param startOffset
	 * 		Initial offset to start from.
	 * @param cf
	 * 		Target class file to parse.
	 * @param member
	 * 		Member to parse.
	 */
	public ClassMemberOffsetConsumer(boolean field, int startOffset, ClassFile cf, ClassMember member) {
		super(cf);
		offset = startOffset;
		consume(2, field ? FIELD_ACC_FLAGS : METHOD_ACC_FLAGS, member.getAccess());
		consume(2, field ? FIELD_NAME_INDEX : METHOD_NAME_INDEX, member.getNameIndex());
		consume(2, field ? FIELD_DESC_INDEX : METHOD_DESC_INDEX, member.getTypeIndex());
		consumeAttributes(member.getAttributes(),
				field ? FIELD_ATTRIBUTES_COUNT : METHOD_ATTRIBUTES_COUNT,
				field ? FIELD_ATTRIBUTES : METHOD_ATTRIBUTES);
		// Put back to inclusive range for last item
		offset--;
	}

	/**
	 * @return Member end offset.
	 */
	public int end() {
		return offset;
	}
}
