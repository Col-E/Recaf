package me.coley.recaf.ui.control.hex.clazz;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import me.coley.cafedude.classfile.ClassMember;
import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.ConstantPoolConstants;
import me.coley.cafedude.classfile.attribute.Attribute;
import me.coley.cafedude.classfile.attribute.BootstrapMethodsAttribute;
import me.coley.cafedude.classfile.attribute.CodeAttribute;
import me.coley.cafedude.classfile.attribute.InnerClassesAttribute;
import me.coley.cafedude.classfile.constant.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.hex.EditableHexLocation;
import me.coley.recaf.ui.control.hex.HexView;
import me.coley.recaf.ui.util.NodeUtil;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.TextDisplayUtil;

import java.util.Objects;
import java.util.stream.Collectors;

public class ClassInfoFormatter implements ConstantPoolConstants {
	public static GridPane format(HexView view, ClassOffsetInfo info) {
		Object value = info.getValue();
		Label title = new Label(info.getType().name());
		if (info.getType() == ClassOffsetInfoType.ATTRIBUTE_INFO) {
			Attribute attr = (Attribute) value;
			String name = info.getClassFile().getPool().getUtf(attr.getNameIndex());
			title.setText(title.getText() + ": " + name);
		} else if (info.getType().name().startsWith("CP_")) {
			ConstPoolEntry entry = (ConstPoolEntry) value;
			int index = info.getClassFile().getPool().indexOf(entry);
			title.setText(index + ": " + title.getText());
		}
		Label range = new Label(HexView.offsetStr(info.getStart()) + " - " + HexView.offsetStr(info.getEnd()));
		title.getStyleClass().add("b");
		title.getStyleClass().add("monospace");
		range.getStyleClass().add("monospace");
		int row = 0;
		GridPane content = new GridPane();
		content.addRow(row++, title);
		content.addRow(row++, range);
		content.setOnMouseEntered(e -> NodeUtil.addStyleClass(content, "hex-hover"));
		content.setOnMouseExited(e -> NodeUtil.removeStyleClass(content, "hex-hover"));
		content.setOnMouseClicked(e -> {
			view.selectRange(EditableHexLocation.RAW, info.getStart(), info.getEnd());
		});

		// If the user has opted to not show class file hints, we're done here.
		if (!Configs.editor().showClassHints) {
			return content;
		}

		// Most of the actual logic in this class is just showing hints... Everything beyond this point is for that.
		// Not everything is done because the base class library CAFED00D is not done. Its not hard to finish, its
		// just a lot of busy work.
		ConstPool cp = info.getClassFile().getPool();
		switch (info.getType()) {
			case MINOR_VERSION:
			case MAJOR_VERSION:
			case CONSTANT_POOL_COUNT:
			case INTERFACES_COUNT:
			case INTERFACE_INDEX:
			case FIELDS_COUNT:
			case METHODS_COUNT:
			case ATTRIBUTES_COUNT:
			case FIELD_ATTRIBUTES_COUNT:
			case METHOD_ATTRIBUTES_COUNT:
			case ATTRIBUTE_LENGTH:
			case CODE_MAX_STACK:
			case CODE_MAX_LOCALS:
			case CODE_LENGTH:
			case CODE_EXCEPTIONS_COUNT:
			case CODE_ATTRIBUTE_COUNT:
			case PARAMETER_ANNOTATIONS_FOR_ARG:
			case BOOTSTRAP_METHODS_COUNT:
			case EXCEPTION_START_PC:
			case EXCEPTION_END_PC:
			case EXCEPTION_HANDLER_PC:
			case EXCEPTION_TYPE_INDEX:
			case CONSTANT_VALUE_INDEX:
			case BOOTSTRAP_METHOD_REF_INDEX:
			case BOOTSTRAP_METHOD_ARGS_COUNT:
			case ELEMENT_VALUE_ARRAY_COUNT:
			case ELEMENT_VALUE_CLASS_INDEX:
			case ELEMENT_VALUE_ENUM_TYPE_INDEX:
			case ELEMENT_VALUE_ENUM_NAME_INDEX:
			case ELEMENT_VALUE_PRIMITIVE_INDEX:
			case ELEMENT_VALUE_UTF_INDEX:
			case ANNOTATIONS_COUNT:
			case ANNOTATION_TYPE_INDEX:
			case ANNOTATION_VALUES_COUNT:
			case ANNOTATION_VALUE_KEY_NAME_INDEX:
			case PARAMETER_ANNOTATIONS_COUNT:
			case PARAMETER_ANNOTATIONS_COUNT_FOR_PARAM:
			case INNER_CLASSES_COUNT:
			case INNER_CLASS_INNER_ACCESS: {
				content.addRow(row, dim(value));
				break;
			}
			case ACCESS_FLAGS:
			case FIELD_ACC_FLAGS:
			case METHOD_ACC_FLAGS: {
				int acc = (int) value;
				String flagNames = describeFlags(info.getType(), acc);
				content.addRow(row, dim(Integer.toString(acc, 2) + " -> " + flagNames));
				break;
			}
			case THIS_CLASS:
			case SUPER_CLASS:
			case ENCLOSING_METHOD_CLASS:
			case NEST_HOST_CLASS:
			case INNER_CLASS_INNER_INFO:  {
				int classIndex = (int) value;
				if (classIndex > 0) {
					CpClass cpClass = (CpClass) cp.get(classIndex);
					CpUtf8 cpClassName = (CpUtf8) cp.get(cpClass.getIndex());
					content.addRow(row, dim(classIndex + ": " + cpClassName.getText()));
				}
				break;
			}
			case FIELD_NAME_INDEX:
			case FIELD_DESC_INDEX:
			case METHOD_NAME_INDEX:
			case METHOD_DESC_INDEX:
			case ATTRIBUTE_NAME_INDEX: {
				int utfIndex = (int) value;
				content.addRow(row, dim(utfIndex + ": " + cp.getUtf(utfIndex)));
				break;
			}
			case INNER_CLASS_INNER_NAME: {
				int innerName = (int) value;
				if (innerName == 0) {
					content.addRow(row, dim(innerName + ": " + "(anonymous)"));
				} else {
					String display = cp.getUtf(innerName);
					content.addRow(row, dim(innerName + ": " + display));
				}
				break;
			}
			case INNER_CLASS_OUTER_INFO: {
				int outerIndex = (int) value;
				if (outerIndex == 0) {
					content.addRow(row, dim(outerIndex + ": " + "(anonymous)"));
				} else {
					CpClass nameType = (CpClass) cp.get(outerIndex);
					String display = cp.getUtf(nameType.getIndex());
					content.addRow(row, dim(outerIndex + ": " + display));
				}
				break;
			}
			case ENCLOSING_METHOD_METHOD: {
				int methodIndex = (int) value;
				if (methodIndex == 0) {
					content.addRow(row, dim(methodIndex + ": " + "(synthetic method scope)"));
				} else {
					CpNameType nameType = (CpNameType) cp.get(methodIndex);
					String display = cp.getUtf(nameType.getNameIndex()) + cp.getUtf(nameType.getTypeIndex());
					content.addRow(row, dim(methodIndex + ": " + display));
				}
				break;
			}
			case INNER_CLASS: {
				InnerClassesAttribute.InnerClass inner = (InnerClassesAttribute.InnerClass) value;
				CpClass cpInner = (CpClass) cp.get(inner.getInnerClassInfoIndex());
				content.addRow(row, dim(cp.getUtf(cpInner.getIndex())));
				break;
			}
			case CODE_BYTECODE: {
				// TODO: Bytecode ought to be "Open in bytecode view" or something
				break;
			}
			case CODE_EXCEPTION: {
				CodeAttribute.ExceptionTableEntry exception = (CodeAttribute.ExceptionTableEntry) value;
				CpClass cpClass = (CpClass) cp.get(exception.getCatchTypeIndex());
				String exceptionType = cp.getUtf(cpClass.getIndex());
				int start = exception.getStartPc();
				int end = exception.getEndPc();
				int handler = exception.getHandlerPc();
				content.addRow(row, dim(String.format("%s [%d - %d : %d]", exceptionType, start, end, handler)));
				break;
			}
			case FIELD_INFO:
			case METHOD_INFO: {
				boolean isMethod = info.getType() == ClassOffsetInfoType.METHOD_INFO;
				ClassMember member = (ClassMember) value;
				String name = cp.getUtf(member.getNameIndex());
				String desc = cp.getUtf(member.getTypeIndex());
				AccessFlag.Type type = isMethod ?
						AccessFlag.Type.METHOD : AccessFlag.Type.FIELD;
				String flagNames = describeFlags(type, member.getAccess());
				if (isMethod) {
					content.addRow(row, dim(String.format("%s %s%s", flagNames, name, desc)));
				} else {
					content.addRow(row, dim(String.format("%s %s %s", flagNames, desc, name)));
				}
				break;
			}
			case CP_UTF8:
			case CP_INTEGER:
			case CP_FLOAT:
			case CP_LONG:
			case CP_DOUBLE:
			case CP_CLASS:
			case CP_STRING:
			case CP_FIELD_REF:
			case CP_METHOD_REF:
			case CP_INTERFACE_METHOD_REF:
			case CP_NAME_TYPE:
			case CP_METHOD_HANDLE:
			case CP_METHOD_TYPE:
			case CP_DYNAMIC:
			case CP_INVOKE_DYNAMIC:
			case CP_MODULE:
			case CP_PACKAGE: {
				ConstPoolEntry entry = (ConstPoolEntry) value;
				content.addRow(row, dim(describeCpEntry(cp, entry)));
				break;
			}
			case BOOTSTRAP_METHOD_ARG: {
				int index = (int) value;
				ConstPoolEntry item = cp.get(index);
				String itemText = describeCpEntry(cp, item);
				content.addRow(row, dim(index + " - " + itemText));
			}
			case BOOTSTRAP_METHOD: {
				BootstrapMethodsAttribute.BootstrapMethod method = (BootstrapMethodsAttribute.BootstrapMethod) value;
				CpMethodHandle cpMethodHandle = (CpMethodHandle) cp.get(method.getBsmMethodref());
				String type = describeHandleKind(cpMethodHandle);
				String ref = describeReference(cp, (ConstRef) cp.get(cpMethodHandle.getReferenceIndex()));
				content.addRow(row, dim(type + " " + ref));
				break;
			}
			case BOOTSTRAP_METHOD_ARGS:
				break;
			case ELEMENT_VALUE_INFO:
			case ELEMENT_VALUE_TAG:
			case ELEMENT_VALUE_ARRAY:
			case ANNOTATIONS:
			case ANNOTATION:
			case ANNOTATION_TARGET_TYPE:
			case ANNOTATION_TARGET_INFO:
			case ANNOTATION_TYPE_PATH:
			case ANNOTATION_VALUES:
			case PARAMETER_ANNOTATIONS:
			default:
				break;
		}
		return content;
	}

	private static String describeReference(ConstPool cp, ConstRef ref) {
		CpClass cpClass = (CpClass) cp.get(ref.getClassIndex());
		CpNameType cpNameType = (CpNameType) cp.get(ref.getNameTypeIndex());
		String className = cp.getUtf(cpClass.getIndex());
		String name = cp.getUtf(cpNameType.getNameIndex());
		String desc = cp.getUtf(cpNameType.getTypeIndex());
		boolean isMethod = ref instanceof CpMethodRef;
		if (isMethod) {
			return String.format("%s.%s%s", className, name, desc);
		} else {
			return String.format("%s.%s %s", className, name, desc);
		}
	}


	private static String describeFlags(ClassOffsetInfoType offsetType, int access) {
		AccessFlag.Type type = AccessFlag.Type.CLASS;
		if (offsetType == ClassOffsetInfoType.FIELD_ACC_FLAGS) {
			type = AccessFlag.Type.FIELD;
		} else if (offsetType == ClassOffsetInfoType.METHOD_ACC_FLAGS) {
			type = AccessFlag.Type.METHOD;
		}
		return describeFlags(type, access);
	}

	private static String describeFlags(AccessFlag.Type type, int access) {
		return AccessFlag.getApplicableFlags(type, access).stream()
				.map(AccessFlag::getName)
				.collect(Collectors.joining(" "));
	}

	private static String describeHandleKind(CpMethodHandle handle) {
		switch (handle.getKind()) {
			case 1:
				return "GetField";
			case 2:
				return "GetStatic";
			case 3:
				return "PutField";
			case 4:
				return "PutStatic";
			case 5:
				return "InvokeVirtual";
			case 6:
				return "InvokeStatic";
			case 7:
				return "InvokeSpecial";
			case 8:
				return "NewInvokeSpecial";
			case 9:
				return "InvokeInterface";
			default:
				return "<?>";
		}
	}

	private static String describeCpEntry(ConstPool cp, ConstPoolEntry item) {
		String itemText = "?";
		switch (item.getTag()) {
			case UTF8: {
				itemText = "utf8: " + ((CpUtf8) item).getText();
				break;
			}
			case INTEGER: {
				itemText = "int: " + ((CpInt) item).getValue();
				break;
			}
			case FLOAT: {
				itemText = "float: " + ((CpFloat) item).getValue();
				break;
			}
			case LONG: {
				itemText = "long: " + ((CpLong) item).getValue();
				break;
			}
			case DOUBLE: {
				itemText = "double: " + ((CpDouble) item).getValue();
				break;
			}
			case CLASS: {
				itemText = "class: " + cp.getUtf(((CpClass) item).getIndex());
				break;
			}
			case STRING: {
				String text = cp.getUtf(((CpString) item).getIndex());
				itemText = "string: \"" + text + "\"";
				break;
			}
			case METHOD_HANDLE: {
				CpMethodHandle handle = (CpMethodHandle) item;
				ConstRef ref = (ConstRef) cp.get(handle.getReferenceIndex());
				itemText = "method-handle: " + describeReference(cp, ref);
				break;
			}
			case METHOD_TYPE: {
				CpMethodType methodType = (CpMethodType) item;
				itemText = "method-type: " + cp.getUtf(methodType.getIndex());
				break;
			}
			case INVOKE_DYNAMIC: {
				CpInvokeDynamic invokeDynamic = (CpInvokeDynamic) item;
				CpNameType nameType = (CpNameType) cp.get(invokeDynamic.getNameTypeIndex());
				itemText = "invoke-dynamic: BSM:" + invokeDynamic.getBsmIndex() + " - " + describeCpEntry(cp, nameType);
				break;
			}
			case DYNAMIC: {
				CpDynamic dynamic = (CpDynamic) item;
				CpNameType nameType = (CpNameType) cp.get(dynamic.getNameTypeIndex());
				itemText = "dynamic: BSM:" + dynamic.getBsmIndex() + " - " + describeCpEntry(cp, nameType);
				break;
			}
			case FIELD_REF: {
				itemText = "field-ref: " + describeReference(cp, (ConstRef) item);
				break;
			}
			case METHOD_REF: {
				itemText = "method-ref: " + describeReference(cp, (ConstRef) item);
				break;
			}
			case INTERFACE_METHOD_REF: {
				itemText = "interface-method-ref: " + describeReference(cp, (ConstRef) item);
				break;
			}
			case NAME_TYPE: {
				CpNameType nameType = (CpNameType) item;
				String name = cp.getUtf(nameType.getNameIndex());
				String type = cp.getUtf(nameType.getTypeIndex());
				if (type.charAt(0) == '(') {
					itemText = "name-type: " + name + type;
				} else {
					itemText = "name-type: " + name + " " + type;
				}
				break;
			}
			case MODULE: {
				CpModule module = (CpModule) item;
				String name = cp.getUtf(module.getIndex());
				itemText = "module: " + name;
				break;
			}
			case PACKAGE: {
				CpPackage pack = (CpPackage) item;
				String name = cp.getUtf(pack.getIndex());
				itemText = "package: " + name;
				break;
			}
		}
		return EscapeUtil.unescapeUnicode(itemText);
	}

	private static Node dim(Object value) {
		String text = TextDisplayUtil.shortenEscapeLimit(Objects.toString(value));
		Label label = new Label(text);
		label.getStyleClass().add("monospace");
		label.getStyleClass().add("faint");
		return label;
	}
}
