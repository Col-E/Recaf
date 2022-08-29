package me.coley.recaf.ui.pane.outline;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.Translatable;

/**
 * Enum for differentiating different member type filters for {@link OutlinePane}.
 *
 * @author Amejonah
 */
public enum MemberType implements Translatable {
	ALL(Icons.CLASS_N_FIELD_N_METHOD, "misc.all"),
	FIELD(Icons.FIELD, "misc.member.field"),
	METHOD(Icons.METHOD, "misc.member.method"),
	FIELD_AND_METHOD(Icons.FIELD_N_METHOD, "misc.member.field_n_method"),
	INNER_CLASS(Icons.CLASS, "misc.member.inner_class");

	final String icon;
	final String key;

	MemberType(String icon, String key) {
		this.icon = icon;
		this.key = key;
	}

	@Override
	public String getTranslationKey() {
		return key;
	}

	public boolean shouldDisplay(MemberType filter) {
		return this == ALL || this == filter || (this == FIELD_AND_METHOD && (filter == FIELD || filter == METHOD));
	}
}
