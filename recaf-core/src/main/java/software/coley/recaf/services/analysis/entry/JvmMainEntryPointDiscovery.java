package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * Discovery for JVM {@code main} methods.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JvmMainEntryPointDiscovery implements EntryPointDiscovery {
	@Nonnull
	@Override
	public EntryPointKind kind() {
		return EntryPointKind.JVM_MAIN_METHOD;
	}

	@Nonnull
	@Override
	public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		List<EntryPoint> entries = new ArrayList<>();
		resource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
			ClassPathNode classPath = null;
			for (MethodMember method : cls.getMethods()) {
				if (!isJvmEntry(cls, method))
					continue;
				if (classPath == null)
					classPath = PathNodes.classPath(workspace, resource, bundle, cls);
				entries.add(new EntryPoint(kind(), classPath, classPath.child(method)));
			}
		}));
		return entries;
	}

	private static boolean isJvmEntry(@Nonnull JvmClassInfo owner, @Nonnull MethodMember method) {
		// Must be named "main".
		if (!method.getName().equals("main"))
			return false;

		// From Java 20 and below the main method contract is the classic one.
		String descriptor = method.getDescriptor();
		int version = JavaVersion.adaptFromClassFileVersion(owner.getVersion());
		if (version <= 20)
			return method.hasModifierMask(PUBLIC | STATIC)
					&& descriptor.equals("([Ljava/lang/String;)V");

		// Modern Java has more flexible entry points (JEP-445/463/477/495/512).
		// - static void main(String[] args)
		// - static void main()
		// - void main(String[] args)
		// - void main()
		return descriptor.equals("([Ljava/lang/String;)V") || descriptor.equals("()V");
	}
}
