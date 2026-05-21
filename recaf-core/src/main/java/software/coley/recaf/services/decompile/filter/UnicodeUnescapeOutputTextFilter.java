package software.coley.recaf.services.decompile.filter;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.workspace.model.Workspace;

public class UnicodeUnescapeOutputTextFilter implements OutputTextFilter {
	@Nonnull
	@Override
	public String filter(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo, @Nonnull String code) {
		return EscapeUtil.unescapeUnicode(code);
	}
}
