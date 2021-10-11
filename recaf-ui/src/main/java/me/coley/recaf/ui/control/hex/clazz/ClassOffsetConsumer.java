package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.ConstPool;
import me.coley.cafedude.attribute.Attribute;

import java.util.*;

/**
 * Base offset consumer for parsing a class file.
 *
 * @author Matt Coley
 */
public class ClassOffsetConsumer {
	protected final NavigableMap<Integer, ClassOffsetInfo> map = new TreeMap<>();
	protected final ClassFile cf;
	protected final ConstPool cp;
	protected int offset = 0;

	/**
	 * @param cf
	 * 		Target class file to parse.
	 */
	public ClassOffsetConsumer(ClassFile cf) {
		this.cf = cf;
		this.cp = cf.getPool();
	}

	/**
	 * @param size
	 * 		Size of item.
	 * @param type
	 * 		Type of item.
	 * @param value
	 * 		Item value.
	 */
	public void consume(int size, ClassOffsetInfoType type, Object value) {
		map.put(offset, new ClassOffsetInfo(cf, type, value, offset, offset + size - 1));
		offset += size;
	}

	/**
	 * @param attributes
	 * 		Collection to consume.
	 * @param countType
	 * 		Count type <i>(Differ class/field/method attrs)</i>.
	 * @param wrapperType
	 * 		Attribute parent/wrapper type <i>(Differ class/field/method attrs)</i>.
	 */
	public void consumeAttributes(Collection<Attribute> attributes,
								  ClassOffsetInfoType countType,
								  ClassOffsetInfoType wrapperType) {
		int attribCount = attributes.size();
		consume(2, countType, attribCount);
		if (attribCount > 0) {
			int attributeStart = offset;
			List<AttributeOffsetConsumer> attributeHelpers = new ArrayList<>();
			for (Attribute attribute : attributes) {
				AttributeOffsetConsumer helper = new AttributeOffsetConsumer(offset, cf, attribute);
				attributeHelpers.add(helper);
				offset = helper.end();
			}
			ClassOffsetInfo info = new ClassOffsetInfo(cf, wrapperType, attributes, attributeStart, offset - 1);
			attributeHelpers.forEach(helper -> helper.info.setParent(info));
			map.put(attributeStart, info);
		}
	}

	/**
	 * @param parent
	 * 		Parent to assign to all values recorded in the current offset consumer.
	 */
	public void assignParent(ClassOffsetInfo parent) {
		map.values().forEach(i -> i.setParent(parent));
	}
}
