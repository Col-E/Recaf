package software.coley.recaf.ui.control.animation;

import jakarta.annotation.Nonnull;
import javafx.animation.Transition;
import javafx.scene.control.Labeled;
import javafx.util.Duration;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.util.StringUtil;

/**
 * Animation that shows a scrolling view of bytecode or raw file bytes in a label.
 *
 * @author Matt Coley
 */
public class LabelByteAnimationTransition extends Transition {
	private final Labeled labeled;
	private byte[] bytes;

	/**
	 * @param labeled
	 * 		Target label.
	 */
	public LabelByteAnimationTransition(@Nonnull Labeled labeled) {
		this.labeled = labeled;
	}

	/**
	 * @param info
	 * 		Class to show bytecode of.
	 */
	public void update(@Nonnull JvmClassInfo info) {
		this.bytes = info.getBytecode();
		setCycleDuration(Duration.millis(bytes.length));
	}

	/**
	 * @param info
	 * 		File to show raw bytes of.
	 */
	public void update(@Nonnull FileInfo info) {
		this.bytes = info.getRawContent();
		setCycleDuration(Duration.millis(bytes.length));
	}

	@Override
	protected void interpolate(double fraction) {
		int bytecodeSize = bytes.length;
		int textLength = 18;
		int middle = (int) (fraction * bytecodeSize);
		int start = middle - (textLength / 2);
		int end = middle + (textLength / 2);

		// We have two rows, top for hex, bottom for text.
		StringBuilder sbHex = new StringBuilder();
		StringBuilder sbText = new StringBuilder();
		for (int i = start; i < end; i++) {
			if (i < 0) {
				sbHex.append("   ");
				sbText.append("   ");
			} else if (i >= bytecodeSize) {
				sbHex.append(" ..");
				sbText.append(" ..");
			} else {
				short b = (short) (bytes[i] & 0xFF);
				char c = (char) b;
				if (Character.isWhitespace(c)) c = ' ';
				else if (c < 32) c = '?';
				String hex = StringUtil.limit(Integer.toHexString(b).toUpperCase(), 2);
				if (hex.length() == 1) hex = "0" + hex;
				sbHex.append(StringUtil.fillLeft(3, " ", hex));
				sbText.append(StringUtil.fillLeft(3, " ", String.valueOf(c)));
			}
		}
		labeled.setText(sbHex + "\n" + sbText);
	}
}
