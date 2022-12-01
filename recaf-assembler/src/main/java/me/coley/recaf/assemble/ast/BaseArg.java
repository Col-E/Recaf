package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.StringUtil;
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
	@SuppressWarnings("unchecked")
	public static <Arg extends BaseArg> Arg of(BiFunction<ArgType, Object, Arg> argMapper, Object value) {
		if (value instanceof String)
			return argMapper.apply(ArgType.STRING, value);
		else if (value instanceof Boolean)
			return argMapper.apply(ArgType.BOOLEAN, value);
		else if (value instanceof Short)
			return argMapper.apply(ArgType.SHORT, value);
		else if (value instanceof Integer)
			return argMapper.apply(ArgType.INTEGER, value);
		else if (value instanceof Byte)
			return argMapper.apply(ArgType.BYTE, value);
		else if (value instanceof Character)
			return argMapper.apply(ArgType.CHAR, value);
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
			String enumDesc = array[0]; // descriptor format, we need internal type
			String enumNane = array[1];
			String type = enumDesc.substring(1, enumDesc.length() - 1);
			return (Arg) new Annotation.AnnoEnum(type, enumNane);
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
	public String print(PrintContext context) {
		switch (type) {
			case TYPE:
				Type type = (Type) value;
				if (type.getSort() == Type.OBJECT)
					return context.fmtKeyword("type ") + context.fmtIdentifier(type.getInternalName());
				else
					return context.fmtKeyword("type ") + context.fmtIdentifier(type.getDescriptor());
			case CHAR:
				return "'" + value + "'";
			case STRING:
				return " \"" + EscapeUtil.escape((String) getValue()) + '\"';
			case HANDLE:
				HandleInfo info = (HandleInfo) value;
				return context.fmtKeyword("handle ") + info.print(context) + "";
			case ANNO:
				Annotation anno = (Annotation) value;
				return anno.print(context);
			case ANNO_LIST:
				List<?> list = (List<?>) value;
				return context.fmtKeyword("args ") + list.stream()
						.map(String::valueOf)
						.collect(Collectors.joining(" ")) + " " + context.fmtKeyword("end");
			case ANNO_ENUM:
				return context.fmtKeyword("annotation-enum " ) + value;
			case BOOLEAN:
				return (Boolean) value ? "true" : "false";
			case LONG:
				return value + "L";
			case FLOAT:
				return value + "F";
			case DOUBLE:
			case SHORT:
			case BYTE:
			case INTEGER:
			default:
				return String.valueOf(value);
		}
	}
}
