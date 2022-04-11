package me.coley.recaf.ui.control.hex.clazz;

import me.coley.cafedude.classfile.AttributeConstants;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.annotation.*;
import me.coley.cafedude.classfile.attribute.*;

import java.util.List;
import java.util.Map;

import static me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType.*;

/**
 * Extracts offset information about an attribute.
 *
 * @author Matt Coley
 */
public class AttributeOffsetConsumer extends ClassOffsetConsumer implements AttributeConstants {
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
		info = new ClassOffsetInfo(cf, ATTRIBUTE_INFO, attribute, offset, offset + size - 1);
		consume(2, ATTRIBUTE_NAME_INDEX, attribute.getNameIndex());
		consume(4, ATTRIBUTE_LENGTH, attribute.computeInternalLength());
		if (attribute instanceof DefaultAttribute) {
			DefaultAttribute dflt = (DefaultAttribute) attribute;
			consume(dflt.getData().length, ATTRIBUTE_DATA_UNSUPPORTED, dflt.getData());
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
					consume(2, CODE_EXCEPTIONS_COUNT, impl.getExceptionTable().size());
					Wrapper exceptionsWrapper = new Wrapper(CODE_EXCEPTIONS);
					for (CodeAttribute.ExceptionTableEntry entry : impl.getExceptionTable()) {
						Wrapper exceptionWrapper = new Wrapper(CODE_EXCEPTION);
						exceptionWrapper.setValue(entry);
						exceptionWrapper.consume(2, EXCEPTION_START_PC, entry.getStartPc());
						exceptionWrapper.consume(2, EXCEPTION_END_PC, entry.getEndPc());
						exceptionWrapper.consume(2, EXCEPTION_HANDLER_PC, entry.getHandlerPc());
						exceptionWrapper.consume(2, EXCEPTION_TYPE_INDEX, entry.getCatchTypeIndex());
						exceptionsWrapper.add(exceptionWrapper.complete());
					}
					map.put(exceptionsWrapper.getStart(), exceptionsWrapper.complete());
					consume(2, CODE_ATTRIBUTE_COUNT, impl.getAttributes().size());
					Wrapper codeAttrWrapper = new Wrapper(CODE_ATTRIBUTES);
					for (Attribute subAttribute : impl.getAttributes()) {
						AttributeOffsetConsumer helper = new AttributeOffsetConsumer(offset, cf, subAttribute);
						codeAttrWrapper.add(helper.info);
						offset = helper.end();
					}
					map.put(codeAttrWrapper.getStart(), codeAttrWrapper.complete());
					break;
				}
				case CONSTANT_VALUE: {
					ConstantValueAttribute impl = (ConstantValueAttribute) attribute;
					consume(2, CONSTANT_VALUE_INDEX, impl.getConstantValueIndex());
					break;
				}
				case ANNOTATION_DEFAULT: {
					AnnotationDefaultAttribute impl = (AnnotationDefaultAttribute) attribute;
					ElementValue value = impl.getElementValue();
					consumeElementValue(value);
					break;
				}
				case RUNTIME_VISIBLE_ANNOTATIONS:
				case RUNTIME_VISIBLE_TYPE_ANNOTATIONS:
				case RUNTIME_INVISIBLE_ANNOTATIONS:
				case RUNTIME_INVISIBLE_TYPE_ANNOTATIONS: {
					AnnotationsAttribute impl = (AnnotationsAttribute) attribute;
					consume(2, ANNOTATIONS_COUNT, impl.getAnnotations().size());
					Wrapper annotationsWrapper = new Wrapper(ANNOTATIONS);
					for (Annotation annotation : impl.getAnnotations()) {
						annotationsWrapper.add(consumeAnnotation(annotation));
					}
					map.put(annotationsWrapper.getStart(), annotationsWrapper.complete());
					break;
				}
				case RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:
				case RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS: {
					ParameterAnnotationsAttribute impl = (ParameterAnnotationsAttribute) attribute;
					consume(1, PARAMETER_ANNOTATIONS_COUNT, impl.getParameterAnnotations().size());
					Wrapper annotationsWrapper = new Wrapper(PARAMETER_ANNOTATIONS);
					for (Map.Entry<Integer, List<Annotation>> entry : impl.getParameterAnnotations().entrySet()) {
						Wrapper arg = new Wrapper(PARAMETER_ANNOTATIONS_FOR_ARG);
						arg.setValue(entry.getKey());
						arg.consume(2, PARAMETER_ANNOTATIONS_COUNT_FOR_PARAM, entry.getValue().size());
						for (Annotation annotation : entry.getValue())
							arg.add(consumeAnnotation(annotation));
						annotationsWrapper.add(arg.complete());
					}
					map.put(annotationsWrapper.getStart(), annotationsWrapper.complete());
					break;
				}
				case BOOTSTRAP_METHODS: {
					BootstrapMethodsAttribute impl = (BootstrapMethodsAttribute) attribute;
					consume(2, BOOTSTRAP_METHODS_COUNT, impl.getBootstrapMethods().size());
					Wrapper bsmsWrapper = new Wrapper(ClassOffsetInfoType.BOOTSTRAP_METHODS);
					for (BootstrapMethodsAttribute.BootstrapMethod bm : impl.getBootstrapMethods()) {
						Wrapper bsmWrapper = new Wrapper(BOOTSTRAP_METHOD);
						bsmWrapper.setValue(bm);
						bsmWrapper.consume(2, BOOTSTRAP_METHOD_REF_INDEX, bm.getBsmMethodref());
						bsmWrapper.consume(2, BOOTSTRAP_METHOD_ARGS_COUNT, bm.getArgs().size());
						Wrapper argsWrapper = new Wrapper(BOOTSTRAP_METHOD_ARGS);
						for (int i : bm.getArgs()) {
							argsWrapper.consume(2, BOOTSTRAP_METHOD_ARG, i);
						}
						bsmWrapper.add(argsWrapper.complete());
						bsmsWrapper.add(bsmWrapper.complete());
					}
					map.put(bsmsWrapper.getStart(), bsmsWrapper.complete());
					break;
				}
				case ENCLOSING_METHOD: {
					EnclosingMethodAttribute impl = (EnclosingMethodAttribute) attribute;
					consume(2, ENCLOSING_METHOD_CLASS, impl.getClassIndex());
					consume(2, ENCLOSING_METHOD_METHOD, impl.getMethodIndex());
					break;
				}
				case INNER_CLASSES: {
					InnerClassesAttribute impl = (InnerClassesAttribute) attribute;
					consume(2, INNER_CLASSES_COUNT, impl.getInnerClasses().size());
					Wrapper innersWrapper = new Wrapper(ClassOffsetInfoType.INNER_CLASSES);
					for (InnerClassesAttribute.InnerClass inner : impl.getInnerClasses()) {
						Wrapper innerWrapper = new Wrapper(INNER_CLASS);
						innerWrapper.setValue(inner);
						innerWrapper.consume(2, INNER_CLASS_INNER_INFO, inner.getInnerClassInfoIndex());
						innerWrapper.consume(2, INNER_CLASS_OUTER_INFO, inner.getOuterClassInfoIndex());
						innerWrapper.consume(2, INNER_CLASS_INNER_NAME, inner.getInnerNameIndex());
						innerWrapper.consume(2, INNER_CLASS_INNER_ACCESS, inner.getInnerClassAccessFlags());
						innersWrapper.add(innerWrapper.complete());
					}
					map.put(innersWrapper.getStart(), innersWrapper.complete());
					break;
				}
				case NEST_HOST: {
					NestHostAttribute impl = (NestHostAttribute) attribute;
					consume(2, NEST_HOST_CLASS, impl.getHostClassIndex());
					break;
				}
				case DEPRECATED:
				case EXCEPTIONS:
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
				case NEST_MEMBERS:
				case RECORD:
				case PERMITTED_SUBCLASSES:
				case SIGNATURE:
				case SOURCE_DEBUG_EXTENSION:
				case SOURCE_FILE:
				case SOURCE_ID:
				case STACK_MAP_TABLE:
				case SYNTHETIC:
				default:
					// Generic handling
					map.put(offset, new ClassOffsetInfo(cf, ATTRIBUTE_DATA, attribute.getNameIndex(), offset, offset + attribute.computeInternalLength() - 1));
					break;
			}
		}
		map.values().forEach(i -> i.setParent(info));
		offset = startOffset + size;
	}

	private ClassOffsetInfo consumeElementValue(ElementValue value) {
		Wrapper elementWrapper = new Wrapper(ELEMENT_VALUE_INFO);
		elementWrapper.consume(1, ELEMENT_VALUE_TAG, value.getTag());
		switch (value.getTag()) {
			case '@': {
				AnnotationElementValue annoValue = (AnnotationElementValue) value;
				elementWrapper.add(consumeAnnotation(annoValue.getAnnotation()));
				break;
			}
			case 'c': {
				ClassElementValue classValue = (ClassElementValue) value;
				elementWrapper.consume(2, ELEMENT_VALUE_CLASS_INDEX, classValue.getClassIndex());
				break;
			}
			case 'e': {
				EnumElementValue enumValue = (EnumElementValue) value;
				elementWrapper.consume(2, ELEMENT_VALUE_ENUM_TYPE_INDEX, enumValue.getTypeIndex());
				elementWrapper.consume(2, ELEMENT_VALUE_ENUM_NAME_INDEX, enumValue.getNameIndex());
				break;
			}
			case 's': {
				Utf8ElementValue utfValue = (Utf8ElementValue) value;
				elementWrapper.consume(2, ELEMENT_VALUE_UTF_INDEX, utfValue.getUtfIndex());
				break;
			}
			case '[': {
				ArrayElementValue arrayValue = (ArrayElementValue) value;
				elementWrapper.consume(2, ELEMENT_VALUE_ARRAY_COUNT, arrayValue.getArray().size());
				Wrapper arrayWrapper = new Wrapper(ELEMENT_VALUE_ARRAY);
				for (ElementValue item : arrayValue.getArray()) {
					arrayWrapper.add(consumeElementValue(item));
				}
				elementWrapper.add(arrayWrapper.complete());
				break;
			}
			case 'B': // byte
			case 'C': // char
			case 'D': // double
			case 'F': // float
			case 'I': // int
			case 'J': // long
			case 'S': // short
			case 'Z': // boolean
			{
				PrimitiveElementValue primitiveValue = (PrimitiveElementValue) value;
				elementWrapper.consume(2, ELEMENT_VALUE_PRIMITIVE_INDEX, primitiveValue.getValueIndex());
				break;
			}
		}
		return elementWrapper.complete();
	}


	private ClassOffsetInfo consumeAnnotation(Annotation annotation) {
		Wrapper annoWrapper = new Wrapper(ANNOTATION);
		if (annotation instanceof TypeAnnotation) {
			TypeAnnotation typeAnnotation = (TypeAnnotation) annotation;
			annoWrapper.consume(1, ANNOTATION_TARGET_TYPE, annotation.getTypeIndex());
			// TODO: Parse the target-info and type-path
			int size = typeAnnotation.getTargetInfo().computeLength();
			annoWrapper.consume(size, ANNOTATION_TARGET_INFO, typeAnnotation.getTargetInfo());
			size = typeAnnotation.getTypePath().computeLength();
			annoWrapper.consume(size, ANNOTATION_TYPE_PATH, typeAnnotation.getTypePath());
		}
		annoWrapper.consume(2, ANNOTATION_TYPE_INDEX, annotation.getTypeIndex());
		annoWrapper.consume(2, ANNOTATION_VALUES_COUNT, annotation.getValues().size());
		// Consume value pairs
		Wrapper values = new Wrapper(ANNOTATION_VALUES);
		for (Map.Entry<Integer, ElementValue> entry : annotation.getValues().entrySet()) {
			values.consume(2, ANNOTATION_VALUE_KEY_NAME_INDEX, entry.getKey());
			values.add(consumeElementValue(entry.getValue()));
		}
		annoWrapper.add(values.complete());
		return annoWrapper.complete();
	}

	/**
	 * @return Attribute end offset.
	 */
	public int end() {
		return offset;
	}
}
