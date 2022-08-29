package me.coley.recaf.ui.pane.outline;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.InnerClassInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Translatable;

import java.util.function.Function;

/**
 * Enum for differentiating different visibility filter for {@link OutlinePane}.
 *
 * @author Amejonah
 */
public enum Visibility implements Translatable {
	ALL(Icons.ACCESS_ALL_VISIBILITY, (flags) -> true),
	PUBLIC(Icons.ACCESS_PUBLIC, AccessFlag::isPublic),
	PROTECTED(Icons.ACCESS_PROTECTED, AccessFlag::isProtected),
	PACKAGE(Icons.ACCESS_PACKAGE, AccessFlag::isPackage),
	PRIVATE(Icons.ACCESS_PRIVATE, AccessFlag::isPrivate);

	public final String icon;
	private final Function<Integer, Boolean> isAccess;

	Visibility(String icon, Function<Integer, Boolean> isAccess) {
		this.icon = icon;
		this.isAccess = isAccess;
	}

	public static Visibility ofItem(ItemInfo info) {
		if (info instanceof MemberInfo) {
			return ofMember((MemberInfo) info);
		} else if (info instanceof CommonClassInfo) {
			return ofClass((CommonClassInfo) info);
		} else if (info instanceof InnerClassInfo) {
			return ofClass((InnerClassInfo) info);
		} else {
			throw new IllegalArgumentException("Unknown item type: " + info.getClass().getSimpleName());
		}
	}

	public boolean isAccess(int flags) {
		return isAccess.apply(flags);
	}

	public static Visibility ofMember(MemberInfo memberInfo) {
		return ofAccess(memberInfo.getAccess());
	}

	public static Visibility ofClass(CommonClassInfo info) {
		return ofAccess(info.getAccess());
	}

	public static Visibility ofClass(InnerClassInfo info) {
		return ofAccess(info.getAccess());
	}

	private static Visibility ofAccess(int access) {
		if (AccessFlag.isPublic(access))
			return PUBLIC;
		if (AccessFlag.isProtected(access))
			return PROTECTED;
		if (AccessFlag.isPackage(access))
			return PACKAGE;
		if (AccessFlag.isPrivate(access))
			return PRIVATE;
		return ALL;
	}

	@Override
	public String getTranslationKey() {
		return this == ALL ? "misc.all" : "misc.accessflag.visibility." + name().toLowerCase();
	}

	public enum IconPosition implements Translatable {
		NONE, LEFT, RIGHT;

		@Override
		public String getTranslationKey() {
			return this == NONE ? "misc.none" : "misc.position." + name().toLowerCase();
		}
	}
}
