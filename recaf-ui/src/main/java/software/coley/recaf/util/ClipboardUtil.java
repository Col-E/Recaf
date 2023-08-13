package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.member.ClassMember;

/**
 * Utils for clipboard actions.
 *
 * @author Matt Coley
 */
public class ClipboardUtil {
	/**
	 * @param info
	 * 		Info to copy name/path of.
	 */
	public static void copyString(@Nonnull Info info) {
		copyString(info.getName());
	}

	/**
	 * @param declaring
	 * 		Declaring class.
	 * @param member
	 * 		Member to copy name/path of.
	 */
	public static void copyString(@Nonnull ClassInfo declaring, @Nonnull ClassMember member) {
		String memberName = member.isField() ? member.getName() + " " + member.getDescriptor() :
				member.getName() + member.getDescriptor();
		String name = declaring.getName() + "." + memberName;
		copyString(name);
	}

	/**
	 * @param text
	 * 		String to copy.
	 */
	public static void copyString(@Nonnull String text) {
		ClipboardContent clipboard = new ClipboardContent();
		clipboard.putString(text);
		Clipboard.getSystemClipboard().setContent(clipboard);
	}
}
