package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.util.NumberUtil;
import me.coley.recaf.util.Types;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * A simpler stack analysis tool for methods defined by {@link Unit}.
 *
 * @author Matt Coley
 */
public class Analyzer {
	private static final Logger logger = Logging.get(Analyzer.class);
	private final String selfType;
	private final Unit unit;
	private final Code code;
	private ExpressionToAstTransformer expressionToAstTransformer;
	private InheritanceChecker inheritanceChecker;

	/**
	 * @param selfType
	 * 		Internal name of class defining the method.
	 * @param unit
	 * 		The unit containing the method code.
	 */
	public Analyzer(String selfType, Unit unit) {
		this.selfType = selfType;
		this.unit = unit;
		code = Objects.requireNonNull(unit.getCode(), "AST Unit must define a code region!");
	}

	/**
	 * @param expressionToAstTransformer
	 * 		Transformer to convert expressions into AST.
	 * 		This allows expressions to be properly analyzed.
	 */
	public void setExpressionToAstTransformer(ExpressionToAstTransformer expressionToAstTransformer) {
		this.expressionToAstTransformer = expressionToAstTransformer;
	}

	/**
	 * @return Lookup for child-parent relations between classes.
	 */
	public InheritanceChecker getInheritanceChecker() {
		return inheritanceChecker;
	}

	/**
	 * @param inheritanceChecker
	 * 		Lookup for child-parent relations between classes.
	 */
	public void setInheritanceChecker(InheritanceChecker inheritanceChecker) {
		this.inheritanceChecker = inheritanceChecker;
	}

	/**
	 * @return Wrapper of analysis information.
	 *
	 * @throws AstException
	 * 		When the AST cannot resolve all required references <i>(missing label, etc)</i>.
	 */
	public Analysis analyze() throws AstException {
		List<AbstractInstruction> instructions = code.getChildrenOfType(AbstractInstruction.class);
		Analysis analysis = new Analysis(instructions.size());
		fillBlocks(analysis, instructions);
		fillFrames(analysis, instructions);
		return analysis;
	}

	private void fillFrames(Analysis analysis, List<AbstractInstruction> instructions) throws AstException {
		// Initialize with method definition parameters
		Frame entryFrame = analysis.frame(0);
		entryFrame.initialize(selfType, (MethodDefinition) unit.getDefinition());
		// Visit the handler block of all try-catches
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new IllegalAstException(tryCatch, "No associated handler label");
			int handlerIndex = instructions.indexOf(handlerLabel);
			branch(analysis, instructions, Integer.MIN_VALUE, handlerIndex);
		}
		// Visit each instruction
		branch(analysis, instructions, -1, 0);
	}

	private void branch(Analysis analysis, List<AbstractInstruction> instructions, int ctxPc, int initialPc) throws AstException {
		int maxPc = instructions.size();
		int pc = initialPc;
		while (pc < maxPc) {
			AbstractInstruction instruction = instructions.get(pc);
			if (execute(analysis, instructions, ctxPc, pc, instruction)) {
				ctxPc = pc;
				pc++;
			} else {
				break;
			}
		}
	}

	private boolean execute(Analysis analysis, List<AbstractInstruction> instructions,
							int ctxPc, int pc, AbstractInstruction instruction) throws AstException {
		Frame frame = analysis.frame(pc);
		Frame oldFrameState = frame.copy();
		// Mark as visited
		boolean wasVisited = frame.markVisited();
		if (ctxPc >= 0) {
			// Need to populate frame from prior state if we've not already done so
			Frame priorFrame = analysis.frame(ctxPc);
			frame.copy(priorFrame);
		}
		// Handle flow control
		boolean continueExec = true;
		List<Label> flowDestinations = new ArrayList<>();
		if (instruction instanceof FlowControl) {
			FlowControl flow = (FlowControl) instruction;
			for (Label label : flow.getTargets(code.getLabels())) {
				if (!flowDestinations.contains(label))
					flowDestinations.add(label);
			}
			// Visit next PC if flow is not forced
			// Examples of forced flow:
			//  - GOTO
			//  - TABLE/LOOKUP-SWITCH
			continueExec = !flow.isForced();
		} else {
			// X-RETURN and ATHROW end execution of the current block.
			int op = instruction.getOpcodeVal();
			if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)
				continueExec = false;
			else if (op == Opcodes.ATHROW)
				continueExec = false;
		}
		for (Label flowDestination : flowDestinations) {
			int labelPc = instructions.indexOf(flowDestination);
			branch(analysis, instructions, pc, labelPc);
		}
		// Handle stack
		if (instruction instanceof Expression) {
			// Ensure the analyzer supports expression unrolling
			if (expressionToAstTransformer == null)
				throw new IllegalAstException(instruction, "Expression transformer not supplied!");
			// When the PC hits an expression, jump into the converted AST
			try {
				Code code = expressionToAstTransformer.transform((Expression) instruction);
				// TODO: I think we can have this call analyze and then copy the frame data from the
				//       final results into this frame.
				//        - Unless the user does 'EXPR throw new Error();' the stack before/after EXPR should be the same
				//        - Mostly just need to assert variable types are consistent, and if values are being tracked
				//          the values get updated (unless 2+ paths exist, then stop tracking due to unknown state)
			} catch (Exception ex) {
				throw new IllegalAstException(instruction, ex);
			}
		} else {
			int op = instruction.getOpcodeVal();
			switch (op) {
				case GOTO:
				case NOP:
					break;
				case ACONST_NULL:
					frame.push(new Value.NullValue());
					break;
				case ICONST_M1:
					frame.push(new Value.NumericValue(INT_TYPE, -1));
					break;
				case ICONST_0:
					frame.push(new Value.NumericValue(INT_TYPE, 0));
					break;
				case ICONST_1:
					frame.push(new Value.NumericValue(INT_TYPE, 1));
					break;
				case ICONST_2:
					frame.push(new Value.NumericValue(INT_TYPE, 2));
					break;
				case ICONST_3:
					frame.push(new Value.NumericValue(INT_TYPE, 3));
					break;
				case ICONST_4:
					frame.push(new Value.NumericValue(INT_TYPE, 4));
					break;
				case ICONST_5:
					frame.push(new Value.NumericValue(INT_TYPE, 5));
					break;
				case LCONST_0:
					frame.push(new Value.NumericValue(LONG_TYPE, 0L));
					break;
				case LCONST_1:
					frame.push(new Value.NumericValue(LONG_TYPE, 1L));
					break;
				case FCONST_0:
					frame.push(new Value.NumericValue(FLOAT_TYPE, 0F));
					break;
				case FCONST_1:
					frame.push(new Value.NumericValue(FLOAT_TYPE, 1F));
					break;
				case FCONST_2:
					frame.push(new Value.NumericValue(FLOAT_TYPE, 2F));
					break;
				case DCONST_0:
					frame.push(new Value.NumericValue(DOUBLE_TYPE, 0.0));
					break;
				case DCONST_1:
					frame.push(new Value.NumericValue(DOUBLE_TYPE, 1.0));
					break;
				case BIPUSH:
				case SIPUSH:
					IntInstruction intInstruction = (IntInstruction) instruction;
					frame.push(new Value.NumericValue(INT_TYPE, intInstruction.getValue()));
					break;
				case LDC:
					LdcInstruction ldcInstruction = (LdcInstruction) instruction;
					switch (ldcInstruction.getValueType()) {
						case TYPE:
							frame.push(new Value.TypeValue((Type) ldcInstruction.getValue()));
							break;
						case STRING:
							frame.push(new Value.StringValue((String) ldcInstruction.getValue()));
							break;
						case INTEGER:
							frame.push(new Value.NumericValue(INT_TYPE, (Integer) ldcInstruction.getValue()));
							break;
						case FLOAT:
							frame.push(new Value.NumericValue(FLOAT_TYPE, (Float) ldcInstruction.getValue()));
							break;
						case DOUBLE:
							frame.push(new Value.NumericValue(DOUBLE_TYPE, (Double) ldcInstruction.getValue()));
							frame.push(new Value.WideReservedValue());
							break;
						case LONG:
							frame.push(new Value.NumericValue(LONG_TYPE, (Long) ldcInstruction.getValue()));
							frame.push(new Value.WideReservedValue());
							break;
						case HANDLE:
							frame.push(new Value.HandleValue((HandleInfo) ldcInstruction.getValue()));
							break;
						case ANNO:
						case ANNO_LIST:
						case ANNO_ENUM:
							throw new IllegalAstException(ldcInstruction, "LDC should not contain annotation values!");
					}
					break;
				case IINC: {
					IincInstruction iincInstruction = (IincInstruction) instruction;
					String varName = iincInstruction.getName();
					Value value = frame.getLocal(varName);
					// Only update the variable if we're tracking the local's exact numeric value
					if (value instanceof Value.NumericValue) {
						Number numValue = ((Value.NumericValue) value).getNumber();
						if (numValue != null) {
							int incr = iincInstruction.getIncrement();
							frame.setLocal(varName, new Value.NumericValue(INT_TYPE, NumberUtil.add(numValue, incr)));
						}
					}
					break;
				}
				case ISTORE:
				case LSTORE:
				case FSTORE:
				case DSTORE:
				case ASTORE: {
					boolean isWideStore = op == DSTORE || op == LSTORE;
					VarInstruction varInstruction = (VarInstruction) instruction;
					String name = varInstruction.getName();
					Value value = isWideStore ? frame.popWide() : frame.pop();
					frame.setLocal(name, value);
					break;
				}
				case ILOAD:
				case LLOAD:
				case FLOAD:
				case DLOAD:
				case ALOAD: {
					boolean isWideLoad = op == DLOAD || op == LLOAD;
					VarInstruction varInstruction = (VarInstruction) instruction;
					String name = varInstruction.getName();
					Value value = Objects.requireNonNull(frame.getLocal(name), "Variable '" + name + "' is undefined!");
					frame.push(value);
					if (isWideLoad) {
						frame.push(new Value.WideReservedValue());
					}
					break;
				}
				case NEWARRAY: {
					// Get array type
					NewArrayInstruction newArrayInstruction = (NewArrayInstruction) instruction;
					Type type = Type.getType(String.valueOf(newArrayInstruction.getArrayType()));
					// Get array size, if possible
					Value.ArrayValue arrayValue;
					Value stackTop = frame.pop();
					if (stackTop instanceof Value.NumericValue) {
						Number size = ((Value.NumericValue) stackTop).getNumber();
						if (size == null) {
							arrayValue = new Value.ArrayValue(type);
						} else {
							arrayValue = new Value.ArrayValue(size.intValue(), type);
						}
					} else {
						// Unknown size due to non-numeric value
						arrayValue = new Value.ArrayValue(type);
						frame.markWonky();
					}
					frame.push(arrayValue);
					break;
				}
				case ANEWARRAY: {
					// Get array type
					TypeInstruction newArrayInstruction = (TypeInstruction) instruction;
					Type type = Type.getObjectType(newArrayInstruction.getType());
					// Get array size, if possible
					Value.ArrayValue arrayValue;
					Value stackTop = frame.pop();
					if (stackTop instanceof Value.NumericValue) {
						Number size = ((Value.NumericValue) stackTop).getNumber();
						if (size == null) {
							arrayValue = new Value.ArrayValue(type);
						} else {
							arrayValue = new Value.ArrayValue(size.intValue(), type);
						}
					} else {
						// Unknown size due to non-numeric value
						arrayValue = new Value.ArrayValue(type);
						frame.markWonky();
					}
					frame.push(arrayValue);
					break;
				}
				case MULTIANEWARRAY: {
					MultiArrayInstruction newArrayInstruction = (MultiArrayInstruction) instruction;
					Type type = Type.getType(newArrayInstruction.getDesc()).getElementType();
					int numDimensions = newArrayInstruction.getDimensions();
					// Create N-Dimensional array
					Value.ArrayValue arrayValue = new Value.ArrayValue(numDimensions, type);
					// Attempt to create correctly-sized sub-arrays from stack
					Value[] backingArray = arrayValue.getArray();
					for (int i = 0; i < numDimensions; i++) {
						Value stackTop = frame.pop();
						if (stackTop instanceof Value.NumericValue) {
							Number size = ((Value.NumericValue) stackTop).getNumber();
							if (size == null) {
								backingArray[i] = new Value.ArrayValue(type);
							} else {
								backingArray[i] = new Value.ArrayValue(size.intValue(), type);
							}
						} else {
							// Unknown size due to non-numeric value
							backingArray[i] = new Value.ArrayValue(type);
							frame.markWonky();
						}
					}
					frame.push(arrayValue);
					break;
				}
				case ARRAYLENGTH: {
					// Get array size if possible
					Value stackTop = frame.pop();
					Value.NumericValue length;
					if (stackTop instanceof Value.ArrayValue) {
						Value[] array = ((Value.ArrayValue) stackTop).getArray();
						if (array != null) {
							length = new Value.NumericValue(INT_TYPE, array.length);
						} else {
							length = new Value.NumericValue(INT_TYPE);
						}
					} else {
						// Unknown length due to non-array value
						length = new Value.NumericValue(INT_TYPE);
						frame.markWonky();
					}
					frame.push(length);
					break;
				}
				case IASTORE:
				case LASTORE:
				case FASTORE:
				case DASTORE:
				case AASTORE:
				case BASTORE:
				case CASTORE:
				case SASTORE: {
					Type type = Types.fromArrayOpcode(op);
					// Stack order (top-bottom): value, index, arrayref
					Value value = Types.isWide(type) ? frame.popWide() : frame.pop();
					Value index = frame.pop();
					Value array = frame.pop();
					if (array instanceof Value.ArrayValue && index instanceof Value.NumericValue) {
						// Check if we were able to track array size beforehand
						Value[] backingArray = ((Value.ArrayValue) array).getArray();
						if (backingArray != null) {
							// Check if we know the actual index
							Value.NumericValue arrayIndex = (Value.NumericValue) index;
							if (arrayIndex.getNumber() != null) {
								// Update the array
								int idx = arrayIndex.getNumber().intValue();
								backingArray[idx] = value;
							}
						}
					} else {
						// Wrong stack value types
						frame.markWonky();
					}
					break;
				}
				case IALOAD:
				case LALOAD:
				case FALOAD:
				case DALOAD:
				case AALOAD:
				case BALOAD:
				case CALOAD:
				case SALOAD: {
					Type fallback = Types.fromArrayOpcode(op);
					Value index = frame.pop();
					Value array = frame.pop();
					if (array instanceof Value.ArrayValue && index instanceof Value.NumericValue) {
						// Check if we were able to track array size beforehand
						Value[] backingArray = ((Value.ArrayValue) array).getArray();
						if (backingArray != null) {
							// Check if we know the actual index
							Value.NumericValue arrayIndex = (Value.NumericValue) index;
							if (arrayIndex.getNumber() != null) {
								// Push the real value if possible
								//  - remove fallback so that we don't double-push
								int idx = arrayIndex.getNumber().intValue();
								if (idx >= 0 && idx < backingArray.length) {
									Value value = backingArray[idx];
									if (value != null) {
										fallback = null;
										frame.push(value);
										// Check for wide types
										if (value instanceof Value.NumericValue) {
											if (Types.isWide(((Value.NumericValue) value).getType())) {
												frame.push(new Value.WideReservedValue());
											}
										}
									}
								}
							}
						}
					} else {
						// Wrong stack value types
						frame.markWonky();
					}
					// If the fallback hasn't been unset, push it to the stack
					if (fallback != null) {
						if (fallback.getSort() <= Type.FLOAT) {
							frame.push(new Value.NumericValue(fallback));
						} else if (fallback.getSort() <= Type.DOUBLE) {
							frame.push(new Value.NumericValue(fallback));
							frame.push(new Value.WideReservedValue());
						} else {
							frame.push(new Value.ObjectValue(fallback));
						}
					}
					break;
				}
				case POP:
					frame.pop();
					break;
				case POP2:
					frame.popWide();
					break;
				case DUP:
					frame.push(frame.peek());
					break;
				case DUP_X1: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					frame.push(value1);
					frame.push(value2);
					frame.push(value1);
					break;
				}
				case DUP_X2: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					Value value3 = frame.pop();
					frame.push(value1);
					frame.push(value3);
					frame.push(value2);
					frame.push(value1);
					break;
				}
				case DUP2: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					frame.push(value2);
					frame.push(value1);
					frame.push(value2);
					frame.push(value1);
					break;
				}
				case DUP2_X1: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					Value value3 = frame.pop();
					frame.push(value2);
					frame.push(value1);
					frame.push(value3);
					frame.push(value2);
					frame.push(value1);
					break;
				}
				case DUP2_X2: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					Value value3 = frame.pop();
					Value value4 = frame.pop();
					frame.push(value2);
					frame.push(value1);
					frame.push(value4);
					frame.push(value3);
					frame.push(value2);
					frame.push(value1);
					break;
				}
				case SWAP: {
					Value value1 = frame.pop();
					Value value2 = frame.pop();
					frame.push(value1);
					frame.push(value2);
					break;
				}
				case IADD:
					binaryOp(frame, INT_TYPE, NumberUtil::add);
					break;
				case LADD:
					binaryOp(frame, LONG_TYPE, NumberUtil::add);
					break;
				case FADD:
					binaryOp(frame, FLOAT_TYPE, NumberUtil::add);
					break;
				case DADD:
					binaryOp(frame, DOUBLE_TYPE, NumberUtil::add);
					break;
				case ISUB:
					binaryOp(frame, INT_TYPE, NumberUtil::sub);
					break;
				case LSUB:
					binaryOp(frame, LONG_TYPE, NumberUtil::sub);
					break;
				case FSUB:
					binaryOp(frame, FLOAT_TYPE, NumberUtil::sub);
					break;
				case DSUB:
					binaryOp(frame, DOUBLE_TYPE, NumberUtil::sub);
					break;
				case IMUL:
					binaryOp(frame, INT_TYPE, NumberUtil::mul);
					break;
				case LMUL:
					binaryOp(frame, LONG_TYPE, NumberUtil::mul);
					break;
				case FMUL:
					binaryOp(frame, FLOAT_TYPE, NumberUtil::mul);
					break;
				case DMUL:
					binaryOp(frame, DOUBLE_TYPE, NumberUtil::mul);
					break;
				case IDIV:
					binaryOp(frame, INT_TYPE, NumberUtil::div);
					break;
				case LDIV:
					binaryOp(frame, LONG_TYPE, NumberUtil::div);
					break;
				case FDIV:
					binaryOp(frame, FLOAT_TYPE, NumberUtil::div);
					break;
				case DDIV:
					binaryOp(frame, DOUBLE_TYPE, NumberUtil::div);
					break;
				case IREM:
					binaryOp(frame, INT_TYPE, NumberUtil::rem);
					break;
				case LREM:
					binaryOp(frame, LONG_TYPE, NumberUtil::rem);
					break;
				case FREM:
					binaryOp(frame, FLOAT_TYPE, NumberUtil::rem);
					break;
				case DREM:
					binaryOp(frame, DOUBLE_TYPE, NumberUtil::rem);
					break;
				case IAND:
					binaryOp(frame, INT_TYPE, NumberUtil::and);
					break;
				case LAND:
					binaryOp(frame, LONG_TYPE, NumberUtil::and);
					break;
				case IOR:
					binaryOp(frame, INT_TYPE, NumberUtil::or);
					break;
				case LOR:
					binaryOp(frame, LONG_TYPE, NumberUtil::or);
					break;
				case IXOR:
					binaryOp(frame, INT_TYPE, NumberUtil::xor);
					break;
				case LXOR:
					binaryOp(frame, LONG_TYPE, NumberUtil::xor);
					break;
				case ISHL:
					binaryOp(frame, INT_TYPE, NumberUtil::shiftLeft);
					break;
				case LSHL:
					binaryOp(frame, LONG_TYPE, NumberUtil::shiftLeft);
					break;
				case ISHR:
					binaryOp(frame, INT_TYPE, NumberUtil::shiftRight);
					break;
				case LSHR:
					binaryOp(frame, LONG_TYPE, NumberUtil::shiftRight);
					break;
				case IUSHR:
					binaryOp(frame, INT_TYPE, NumberUtil::shiftRightU);
					break;
				case LUSHR:
					binaryOp(frame, LONG_TYPE, NumberUtil::shiftRightU);
					break;
				case LCMP:
					binaryOp(frame, LONG_TYPE, NumberUtil::cmp);
					break;
				case INEG:
					unaryOp(frame, INT_TYPE, NumberUtil::neg);
					break;
				case LNEG:
					unaryOp(frame, LONG_TYPE, NumberUtil::neg);
					break;
				case FNEG:
					unaryOp(frame, FLOAT_TYPE, NumberUtil::neg);
					break;
				case DNEG:
					unaryOp(frame, DOUBLE_TYPE, NumberUtil::neg);
					break;
				case D2L:
				case F2L:
				case I2L:
					unaryOp(frame, LONG_TYPE, Number::longValue);
					break;
				case D2F:
				case L2F:
				case I2F:
					unaryOp(frame, FLOAT_TYPE, Number::floatValue);
					break;
				case F2D:
				case L2D:
				case I2D:
					unaryOp(frame, DOUBLE_TYPE, Number::doubleValue);
					break;
				case L2I:
				case F2I:
				case D2I:
					unaryOp(frame, INT_TYPE, Number::intValue);
					break;
				case I2B:
					unaryOp(frame, INT_TYPE, n -> (byte) n.intValue());
					break;
				case I2C:
					unaryOp(frame, INT_TYPE, n -> (int) (char) n.intValue());
					break;
				case I2S:
					unaryOp(frame, INT_TYPE, n -> (short) n.intValue());
					break;
				case FCMPL:
					binaryOp(frame, INT_TYPE, (n1, n2) -> {
						if (Float.isNaN(n1.floatValue())) return -1;
						return NumberUtil.cmp(n1, n2);
					});
					break;
				case FCMPG:
					binaryOp(frame, INT_TYPE, (n1, n2) -> {
						if (Float.isNaN(n1.floatValue())) return 1;
						return NumberUtil.cmp(n1, n2);
					});
					break;
				case DCMPL:
					binaryOp(frame, INT_TYPE, (n1, n2) -> {
						if (Double.isNaN(n1.doubleValue())) return -1;
						return NumberUtil.cmp(n1, n2);
					});
					break;
				case DCMPG:
					binaryOp(frame, INT_TYPE, (n1, n2) -> {
						if (Double.isNaN(n1.doubleValue())) return 1;
						return NumberUtil.cmp(n1, n2);
					});
					break;
				case IFEQ:
				case IFNE:
				case IFLT:
				case IFGE:
				case IFGT:
				case IFLE:
				case IFNULL:
				case IFNONNULL:
					// TODO: Mark jump as always (not)-taken if top value meets expectation
					frame.pop();
					break;
				case IF_ICMPEQ:
				case IF_ICMPNE:
				case IF_ICMPLT:
				case IF_ICMPGE:
				case IF_ICMPGT:
				case IF_ICMPLE:
				case IF_ACMPEQ:
				case IF_ACMPNE:
					// TODO: Mark jump as always (not)-taken if top value meets expectation
					frame.popWide();
					break;
				case TABLESWITCH:
				case LOOKUPSWITCH:
				case IRETURN:
				case FRETURN:
				case ARETURN:
					frame.pop();
					break;
				case DRETURN:
				case LRETURN:
					frame.popWide();
					break;
				case RETURN:
					break;
				case MONITORENTER:
				case MONITOREXIT:
					frame.pop();
					break;
				case ATHROW:
					// Top of stack is an object ref to an exception
					frame.pop();
					// Throwing clears the stack
					while (!frame.getStack().isEmpty())
						frame.pop();
					break;
				case NEW: {
					TypeInstruction typeInstruction = (TypeInstruction) instruction;
					frame.push(new Value.ObjectValue(Type.getObjectType(typeInstruction.getType())));
					break;
				}
				case CHECKCAST: {
					// Replace top stack value with cast type. Otherwise, it's a ClassCastException.
					TypeInstruction typeInstruction = (TypeInstruction) instruction;
					frame.pop();
					frame.push(new Value.ObjectValue(Type.getType(typeInstruction.getType())));
					break;
				}
				case INSTANCEOF: {
					Value value = frame.pop();
					if (value instanceof Value.ObjectValue) {
						// TODO: We can have a type-checker to know for certain if the check is redundant
						frame.push(new Value.NumericValue(INT_TYPE));
					} else {
						// Shouldn't be instance checking any non object ref
						frame.markWonky();
						frame.push(new Value.NumericValue(INT_TYPE));
					}
					break;
				}
				case GETFIELD:
					// Pop field owner ctx
					frame.pop();
					// Fall through
				case GETSTATIC: {
					// Push field value
					FieldInstruction fieldInstruction = (FieldInstruction) instruction;
					String desc = fieldInstruction.getDesc();
					Type type = Type.getType(desc);
					if (type.getSort() <= Type.DOUBLE) {
						frame.push(new Value.NumericValue(type));
						if (Types.isWide(type))
							frame.push(new Value.WideReservedValue());
					} else if (type.getSort() == ARRAY) {
						frame.push(new Value.ArrayValue(type.getElementType()));
					} else {
						frame.push(new Value.ObjectValue(type));
					}
					break;
				}
				case PUTSTATIC: {
					// Pop value
					FieldInstruction fieldInstruction = (FieldInstruction) instruction;
					Type type = Type.getType(fieldInstruction.getDesc());
					if (Types.isWide(type))
						frame.popWide();
					else
						frame.pop();
					break;
				}
				case PUTFIELD: {
					// Pop value
					FieldInstruction fieldInstruction = (FieldInstruction) instruction;
					Type type = Type.getType(fieldInstruction.getDesc());
					if (Types.isWide(type))
						frame.popWide();
					else
						frame.pop();
					// Pop field owner context
					frame.pop();
					break;
				}
				case INVOKESTATIC:
					// Pop method owner ctx
					frame.pop();
					// Fall through
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE: {
					// Pop arguments off stack and push method return value
					MethodInstruction methodInstruction = (MethodInstruction) instruction;
					String desc = methodInstruction.getDesc();
					Type type = Type.getMethodType(desc);
					Type[] argTypes = type.getArgumentTypes();
					for (int i = argTypes.length - 1; i > 0; i--) {
						// Iterating backwards so arguments are popped off stack in correct order.
						if (Types.isWide(argTypes[i]))
							frame.popWide();
						else
							frame.pop();
					}
					Type retType = type.getReturnType();
					if (retType.getSort() <= Type.DOUBLE) {
						frame.push(new Value.NumericValue(retType));
						if (Types.isWide(retType))
							frame.push(new Value.WideReservedValue());
					} else if (retType.getSort() == ARRAY) {
						frame.push(new Value.ArrayValue(retType.getElementType()));
					} else {
						frame.push(new Value.ObjectValue(retType));
					}
					break;
				}
				case INVOKEDYNAMIC:
					// Same handling as an invoke-static
					IndyInstruction indyInstruction = (IndyInstruction) instruction;
					String desc = indyInstruction.getDesc();
					Type type = Type.getMethodType(desc);
					Type[] argTypes = type.getArgumentTypes();
					for (int i = argTypes.length - 1; i > 0; i--) {
						// Iterating backwards so arguments are popped off stack in correct order.
						if (Types.isWide(argTypes[i]))
							frame.popWide();
						else
							frame.pop();
					}
					Type retType = type.getReturnType();
					if (retType.getSort() <= Type.DOUBLE) {
						frame.push(new Value.NumericValue(retType));
						if (Types.isWide(retType))
							frame.push(new Value.WideReservedValue());
					} else if (retType.getSort() == ARRAY) {
						frame.push(new Value.ArrayValue(retType.getElementType()));
					} else {
						frame.push(new Value.ObjectValue(retType));
					}
					break;
				case JSR:
				case RET:
					throw new IllegalAstException(instruction, "JSR/RET has been deprecated");
			}
		}
		// If we had already visited the frame the following frames may already be done.
		// We only need to recompute them if the old state and new state have matching local/stack states.
		if (wasVisited && ctxPc >= 0) {
			try {
				boolean modified = frame.merge(oldFrameState, this);
				continueExec &= modified;
			} catch (FrameMergeException ex) {
				throw new IllegalAstException(instruction, ex);
			}
		}
		// Only continue if needed
		return continueExec;
	}

	private void fillBlocks(Analysis analysis, List<AbstractInstruction> instructions) throws IllegalAstException {
		// Create the first block
		Frame entryFrame = analysis.frame(0);
		Block entryBlock = new Block();
		entryBlock.add(instructions.get(0), entryFrame);
		analysis.addBlock(0, entryBlock);
		// Create new blocks from try-catch handlers
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new IllegalAstException(tryCatch, "No associated handler label");
			int handlerIndex = instructions.indexOf(handlerLabel);
			if (!analysis.isBlockStart(handlerIndex)) {
				Frame handlerFrame = analysis.frame(handlerIndex);
				Block handlerBlock = new Block();
				handlerBlock.add(handlerLabel, handlerFrame);
				analysis.addBlock(handlerIndex, handlerBlock);
			}
		}
		// Create new blocks from instructions
		for (int insnIndex = 0; insnIndex < instructions.size(); insnIndex++) {
			AbstractInstruction instruction = instructions.get(insnIndex);
			// The branch-taken/branch-not-taken elements begin new blocks
			//  - conditionals/switches/etc
			if (instruction instanceof FlowControl) {
				FlowControl flow = (FlowControl) instruction;
				List<Label> targets = flow.getTargets(code.getLabels());
				// Branch taken
				for (Label target : targets) {
					int targetIndex = instructions.indexOf(target);
					Frame targetFrame = analysis.frame(targetIndex);
					if (!analysis.isBlockStart(targetIndex)) {
						Block targetBlock = new Block();
						targetBlock.add(target, targetFrame);
						analysis.addBlock(targetIndex, targetBlock);
					}
				}
				// Branch not taken
				int nextIndex = insnIndex + 1;
				AbstractInstruction next = instructions.get(nextIndex);
				if (!analysis.isBlockStart(nextIndex)) {
					Frame nextFrame = analysis.frame(nextIndex);
					Block targetBlock = new Block();
					targetBlock.add(next, nextFrame);
					analysis.addBlock(nextIndex, targetBlock);
				}
			} else {
				// Instructions after return statements are the last sources of new blocks
				int op = instruction.getOpcodeVal();
				if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
					int nextIndex = insnIndex + 1;
					if (nextIndex >= instructions.size())
						continue;
					AbstractInstruction next = instructions.get(nextIndex);
					if (!analysis.isBlockStart(nextIndex)) {
						Frame nextFrame = analysis.frame(nextIndex);
						Block targetBlock = new Block();
						targetBlock.add(next, nextFrame);
						analysis.addBlock(nextIndex, targetBlock);
					}
				}
			}
		}
		// Ensure all blocks inside a 'try' range flow into the 'catch' handler.
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label startLabel = code.getLabel(tryCatch.getStartLabel());
			Label endLabel = code.getLabel(tryCatch.getEndLabel());
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (startLabel == null)
				throw new IllegalAstException(tryCatch, "No associated start label");
			if (endLabel == null)
				throw new IllegalAstException(tryCatch, "No associated end label");
			int startIndex = instructions.indexOf(startLabel);
			int endIndex = instructions.indexOf(endLabel);
			int handlerIndex = instructions.indexOf(handlerLabel);
			Block handlerBlock = analysis.block(handlerIndex);
			for (int i = startIndex; i < endIndex; i++) {
				Block block = analysis.blockFloot(i);
				block.addHandlerEdge(handlerBlock);
			}
		}
		// Fill in block's instructions so consecutive instructions belong to the same block
		Block block = analysis.block(0);
		for (int insnIndex = 1; insnIndex < instructions.size(); insnIndex++) {
			if (analysis.isBlockStart(insnIndex)) {
				block = analysis.block(insnIndex);
			} else {
				AbstractInstruction instruction = instructions.get(insnIndex);
				Frame frame = analysis.frame(insnIndex);
				block.add(instruction, frame);
			}
		}
	}


	private static void binaryOp(Frame frame, Type type, BiFunction<Number, Number, Number> function) {
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		Value.NumericValue result;
		try {
			result = new Value.NumericValue(type, function.apply(
					((Value.NumericValue) value1).getNumber(),
					((Value.NumericValue) value2).getNumber()
			));
		} catch (Exception ex) {
			logger.debug("Binary operation Error", ex);
			result = new Value.NumericValue(type);
			frame.markWonky();
		}
		frame.push(result);
	}


	private static void unaryOp(Frame frame, Type type, Function<Number, Number> function) {
		Value value = frame.pop();
		Value.NumericValue result;
		try {
			result = new Value.NumericValue(type, function.apply(
					((Value.NumericValue) value).getNumber()
			));
		} catch (Exception ex) {
			logger.debug("Unnary operation Error", ex);
			result = new Value.NumericValue(type);
			frame.markWonky();
		}
		frame.push(result);
	}
}
