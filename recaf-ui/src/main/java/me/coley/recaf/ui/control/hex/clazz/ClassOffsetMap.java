package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.Field;
import me.coley.cafedude.Method;

import java.util.Map.Entry;
import java.util.NavigableMap;

import static me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType.*;

/**
 * Extracts offset information about sections of a class file.
 *
 * @author Matt Coley
 */
public class ClassOffsetMap extends ClassOffsetConsumer {
	/**
	 * @param cf
	 * 		Target class file to parse.
	 */
	public ClassOffsetMap(ClassFile cf) {
		super(cf);
		parse();
	}

	private void parse() {
		consume(4, MAGIC, 0xCAFEBABE);
		consume(2, MINOR_VERSION, cf.getVersionMinor());
		consume(2, MAJOR_VERSION, cf.getVersionMajor());
		consumeCP();
		consume(2, ACCESS_FLAGS, cf.getAccess());
		consume(2, THIS_CLASS, cf.getClassIndex());
		consume(2, SUPER_CLASS, cf.getSuperIndex());
		consume(2, INTERFACES_COUNT, cf.getInterfaceIndices().size());
		for (int itf : cf.getInterfaceIndices()) {
			consume(2, INTERFACE, itf);
		}
		consumeFields();
		consumeMethods();
		consumeAttributes(cf.getAttributes(), ATTRIBUTES_COUNT, CLASS_ATTRIBUTES);
	}

	private void consumeCP() {
		consume(2, CONSTANT_POOL_COUNT, cp.size());
		ConstPoolOffsetConsumer cpSizes = new ConstPoolOffsetConsumer(cf);
		map.put(offset, new ClassOffsetInfo(cf, CONSTANT_POOL, cp, offset, cpSizes.end()));
		offset = cpSizes.end() + 1;
	}

	private void consumeFields() {
		consume(2, FIELDS_COUNT, cf.getFields().size());
		for (Field field : cf.getFields()) {
			ClassMemberOffsetConsumer memberConsumer = new ClassMemberOffsetConsumer(true, offset, cf, field);
			ClassOffsetInfo info = new ClassOffsetInfo(cf, FIELD_INFO, cf.getFields().size(), offset, memberConsumer.end());
			memberConsumer.assignParent(info);
			map.put(offset, info);
			offset = memberConsumer.end() + 1;
		}
	}

	private void consumeMethods() {
		consume(2, METHODS_COUNT, cf.getMethods().size());
		for (Method method : cf.getMethods()) {
			ClassMemberOffsetConsumer memberConsumer = new ClassMemberOffsetConsumer(false, offset, cf, method);
			ClassOffsetInfo info = new ClassOffsetInfo(cf, METHOD_INFO, cf.getFields().size(), offset, memberConsumer.end());
			memberConsumer.assignParent(info);
			map.put(offset, info);
			offset = memberConsumer.end() + 1;
		}
	}

	/**
	 * @param offset
	 * 		Some offset in the class file.
	 *
	 * @return The information about whatever is at the offset.
	 */
	public ClassOffsetInfo get(int offset) {
		Entry<Integer, ClassOffsetInfo> x = map.floorEntry(offset);
		if (x != null)
			return x.getValue();
		return null;
	}

	/**
	 * @return Map of starting offsets to things recognized in the class file.
	 */
	public NavigableMap<Integer, ClassOffsetInfo> getMap() {
		return map;
	}
}
