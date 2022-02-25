package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.ClassFile;

import java.util.*;
import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.attribute.Attribute;

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
	 * @param collection
	 * 		Collection to store results into.
	 * @param size
	 * 		Size of item.
	 * @param type
	 * 		Type of item.
	 * @param value
	 * 		Item value.
	 *
	 * @return The info object of the consumed region.
	 */
	public ClassOffsetInfo consumeInto(Collection<ClassOffsetInfo> collection,
									   int size, ClassOffsetInfoType type, Object value) {
		ClassOffsetInfo info = consumeAndRegister(size, type, value, false);
		collection.add(info);
		return info;
	}

	/**
	 * @param size
	 * 		Size of item.
	 * @param type
	 * 		Type of item.
	 * @param value
	 * 		Item value.
	 *
	 * @return The info object of the consumed region.
	 */
	public ClassOffsetInfo consume(int size, ClassOffsetInfoType type, Object value) {
		return consumeAndRegister(size, type, value, true);
	}

	/**
	 * @param size
	 * 		Size of item.
	 * @param type
	 * 		Type of item.
	 * @param value
	 * 		Item value.
	 * @param register
	 * 		Auto-register info to offset.
	 *
	 * @return The info object of the consumed region.
	 */
	private ClassOffsetInfo consumeAndRegister(int size, ClassOffsetInfoType type, Object value, boolean register) {
		ClassOffsetInfo info = new ClassOffsetInfo(cf, type, value, offset, offset + size - 1);
		if (register) map.put(offset, info);
		offset += size;
		return info;
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

	/**
	 * Utility to wrap sub-items in a builder-like pattern.
	 */
	public class Wrapper {
		private final List<ClassOffsetInfo> items = new ArrayList<>();
		private final int start;
		private final ClassOffsetInfoType type;
		private Object value;

		/**
		 * @param type
		 * 		Expected type of data.
		 */
		public Wrapper(ClassOffsetInfoType type) {
			this.start = offset;
			this.type = type;
		}

		/**
		 * Used when a {@link ClassOffsetInfo} is generated externally instead
		 * of from our {@link #consume(int, ClassOffsetInfoType, Object)}.
		 *
		 * @param info
		 * 		Item to add.
		 */
		public void add(ClassOffsetInfo info) {
			items.add(info);
		}

		/**
		 * Add an item to the wrapper.
		 *
		 * @param size
		 * 		Size of item.
		 * @param type
		 * 		Type of item.
		 * @param value
		 * 		Item value.
		 *
		 * @return The info object of the consumed region.
		 */
		public ClassOffsetInfo consume(int size, ClassOffsetInfoType type, Object value) {
			ClassOffsetInfo info = consumeAndRegister(size, type, value, false);
			add(info);
			return info;
		}

		/**
		 * @return Build to offset info.
		 */
		public ClassOffsetInfo complete() {
			if (value == null)
				value = items;
			ClassOffsetInfo info = new ClassOffsetInfo(cf, type, value, start, offset - 1);
			items.forEach(i -> i.setParent(info));
			return info;
		}

		/**
		 * @return Start offset.
		 */
		public int getStart() {
			return start;
		}

		/**
		 * @param value
		 * 		Override value to assign to the {@link #complete() generated offset info}.
		 */
		public void setValue(Object value) {
			this.value = value;
		}
	}
}
