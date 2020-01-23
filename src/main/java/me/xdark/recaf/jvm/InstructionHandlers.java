package me.xdark.recaf.jvm;

import me.xdark.recaf.jvm.instructions.*;

import static org.objectweb.asm.Opcodes.*;

final class InstructionHandlers {
	private static final InstructionHandler[] HANDLERS;

	private InstructionHandlers() {
	}

	static InstructionHandler getHandlerForOpcode(int opcode) {
		return HANDLERS[opcode];
	}

	static {
		InstructionHandler[] handlers = new InstructionHandler[200];
		handlers[NOP] = new InstructionHandlerNop();
		handlers[ACONST_NULL] = new InstructionHandlerConstNull();
		handlers[ICONST_M1] = new InstructionHandlerIConstM1();
		handlers[ICONST_0] = new InstructionHandlerIConst0();
		handlers[ICONST_1] = new InstructionHandlerIConst1();
		handlers[ICONST_2] = new InstructionHandlerIConst2();
		handlers[ICONST_3] = new InstructionHandlerIConst3();
		handlers[ICONST_4] = new InstructionHandlerIConst4();
		handlers[ICONST_5] = new InstructionHandlerIConst5();
		handlers[LCONST_0] = new InstructionHandlerLConst0();
		handlers[LCONST_1] = new InstructionHandlerLConst1();
		handlers[FCONST_0] = new InstructionHandlerFConst0();
		handlers[FCONST_1] = new InstructionHandlerFConst1();
		handlers[FCONST_2] = new InstructionHandlerFConst2();
		handlers[DCONST_0] = new InstructionHandlerDConst0();
		handlers[DCONST_1] = new InstructionHandlerDConst1();
		handlers[BIPUSH] = new InstructionHandlerBiPush();
		handlers[SIPUSH] = new InstructionHandlerSiPush();
		handlers[LDC] = new InstructionHandlerLdc();
		handlers[ILOAD] = new InstructionHandlerLoadInt();
		handlers[LLOAD] = new InstructionHandlerLoadLong();
		handlers[FLOAD] = new InstructionHandlerLoadFloat();
		handlers[DLOAD] = new InstructionHandlerLoadDouble();
		handlers[ALOAD] = new InstructionHandlerLoad();
		handlers[IALOAD] = new InstructionHandlerLoadArrayInt();
		handlers[LALOAD] = new InstructionHandlerLoadArrayLong();
		handlers[FALOAD] = new InstructionHandlerLoadArrayFloat();
		handlers[DALOAD] = new InstructionHandlerLoadArrayDouble();
		handlers[AALOAD] = new InstructionHandlerLoadArray();
		handlers[BALOAD] = new InstructionHandlerLoadArrayByte();
		handlers[CALOAD] = new InstructionHandlerLoadArrayChar();
		handlers[SALOAD] = new InstructionHandlerLoadArrayShort();
		handlers[ISTORE] = new InstructionHandlerStoreInt();
		handlers[LSTORE] = new InstructionHandlerStoreLong();
		handlers[FSTORE] = new InstructionHandlerStoreFloat();
		handlers[DSTORE] = new InstructionHandlerStoreDouble();
		handlers[ASTORE] = new InstructionHandlerStore();
		handlers[POP] = new InstructionHandlerPop();
		handlers[POP2] = new InstructionHandlerPop2();
		handlers[DUP] = new InstructionHandlerDup();
		handlers[DUP_X1] = new InstructionHandlerDupX1();
		handlers[DUP2_X2] = new InstructionHandlerDupX2();
		handlers[DUP2] = new InstructionHandlerDup2();
		handlers[DUP2_X1] = new InstructionHandlerDup2X1();
		handlers[DUP2_X2] = new InstructionHandlerDup2X2();
		handlers[SWAP] = new InstructionHandlerSwap();
		handlers[IADD] = new InstructionHandlerAddInt();
		handlers[LADD] = new InstructionHandlerAddLong();
		handlers[FADD] = new InstructionHandlerAddFloat();
		handlers[DADD] = new InstructionHandlerAddDouble();
		handlers[ISUB] = new InstructionHandlerSubInt();
		handlers[LSUB] = new InstructionHandlerSubLong();
		handlers[FSUB] = new InstructionHandlerSubFloat();
		handlers[DSUB] = new InstructionHandlerSubDouble();
		handlers[IMUL] = new InstructionHandlerMulInt();
		handlers[LMUL] = new InstructionHandlerMulLong();
		handlers[FMUL] = new InstructionHandlerMulFloat();
		handlers[DMUL] = new InstructionHandlerMulDouble();
		handlers[IDIV] = new InstructionHandlerDivInt();
		handlers[LDIV] = new InstructionHandlerDivLong();
		handlers[FDIV] = new InstructionHandlerDivFloat();
		handlers[DDIV] = new InstructionHandlerDivDouble();
		handlers[IREM] = new InstructionHandlerRemInt();
		handlers[LREM] = new InstructionHandlerRemLong();
		handlers[FREM] = new InstructionHandlerRemFloat();
		handlers[DREM] = new InstructionHandlerRemDouble();
		handlers[INEG] = new InstructionHandlerNegativeInt();
		handlers[LNEG] = new InstructionHandlerNegativeLong();
		handlers[FNEG] = new InstructionHandlerNegativeFloat();
		handlers[DNEG] = new InstructionHandlerNegativeDouble();
		handlers[ISHL] = new InstructionHandlerShlInt();
		handlers[LSHL] = new InstructionHandlerShlLong();
		handlers[ISHR] = new InstructionHandlerShrInt();
		handlers[LSHR] = new InstructionHandlerShrLong();
		handlers[IUSHR] = new InstructionHandlerUShrInt();
		handlers[LUSHR] = new InstructionHandlerUShrLong();
		handlers[IAND] = new InstructionHandlerAndInt();
		handlers[LAND] = new InstructionHandlerAndLong();
		handlers[IOR] = new InstructionHandlerOrInt();
		handlers[LOR] = new InstructionHandlerOrLong();
		handlers[IXOR] = new InstructionHandlerXorInt();
		handlers[LXOR] = new InstructionHandlerXorLong();
		handlers[IINC] = new InstructionHandlerIncInt();
		handlers[I2L] = new InstructionHandlerIntToLong();
		handlers[I2F] = new InstructionHandlerIntToFloat();
		handlers[I2D] = new InstructionHandlerIntToDouble();
		handlers[L2I] = new InstructionHandlerLongToInt();
		handlers[L2F] = new InstructionHandlerLongToFloat();
		handlers[L2D] = new InstructionHandlerLongToDouble();
		handlers[F2I] = new InstructionHandlerFloatToInt();
		handlers[F2L] = new InstructionHandlerFloatToLong();
		handlers[F2D] = new InstructionHandlerFloatToDouble();
		handlers[D2I] = new InstructionHandlerDoubleToInt();
		handlers[D2L] = new InstructionHandlerDoubleToLong();
		handlers[D2F] = new InstructionHandlerDoubleToFloat();
		handlers[I2B] = new InstructionHandlerIntToByte();
		handlers[I2C] = new InstructionHandlerIntToChar();
		handlers[I2S] = new InstructionHandlerIntToShort();
		handlers[LCMP] = new InstructionHandlerCompareLong();
		handlers[FCMPL] = new InstructionHandlerCompareFloatNegative();
		handlers[FCMPG] = new InstructionHandlerCompareFloatPositive();
		handlers[DCMPL] = new InstructionHandlerCompareDoubleNegative();
		handlers[DCMPG] = new InstructionHandlerCompareDoublePositive();
		handlers[IFEQ] = new InstructionHandlerIntEqualsZero();
		handlers[IFNE] = new InstructionHandlerIntNotEqualsZero();
		handlers[IFLT] = new InstructionHandlerIntLessZero();
		handlers[IFGE] = new InstructionHandlerIntGreaterEqualsZero();
		handlers[IFGT] = new InstructionHandlerIntGreaterZero();
		handlers[IFLE] = new InstructionHandlerIntLessEqualsZero();
		handlers[IF_ICMPEQ] = new InstructionHandlerIntsEquals();
		handlers[IF_ICMPNE] = new InstructionHandlerIntsNotEquals();
		handlers[IF_ICMPLT] = new InstructionHandlerIntsLess();
		handlers[IF_ICMPGE] = new InstructionHandlerIntsGreaterEquals();
		handlers[IF_ICMPGT] = new InstructionHandlerIntsGreater();
		handlers[IF_ICMPLE] = new InstructionHandlerIntsLessEquals();
		handlers[IF_ACMPEQ] = new InstructionHandlerEquals();
		handlers[IF_ACMPNE] = new InstructionHandlerIntsNotEquals();
		handlers[GOTO] = new InstructionHandlerGoto();
		handlers[JSR] = new InstructionHandlerJumpSubroutine(); // TODO
		handlers[RET] = new InstructionHandlerReturnSubroutine(); // TODO
		handlers[TABLESWITCH] = new InstructionHandlerTableSwitch();
		handlers[LOOKUPSWITCH] = new InstructionHandlerLookupSwitch();
		handlers[IRETURN] = new InstructionHandlerReturnInt();
		handlers[LRETURN] = new InstructionHandlerReturnLong();
		handlers[FRETURN] = new InstructionHandlerReturnFloat();
		handlers[DRETURN] = new InstructionHandlerReturnDouble();
		handlers[ARETURN] = new InstructionHandlerReturnReference();
		handlers[RETURN] = new InstructionHandlerReturn();
		// TODO GETSTATIC
		// TODO PUTSTATIC
		// TODO GETFIELD
		// TODO PUTFIELD
		// TODO INVOKEVIRTUAL
		// TODO INVOKESPECIAL
		// TODO INVOKESTATIC
		// TODO INVOKEINTERFACE
		// TODO INVOKEDYNAMIC
		// TODO NEW
		// TODO NEWARRAY
		// TODO ANEWARRAY
		handlers[ARRAYLENGTH] = new InstructionHandlerArrayLength();
		handlers[ATHROW] = new InstructionHandlerThrow();
		// TODO CHECKCAST
		// TODO INSTANCEOF
		// TODO MONITORENTER
		// TODO MONITOREXIT
		// TODO MULTIANEWARRAY
		handlers[IFNULL] = new InstructionHandlerIfNull();
		handlers[IFNONNULL] = new InstructionHandlerIfNotNull();
		HANDLERS = handlers;
	}
}
