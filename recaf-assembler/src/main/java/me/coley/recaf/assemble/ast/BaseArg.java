package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Base arg type, essentially an object wrapper.
 *
 * @author Matt Coley
 */
public abstract class BaseArg extends BaseElement implements Printable {
	private final ArgType type;
	private final Object value;

	/**
	 * @param type
	 * 		Type of value.
	 * @param value
	 * 		Value instance.
	 */
	public BaseArg(ArgType type, Object value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * @param argMapper
	 * 		Mapper to the generic arg class.
	 * @param value
	 * 		Value instance.
	 * @param <Arg>
	 * 		Argument class type.
	 *
	 * @return Arg wrapper.
	 */
	public static <Arg extends BaseArg> Arg of(BiFunction<ArgType, Object, Arg> argMapper, Object value) {
		if (value instanceof String)
			return argMapper.apply(ArgType.STRING, value);
		else if (value instanceof Integer)
			return argMapper.apply(ArgType.INTEGER, value);
		else if (value instanceof Float)
			return argMapper.apply(ArgType.FLOAT, value);
		else if (value instanceof Double)
			return argMapper.apply(ArgType.DOUBLE, value);
		else if (value instanceof Long)
			return argMapper.apply(ArgType.LONG, value);
		else if (value instanceof Type)
			return argMapper.apply(ArgType.TYPE, value);
		else if (value instanceof Annotation)
			return argMapper.apply(ArgType.ANNO, value);
		else if (value instanceof List)
			return argMapper.apply(ArgType.ANNO_LIST, value);
		else if (value instanceof Handle) {
			Handle handle = (Handle) value;
			HandleInfo handleInfo = new HandleInfo(OpcodeUtil.tagToName(handle.getTag()), handle.getOwner(), handle.getName(), handle.getDesc());
			return argMapper.apply(ArgType.HANDLE, handleInfo);
		} else if (value instanceof String[]) {
			// ASM has this as a special case for enums
			//  0: desc of enum type
			//  1: name of enum value
			String[] array = (String[]) value;
			return argMapper.apply(ArgType.ANNO_ENUM, array);
		} else if (value == null)
			throw new IllegalStateException("Arg content must not be null!");
		throw new IllegalStateException("Unsupported argument type: " + value.getClass().getName());
	}

	/**
	 * @return Type of value.
	 */
	public ArgType getType() {
		return type;
	}

	/**
	 * @return Value instance.
	 */
	public Object getValue() {
		return value;
	}

	@Override
	public String print() {
		switch (type) {
			case TYPE:
				Type type = (Type) value;
				if (type.getSort() == Type.OBJECT)
					return type.getInternalName();
				else
					return type.getDescriptor();
			case STRING:
				return "\"" + value + "\"";
			case HANDLE:
				HandleInfo info = (HandleInfo) value;
				return "handle(" + info.print() + ")";
			case ANNO:
				Annotation anno = (Annotation) value;
				return anno.print();
			case ANNO_LIST:
				List<?> list = (List<?>) value;
				return "[" + list.stream()
						.map(String::valueOf)
						.collect(Collectors.joining(", ")) + "]";
			case ANNO_ENUM:
				return (String) value;
			case INTEGER:
			case FLOAT:
			case DOUBLE:
			case LONG:
			default:
				return String.valueOf(value);
		}
	}
}
