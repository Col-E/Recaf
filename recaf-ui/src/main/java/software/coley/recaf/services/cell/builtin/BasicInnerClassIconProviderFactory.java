package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.properties.builtin.ThrowableProperty;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.services.cell.InnerClassIconProviderFactory;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link InnerClassIconProviderFactory}.
 * <br>
 * Not used by the default implementation for context-menus on {@link InnerClassInfo}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicInnerClassIconProviderFactory implements InnerClassIconProviderFactory {
	private static final IconProvider MISSING = () -> new FontIconView(CarbonIcons.MISUSE_ALT);
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
	public IconProvider getInnerClassInfoIconProvider(@Nonnull Workspace workspace,
													  @Nonnull WorkspaceResource resource,
													  @Nonnull ClassBundle<? extends ClassInfo> bundle,
													  @Nonnull ClassInfo outerClass,
													  @Nonnull InnerClassInfo inner) {
		return () -> {
			// While the inner class attribute gives us access flags, we want to grab the REAL
			// class-info instance for the inner class. This allows us to check for properties and such.
			ClassInfo innerClass = bundle.get(inner.getInnerClassName());
			if (innerClass == null)
				return MISSING.makeIcon();
			return classIconProvider(innerClass);
		};
	}

	private static Node classIconProvider(ClassInfo info) {
		// Special class cases
		if (info.hasEnumModifier()) return ENUM.makeIcon();
		if (info.hasAnnotationModifier()) return ANNO.makeIcon();
		if (info.hasInterfaceModifier()) return INTERFACE.makeIcon();

		// Normal class, consider other edge cases
		if (ThrowableProperty.get(info)) {
			if (info.hasAnnotationModifier()) {
				return ABSTRACT_EXCEPTION.makeIcon();
			} else {
				return EXCEPTION.makeIcon();
			}
		} else if (info.isAnonymousInner()) {
			return ANONYMOUS.makeIcon();
		} else if (info.hasAbstractModifier()) {
			return ABSTRACT.makeIcon();
		}
		return CLASS.makeIcon();
	}
}
