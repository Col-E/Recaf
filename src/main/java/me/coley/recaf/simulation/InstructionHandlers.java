package me.coley.recaf.simulation;

import me.coley.recaf.simulation.instructions.InstructionHandlerAddDouble;
import me.coley.recaf.simulation.instructions.InstructionHandlerAddFloat;
import me.coley.recaf.simulation.instructions.InstructionHandlerAddInt;
import me.coley.recaf.simulation.instructions.InstructionHandlerAddLong;
import me.coley.recaf.simulation.instructions.InstructionHandlerBiPush;
import me.coley.recaf.simulation.instructions.InstructionHandlerConstNull;
import me.coley.recaf.simulation.instructions.InstructionHandlerDConst0;
import me.coley.recaf.simulation.instructions.InstructionHandlerDConst1;
import me.coley.recaf.simulation.instructions.InstructionHandlerDup;
import me.coley.recaf.simulation.instructions.InstructionHandlerDup2;
import me.coley.recaf.simulation.instructions.InstructionHandlerDup2X1;
import me.coley.recaf.simulation.instructions.InstructionHandlerDup2X2;
import me.coley.recaf.simulation.instructions.InstructionHandlerDupX1;
import me.coley.recaf.simulation.instructions.InstructionHandlerDupX2;
import me.coley.recaf.simulation.instructions.InstructionHandlerFConst0;
import me.coley.recaf.simulation.instructions.InstructionHandlerFConst1;
import me.coley.recaf.simulation.instructions.InstructionHandlerFConst2;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst0;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst1;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst2;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst3;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst4;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConst5;
import me.coley.recaf.simulation.instructions.InstructionHandlerIConstM1;
import me.coley.recaf.simulation.instructions.InstructionHandlerLConst0;
import me.coley.recaf.simulation.instructions.InstructionHandlerLConst1;
import me.coley.recaf.simulation.instructions.InstructionHandlerLdc;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoad;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArray;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayByte;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayChar;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayDouble;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayFloat;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayInt;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayLong;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadArrayShort;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadDouble;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadFloat;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadInt;
import me.coley.recaf.simulation.instructions.InstructionHandlerLoadLong;
import me.coley.recaf.simulation.instructions.InstructionHandlerNop;
import me.coley.recaf.simulation.instructions.InstructionHandlerPop;
import me.coley.recaf.simulation.instructions.InstructionHandlerPop2;
import me.coley.recaf.simulation.instructions.InstructionHandlerSiPush;
import me.coley.recaf.simulation.instructions.InstructionHandlerStore;
import me.coley.recaf.simulation.instructions.InstructionHandlerStoreDouble;
import me.coley.recaf.simulation.instructions.InstructionHandlerStoreFloat;
import me.coley.recaf.simulation.instructions.InstructionHandlerStoreInt;
import me.coley.recaf.simulation.instructions.InstructionHandlerStoreLong;
import me.coley.recaf.simulation.instructions.InstructionHandlerSwap;

import static org.objectweb.asm.Opcodes.*;

final class InstructionHandlers {
	private static final InstructionHandler[] HANDLERS;

	private InstructionHandlers() {
	}

	static InstructionHandler getHandlerForOpcode(int opcode) {
		return HANDLERS[opcode];
	}

	static {
		InstructionHandler[] handlers = new InstructionHandler[199];
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
		HANDLERS = handlers;
	}
}
