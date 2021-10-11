package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.Constants;
import me.coley.cafedude.attribute.Attribute;
import me.coley.cafedude.attribute.CodeAttribute;
import me.coley.cafedude.attribute.ConstantValueAttribute;
import me.coley.cafedude.attribute.DefaultAttribute;

import static me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType.*;

/**
 * Extracts offset information about an attribute.
 *
 * @author Matt Coley
 */
public class AttributeOffsetConsumer extends ClassOffsetConsumer implements Constants.Attributes {
	public final ClassOffsetInfo info;

	/**
	 * @param startOffset
	 * 		Initial offset to start from.
	 * @param cf
	 * 		Target class file to parse.
	 * @param attribute
	 * 		Attribute to parse.
	 */
	public AttributeOffsetConsumer(int startOffset, ClassFile cf, Attribute attribute) {
		super(cf);
		offset = startOffset;
		int size = attribute.computeCompleteLength();
		String name = cp.getUtf(attribute.getNameIndex());
		info = new ClassOffsetInfo(cf, ATTRIBUTE, attribute, offset, offset + size - 1);
		consume(2, ATTRIBUTE_NAME_INDEX, attribute.getNameIndex());
		consume(4, ATTRIBUTE_LENGTH, attribute.computeInternalLength());
		if (attribute instanceof DefaultAttribute) {
			DefaultAttribute dflt = (DefaultAttribute) attribute;
			consume(dflt.getData().length, ATTRIBUTE_DATA, dflt.getData());
		} else {
			switch (name) {
				case CODE: {
					CodeAttribute impl = (CodeAttribute) attribute;
					consume(2, CODE_MAX_STACK, impl.getMaxStack());
					consume(2, CODE_MAX_LOCALS, impl.getMaxStack());
					consume(4, CODE_LENGTH, impl.getCode().length);
					if (impl.getCode().length > 0) {
						consume(impl.getCode().length, CODE_BYTECODE, impl.getCode());
					}
					consume(2, CODE_EXCEPTIONS_TABLE_LENGTH, impl.getExceptionTable().size());
					for (CodeAttribute.ExceptionTableEntry entry : impl.getExceptionTable()) {
						consume(8, CODE_EXCEPTION, entry);
					}
					consume(2, ATTRIBUTE_LENGTH, impl.getAttributes().size());
					for (Attribute subAttribute : impl.getAttributes()) {
						AttributeOffsetConsumer helper = new AttributeOffsetConsumer(offset, cf, subAttribute);
						map.put(offset, helper.info);
						offset = helper.end();
					}
					break;
				}
				case CONSTANT_VALUE: {
					ConstantValueAttribute impl = (ConstantValueAttribute) attribute;
					consume(2, CONSTANT_VALUE_INDEX, impl.getConstantValueIndex());
					break;
				}
				case ANNOTATION_DEFAULT:
				case BOOTSTRAP_METHODS:
				case DEPRECATED:
				case ENCLOSING_METHOD:
				case EXCEPTIONS:
				case INNER_CLASSES:
				case LINE_NUMBER_TABLE:
				case LOCAL_VARIABLE_TABLE:
				case LOCAL_VARIABLE_TYPE_TABLE:
				case METHOD_PARAMETERS:
				case MODULE:
				case MODULE_HASHES:
				case MODULE_MAIN_CLASS:
				case MODULE_PACKAGES:
				case MODULE_RESOLUTION:
				case MODULE_TARGET:
				case NEST_HOST:
				case NEST_MEMBERS:
				case RECORD:
				case RUNTIME_VISIBLE_ANNOTATIONS:
				case RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:
				case RUNTIME_VISIBLE_TYPE_ANNOTATIONS:
				case RUNTIME_INVISIBLE_ANNOTATIONS:
				case RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS:
				case RUNTIME_INVISIBLE_TYPE_ANNOTATIONS:
				case PERMITTED_SUBCLASSES:
				case SIGNATURE:
				case SOURCE_DEBUG_EXTENSION:
				case SOURCE_FILE:
				case SOURCE_ID:
				case STACK_MAP_TABLE:
				case SYNTHETIC:
				default:
					// Generic handling
					map.put(offset, new ClassOffsetInfo(cf, ATTRIBUTE_DATA, attribute.getNameIndex(), offset, offset + size - 1));
					break;
			}
		}
		map.values().forEach(i -> i.setParent(info));
		offset = startOffset + size;
	}

	/**
	 * @return Attribute end offset.
	 */
	public int end() {
		return offset;
	}
}
