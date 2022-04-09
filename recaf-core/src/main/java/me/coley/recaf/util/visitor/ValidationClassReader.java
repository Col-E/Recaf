package me.coley.recaf.util.visitor;

import me.coley.cafedude.classfile.ConstantPoolConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * Optimized ClassReader that provides faster
 * methods for constant lookups.
 *
 * @author xDark
 */
public class ValidationClassReader extends ClassReader implements ConstantPoolConstants {

	private static final Object DUMMY = new Object();
	private final byte[] classFile;
	private final boolean[] valid;

	/**
	 * Create reader.
	 *
	 * @param classFile
	 * 		Class file content.
	 */
	public ValidationClassReader(byte[] classFile) {
		super(classFile);
		this.classFile = classFile;
		valid = new boolean[getItemCount()];
	}

	@Override
	protected Label readLabel(int bytecodeOffset, Label[] labels) {
		Label unused = labels[bytecodeOffset];
		return null;
	}

	@Override
	public Object readConst(int constantPoolEntryIndex, char[] charBuffer) {
		boolean[] valid = this.valid;
		if (valid == null) return super.readConst(constantPoolEntryIndex, charBuffer);
		if (valid[constantPoolEntryIndex]) return DUMMY;
		int cpInfoOffset = getItem(constantPoolEntryIndex);
		byte[] classFile = this.classFile;
		switch (classFile[cpInfoOffset - 1]) {
			case INTEGER:
			case FLOAT:
				throwIfOutOfBounds(classFile, cpInfoOffset + 3);
				break;
			case LONG:
			case DOUBLE:
				throwIfOutOfBounds(classFile, cpInfoOffset + 7);
				break;
			case CLASS:
			case STRING:
			case METHOD_TYPE:
				readUTF8(cpInfoOffset, charBuffer);
				break;
			case METHOD_HANDLE:
			case DYNAMIC:
				super.readConst(constantPoolEntryIndex, charBuffer);
				break;
			default:
				throw QuietValidationException.INSTANCE;
		}
		valid[constantPoolEntryIndex] = true;
		return DUMMY;
	}

	@Override
	public String readUTF8(int offset, char[] charBuffer) {
		// TODO: Something in this method makes us skip over later validation checks
		boolean[] valid = this.valid;
		if (valid == null) return super.readUTF8(offset, charBuffer);
		if (offset == 0) return null;
		int constantPoolEntryIndex = readUnsignedShort(offset);
		if (constantPoolEntryIndex == 0) return null;
		if (valid[constantPoolEntryIndex]) return "";
		int cpInfoOffset = getItem(constantPoolEntryIndex);
		int utfLength = readUnsignedShort(cpInfoOffset);
		int currentOffset = cpInfoOffset + 2;
		int endOffset = currentOffset + utfLength;
		byte[] classBuffer = classFile;

		while (currentOffset < endOffset) {
			int x = currentOffset++;
			throwIfOutOfBounds(classBuffer, x);
			int currentByte = classBuffer[x];
			if ((currentByte & 0x80) == 0) {
			} else if ((currentByte & 0xE0) == 0xC0) {
				throwIfOutOfBounds(classBuffer, currentOffset++);
			} else {
				if (isOutOfBounds(classBuffer, currentOffset++)
						|| isOutOfBounds(classBuffer, currentOffset++)) {
					throw QuietValidationException.INSTANCE;
				}
			}
		}
		valid[constantPoolEntryIndex] = true;
		return "";
	}

	private static void throwIfOutOfBounds(byte[] classFileBuffer, int offset) {
		if (isOutOfBounds(classFileBuffer, offset)) {
			throw QuietValidationException.INSTANCE;
		}
	}

	private static boolean isOutOfBounds(byte[] classFileBuffer, int offset) {
		return offset >= classFileBuffer.length;
	}
}
