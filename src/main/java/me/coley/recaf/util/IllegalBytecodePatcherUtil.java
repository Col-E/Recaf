package me.coley.recaf.util;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.constant.ConstPoolEntry;
import me.coley.cafedude.constant.CpClass;
import me.coley.cafedude.constant.CpUtf8;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Utility to attempt basic recovery of classes that crash ASM.
 *
 * @author Matt
 */
public class IllegalBytecodePatcherUtil {
	/**
	 * @param classes
	 * 		Successfully loaded classes in the input.
	 * @param invalidClasses
	 * 		The complete map of invalid classes in the input.
	 * @param value
	 * 		Raw bytecode of a class that crashes ASM.
	 *
	 * @return Modified bytecode, hopefully that yields valid ASM parsable class.
	 * If any exception is thrown the original bytecode is returned.
	 */
	public static byte[] fix(Map<String, byte[]> classes, Map<String, byte[]> invalidClasses, byte[] value) {
		try {
			String str = new String(value).toLowerCase();
			ClassFile cf = new ClassFileReader().read(value);
			// Patch oak classes (pre-java)
			//  - CafeDude does this by default
			if (cf.getVersionMajor() < 45 ||(cf.getVersionMajor() == 45 && cf.getVersionMinor() <= 2)) {
				// Bump version into range where class file format uses full length values
				cf.setVersionMajor(45);
				cf.setVersionMinor(3);
				return new ClassFileWriter().write(cf);
			}
			// TODO: A good automated system to detect the problem would be handy
			//  - Something better than this obviously since these are essentially swappable watermarks
			else if (str.contains("binscure") || str.contains("binclub") || str.contains("java/yeet")) {
				return patchBinscure(cf);
			} else {
				// TODO: Other obfuscators that create invalid classes, like Paramorphism, should be supported
				//  - Some code for this already exists in the discord group but its outdated...
				Log.info("Unknown protection on class file");
			}
			return new ClassFileWriter().write(cf);
		} catch (Throwable t) {
			// Fallback, yield original value
			Log.error(t, "Failed to patch class");
			return value;
		}
	}

	/**
	 * Patch Binscure obfuscation.
	 *
	 * @param cf
	 * 		Classfile obfuscated by Binscure.
	 *
	 * @return ASM-parsable bytecode.
	 *
	 * @throws InvalidClassException
	 * 		When the class could not be read or written back to.
	 */
	private static byte[] patchBinscure(ClassFile cf) throws InvalidClassException {
		// Swap illegal class names like "give up" to something legal
		int bad = 1;
		for (ConstPoolEntry entry : cf.getPool()) {
			if (entry instanceof CpClass) {
				CpUtf8 name = ((CpUtf8) cf.getPool().get(((CpClass) entry).getIndex()));
				if (isIllegalName(name.getText())) {
					name.setText("patched/binclub/FakeType" + (bad++));
				}
			}
		}
		// Write class-file back
		byte[] out = new ClassFileWriter().write(cf);
		// Attempt to pass-through asm to remove some extra junk,
		// which assists some decompilers in not totally shitting themselves.
		// The output is still trash, but at least there is output.
		if (ClassUtil.isValidClass(out)) {
			try {
				ClassNode node = ClassUtil.getNode(new org.objectweb.asm.ClassReader(out),
						org.objectweb.asm.ClassReader.EXPAND_FRAMES);
				for (MethodNode mn : node.methods) {
					for (AbstractInsnNode insn : mn.instructions) {
						// These "ex-dee le may-may" named instructions are bogus.
						// They're in code-blocks that get skipped over and only serve to include junk
						// in the constant-pool that throws off decompilers.
						if (insn instanceof InvokeDynamicInsnNode) {
							InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
							if (indy.bsm.getOwner().equals("java/yeet") ||
									indy.name.equals("fuck") || indy.name.equals("yayeet")) {
								mn.instructions.set(insn, new InsnNode(Opcodes.NOP));
							}
						}
					}
					// TODO: Find a nice way to remove bad catch blocks
				}
				return ClassUtil.toCode(node, 0);
			} catch (Throwable t) {
				Log.warn("Failed to remove junk INVOKEDYNAMIC instructions");
			}
		} else {
			Log.error("Binscure process failed to fully patch class. New ASM crash method?");
		}
		// Fallback if second-asm pass fails.
		// Classes should be valid at this point, but not entierly cleaned up.
		return out;
	}

	private static boolean isIllegalName(String name) {
		return name.matches(".*\\s+.*");
	}
}
