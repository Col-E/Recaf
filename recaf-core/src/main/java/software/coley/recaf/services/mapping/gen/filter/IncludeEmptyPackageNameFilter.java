package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;

/**
 * Filter that includes class names with zero-width packages.
 *
 * @author Matt Coley
 */
public class IncludeEmptyPackageNameFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeEmptyPackageNameFilter(@Nullable NameGeneratorFilter next) {
		super(next, false);
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (info.getName().contains("//"))
			return true;
		return super.shouldMapClass(info);
	}
}
