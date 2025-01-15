package software.coley.recaf.services.cell.icon;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.ThrowableProperty;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link ClassIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicClassIconProviderFactory implements ClassIconProviderFactory {
	private static final IconProvider CLASS = Icons.createProvider(Icons.CLASS);
	private static final IconProvider INTERFACE = Icons.createProvider(Icons.INTERFACE);
	private static final IconProvider ANNO = Icons.createProvider(Icons.ANNOTATION);
	private static final IconProvider ENUM = Icons.createProvider(Icons.ENUM);
	private static final IconProvider ANONYMOUS = Icons.createProvider(Icons.CLASS_ANONYMOUS);
	private static final IconProvider ABSTRACT = Icons.createProvider(Icons.CLASS_ABSTRACT);
	private static final IconProvider EXCEPTION = Icons.createProvider(Icons.CLASS_EXCEPTION);
	private static final IconProvider ABSTRACT_EXCEPTION = Icons.createProvider(Icons.CLASS_ABSTRACT_EXCEPTION);

	@Nonnull
	@Override
	public IconProvider getJvmClassInfoIconProvider(@Nonnull Workspace workspace,
													@Nonnull WorkspaceResource resource,
													@Nonnull JvmClassBundle bundle,
													@Nonnull JvmClassInfo info) {
		return classIconProvider(info);
	}

	@Nonnull
	@Override
	public IconProvider getAndroidClassInfoIconProvider(@Nonnull Workspace workspace,
														@Nonnull WorkspaceResource resource,
														@Nonnull AndroidClassBundle bundle,
														@Nonnull AndroidClassInfo info) {
		return classIconProvider(info);
	}

	@Nonnull
	private static IconProvider classIconProvider(@Nonnull ClassInfo info) {
		// Special class cases
		if (info.hasEnumModifier()) return ENUM;
		if (info.hasAnnotationModifier()) return ANNO;
		if (info.hasInterfaceModifier()) return INTERFACE;

		// Normal class, consider other edge cases
		if (ThrowableProperty.get(info)) {
			if (info.hasAnnotationModifier()) {
				return ABSTRACT_EXCEPTION;
			} else {
				return EXCEPTION;
			}
		} else if (info.isAnonymousInnerClass()) {
			return ANONYMOUS;
		} else if (info.hasAbstractModifier()) {
			return ABSTRACT;
		}
		return CLASS;
	}
}
