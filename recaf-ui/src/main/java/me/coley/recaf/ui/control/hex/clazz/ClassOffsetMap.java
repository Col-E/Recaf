package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.ClassFile;

import java.util.Map.Entry;
import java.util.NavigableMap;
import me.coley.cafedude.classfile.Field;
import me.coley.cafedude.classfile.Method;

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
		consumeInterfaces();
		consumeFields();
		consumeMethods();
		consumeAttributes(cf.getAttributes(), ATTRIBUTES_COUNT, CLASS_ATTRIBUTES);
	}

	private void consumeCP() {
		consume(2, CONSTANT_POOL_COUNT, cp.size());
		ConstPoolOffsetConsumer cpHelper = new ConstPoolOffsetConsumer(cf);
		ClassOffsetInfo info = new ClassOffsetInfo(cf, CONSTANT_POOL, cp, offset, cpHelper.end());
		cpHelper.assignParent(info);
		map.put(offset, info);
		offset = cpHelper.end() + 1;
	}

	private void consumeInterfaces() {
		Wrapper wrapper = new Wrapper(INTERFACES);
		wrapper.setValue(cf.getInterfaceIndices());
		for (int itf : cf.getInterfaceIndices()) {
			wrapper.consume(2, INTERFACE_INDEX, itf);
		}
		map.put(wrapper.getStart(), wrapper.complete());
	}

	private void consumeFields() {
		consume(2, FIELDS_COUNT, cf.getFields().size());
		Wrapper fields = new Wrapper(FIELDS);
		fields.setValue(cf.getFields());
		for (Field field : cf.getFields()) {
			ClassMemberOffsetConsumer memberConsumer = new ClassMemberOffsetConsumer(true, offset, cf, field);
			ClassOffsetInfo info = new ClassOffsetInfo(cf, FIELD_INFO, field, offset, memberConsumer.end());
			memberConsumer.assignParent(info);
			fields.add(info);
			offset = memberConsumer.end() + 1;
		}
		map.put(fields.getStart(), fields.complete());
	}

	private void consumeMethods() {
		consume(2, METHODS_COUNT, cf.getMethods().size());
		Wrapper methods = new Wrapper(METHODS);
		methods.setValue(cf.getMethods());
		for (Method method : cf.getMethods()) {
			ClassMemberOffsetConsumer memberConsumer = new ClassMemberOffsetConsumer(false, offset, cf, method);
			ClassOffsetInfo info = new ClassOffsetInfo(cf, METHOD_INFO, method, offset, memberConsumer.end());
			memberConsumer.assignParent(info);
			methods.add(info);
			offset = memberConsumer.end() + 1;
		}
		map.put(methods.getStart(), methods.complete());
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
