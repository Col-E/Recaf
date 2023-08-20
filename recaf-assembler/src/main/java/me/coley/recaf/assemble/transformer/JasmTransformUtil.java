package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.arch.module.*;
import me.coley.recaf.assemble.ast.arch.module.Module;
import me.coley.recaf.assemble.ast.arch.record.Record;
import me.coley.recaf.assemble.ast.arch.record.RecordComponent;
import me.coley.recaf.assemble.ast.meta.Signature;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Location;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.attributes.*;
import me.darknet.assembler.parser.groups.annotation.*;
import me.darknet.assembler.parser.groups.method.*;
import me.darknet.assembler.parser.groups.module.*;
import me.darknet.assembler.parser.groups.instructions.*;
import me.darknet.assembler.parser.groups.record.RecordComponentGroup;
import me.darknet.assembler.parser.groups.record.RecordGroup;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JasmTransformUtil {
	public static Modifiers convertModifiers(AccessModsGroup group) {
		Modifiers modifiers = new Modifiers();
		for (AccessModGroup accessMod : group.getAccessMods())
			modifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
		return wrap(group, modifiers);
	}

	public static Signature convertSignature(SignatureGroup group) {
		return wrap(group, new Signature(group.getDescriptor().content()));
	}

	public static ThrownException convertThrows(ThrowsGroup thrw) {
		return wrap(thrw, new ThrownException(thrw.getClassName().content()));
	}

	public static HandleInfo convertHandle(HandleGroup handle) {
		String desc = handle.getDescriptor().content();
		if (desc.charAt(0) == '(') {
			MethodDescriptor mdh = new MethodDescriptor(handle.getName().content(), desc);
			return wrap(handle, new HandleInfo(
					handle.getHandleType().content(),
					mdh.getOwner(),
					mdh.getName(),
					mdh.getDescriptor()));
		} else {
			FieldDescriptor mdh = new FieldDescriptor(handle.getName().content(), desc);
			return wrap(handle, new HandleInfo(
					handle.getHandleType().content(),
					mdh.getOwner(),
					mdh.getName(),
					mdh.getDesc()));
		}
	}

	public static Annotation convertAnnotation(AnnotationGroup annotation) throws AssemblerException {
		Map<String, Annotation.AnnoArg> args = new HashMap<>();
		for (AnnotationParamGroup param : annotation.getParams()) {
			annotationParam(param, args);
		}
		return wrap(annotation, new Annotation(annotation.isInvisible(), annotation.getClassGroup().content(), args));
	}

	/**
	 * Convert a {@link InnerClassGroup} to a {@link InnerClass}.
	 * @param innerClass Inner class group.
	 * @return Inner class.
	 * @throws AssemblerException If the inner class group is invalid.
	 */
	public static InnerClass convertInnerClass(InnerClassGroup innerClass) throws AssemblerException {
		Modifiers modifiers = new Modifiers();
		for (AccessModGroup accessMod : innerClass.getAccessMods().getAccessMods())
			modifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
		return wrap(innerClass, new InnerClass(
				modifiers,
				content(innerClass.getName()),
				content(innerClass.getOuterName()),
				content(innerClass.getInnerName())));
	}

	/**
	 * Convert a {@link ModuleGroup} to a {@link Module}.
	 * @param moduleGroup Module group.
	 * @return Module.
	 * @throws AssemblerException If the module group is invalid.
	 */
	public static Module convertModule(ModuleGroup moduleGroup) throws AssemblerException {
		Modifiers modifiers = new Modifiers();
		for (AccessModGroup accessMod : moduleGroup.getAccessMods().getAccessMods())
			modifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
		String name = content(moduleGroup.getName());
		Module module = new Module(name, modifiers);
		if(moduleGroup.getVersion() != null)
			module.setVersion(content(moduleGroup.getVersion().getVersionIdentifier()));
		if(moduleGroup.getMainClass() != null) {
			module.setMainClass(content(moduleGroup.getMainClass()));
		}
		for (PackageGroup aPackage : moduleGroup.getPackages()) {
			module.addPackage(content(aPackage.getPackageClass()));
		}
		for (RequireGroup require : moduleGroup.getRequires()) {
			Modifiers requireModifiers = new Modifiers();
			for (AccessModGroup accessMod : require.getAccessMods().getAccessMods())
				requireModifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
			ModuleRequire moduleRequire = new ModuleRequire(content(require.getModule()), requireModifiers);
			if(require.getVersion() != null) {
				moduleRequire.setVersion(content(require.getVersion().getVersionIdentifier()));
			}
			module.addRequire(moduleRequire);
		}
		for (ExportGroup export : moduleGroup.getExports()) {
			Modifiers exportModifiers = new Modifiers();
			for (AccessModGroup accessMod : export.getAccessMods().getAccessMods())
				exportModifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
			ModuleExport moduleExport = new ModuleExport(content(export.getModule()), exportModifiers);
			if(export.getTo() != null) {
				for (IdentifierGroup identifierGroup : export.getTo().getTo()) {
					moduleExport.addPackage(content(identifierGroup));
				}
			}
			module.addExport(moduleExport);
		}
		for (OpenGroup open : moduleGroup.getOpens()) {
			Modifiers openModifiers = new Modifiers();
			for (AccessModGroup accessMod : open.getAccessMods().getAccessMods())
				openModifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
			ModuleOpen moduleExport = new ModuleOpen(content(open.getModule()), openModifiers);
			if(open.getTo() != null) {
				for (IdentifierGroup identifierGroup : open.getTo().getTo()) {
					moduleExport.addPackage(content(identifierGroup));
				}
			}
			module.addOpen(moduleExport);
		}
		for (UseGroup use : moduleGroup.getUses()) {
			module.addUse(content(use.getService()));
		}
		for (ProvideGroup provide : moduleGroup.getProvides()) {
			ModuleProvide moduleProvide = new ModuleProvide(content(provide.getService()));
			if(provide.getWith() != null) {
				for (IdentifierGroup identifierGroup : provide.getWith().getWith()) {
					moduleProvide.addPackage(content(identifierGroup));
				}
			}
			module.addProvide(moduleProvide);
		}
		return module;
	}

	public static Record convertRecord(RecordGroup group) throws AssemblerException {
		Record record = new Record();
		for (RecordComponentGroup component : group.getComponents()) {
			RecordComponent recordComponent = new RecordComponent(content(component.getIdentifier()), content(component.getDescriptor()));
			for (AttributeGroup attribute : component.getAttributes()) {
				if(attribute instanceof AnnotationGroup) {
					recordComponent.addAnnotation(convertAnnotation((AnnotationGroup) attribute));
				} else if(attribute instanceof SignatureGroup) {
					recordComponent.setSignature(new Signature(content(((SignatureGroup) attribute).getDescriptor())));
				} else {
					throw new AssemblerException("Unknown attribute type: " + attribute.getClass().getSimpleName(), component.getStartLocation());
				}
			}
			record.addComponent(recordComponent);
		}
		return record;
	}

	public static Object convert(Group group) throws AssemblerException {
		if (group.isType(Group.GroupType.NUMBER)) {
			return ((NumberGroup) group).getNumber();
		} else if (group.isType(Group.GroupType.TYPE)) {
			TypeGroup typeGroup = (TypeGroup) group;
			try {
				String desc = typeGroup.getDescriptor().content();
				if (desc.isEmpty()) return Type.getType(desc);
				if (desc.charAt(0) == '(') {
					return Type.getMethodType(typeGroup.getDescriptor().content());
				} else {
					return Type.getObjectType(typeGroup.getDescriptor().content());
				}
			} catch (IllegalArgumentException e) {
				throw new AssemblerException("Invalid type: " + typeGroup.getDescriptor().content(),
						typeGroup.getStartLocation());
			}
		} else if (group.isType(Group.GroupType.HANDLE)) {
			HandleGroup handle = (HandleGroup) group;
			HandleInfo info = convertHandle(handle);
			return info.toHandle();
		} else if (group.isType(Group.GroupType.STRING)) {
			return group.content();
		} else {
			String content = group.content();
			if (content.length() == 3 && content.charAt(0) == '\'' && content.charAt(2) == '\'')
				return content.charAt(1);
			if (content.equals("true")) return true;
			if (content.equals("false")) return false;
			if (content.equals("null")) return null;
			if (content.equals("NaN")) return Double.NaN;
			if (content.equals("NaNf")) return Float.NaN;
			if (content.equals("Infinity")) return Double.POSITIVE_INFINITY;
			if (content.equals("-Infinity")) return Double.NEGATIVE_INFINITY;
			if (content.equals("Infinityf")) return Float.POSITIVE_INFINITY;
			if (content.equals("-Infinityf")) return Float.NEGATIVE_INFINITY;
			return content;
		}
	}

	public static ArgType argType(Group group) {
		if (group instanceof NumberGroup) {
			NumberGroup number = (NumberGroup) group;
			if (number.isFloat()) {
				return number.isWide() ? ArgType.DOUBLE : ArgType.FLOAT;
			} else {
				return number.isWide() ? ArgType.LONG : ArgType.INTEGER;
			}
		} else if (group instanceof StringGroup) {
			return ArgType.STRING;
		} else if (group instanceof TypeGroup) {
			return ArgType.TYPE;
		} else if (group instanceof HandleGroup) {
			return ArgType.HANDLE;
		} else if (group instanceof IdentifierGroup) {
			String content = group.content();
			if (content.length() == 3 && content.charAt(0) == '\'' && content.charAt(2) == '\'')
				return ArgType.CHAR;
			switch (content) {
				case "true":
				case "false":
					return ArgType.BOOLEAN;
				case "NaN":
				case "Infinity":
				case "-Infinity":
					return ArgType.DOUBLE;
				case "NaNf":
				case "Infinityf":
				case "-Infinityf":
					return ArgType.FLOAT;
				default:
					throw new IllegalArgumentException("Cannot convert to constant '" + group.content() + "'");
			}
		}
		throw new IllegalArgumentException("Cannot convert to constant '" + group.content() + "'");
	}


	private static void annotationParam(AnnotationParamGroup annotationParam, Map<String, Annotation.AnnoArg> map) throws AssemblerException {
		if (annotationParam.getParamValue().isType(Group.GroupType.ARGS)) {
			ArgsGroup args = (ArgsGroup) annotationParam.getParamValue();
			Map<String, Annotation.AnnoArg> argMap = new HashMap<>();
			for (Group group : args.getBody().getChildren()) {
				paramValue(group.content(), group, argMap);
			}
			map.put(annotationParam.getName().content(),
					wrap(annotationParam.getParamValue(), new Annotation.AnnoArg(
							ArgType.ANNO_LIST,
							new ArrayList<>(argMap.values())
					)));
		} else {
			paramValue(annotationParam.getName().content(), annotationParam.getParamValue(), map);
		}
	}

	private static void paramValue(String name, Group value, Map<String, Annotation.AnnoArg> map) throws AssemblerException {
		if (value.isType(Group.GroupType.ARGS)) {
			ArgsGroup args = (ArgsGroup) value;
			for (Group group : args.getBody().getChildren()) {
				paramValue(name, group, map);
			}
		} else if (value.isType(Group.GroupType.ENUM)) {
			EnumGroup enumGroup = (EnumGroup) value;
			map.put(name,
					wrap(value, new Annotation.AnnoEnum(
							enumGroup.getDescriptor().content(),
							enumGroup.getEnumValue().content()
					)));
		} else if (value.isType(Group.GroupType.ANNOTATION)) {
			AnnotationGroup annotationGroup = (AnnotationGroup) value;
			Map<String, Annotation.AnnoArg> argMap = new HashMap<>();
			for (AnnotationParamGroup param : annotationGroup.getParams()) {
				annotationParam(param, argMap);
			}
			map.put(name,
					wrap(value, new Annotation.AnnoArg(
							ArgType.ANNO,
							new Annotation(
									!annotationGroup.isInvisible(),
									annotationGroup.getClassGroup().content(),
									argMap
							))));
		} else {
			map.put(name,
					wrap(value, new Annotation.AnnoArg(
							argType(value),
							convert(value)
					)));
		}

	}

	public static <E extends BaseElement> E wrap(Group group, E element) {
		if (group == null)
			throw new IllegalArgumentException("Group must not be null!");
		Location startLocation = group.getStartLocation();
		if (startLocation == null)
			throw new IllegalArgumentException("Group start must not be null!");
		Location endLocation = group.getEndLocation();
		int startPos = startLocation.getStart();
		int endPos = endLocation.getPosition();
		int column = startLocation.getColumn();
		return element.setLine(startLocation.getLine())
				.setColumnRange(column, column + (endPos - startPos))
				.setRange(startPos, endPos);
	}

	public static String content(Group group) {
		String content = group.content();
		if(content.equals(EscapeUtil.EMPTY_PLACEHOLDER)) return "";
		return EscapeUtil.unescape(group.content());
	}
}
