package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import regexodus.Pattern;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.visitors.SkippingClassVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that creates mappings to rename obfuscated classes based on any remaining {@code SourceFile} attribute.
 *
 * @author Matt Coley
 */
@Dependent
public class SourceNameRestorationTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Inner classes will retain the source attribute of their outer class, and
		// we only want to rename the top-level/outer class.
		if (initialClassState.isInnerClass())
			return;

		// Extract the source-file attribute contents, see if its reasonable
		// and then add it to the mappings.
		initialClassState.getClassReader().accept(new SkippingClassVisitor() {
			private static final Pattern SOURCE_NAME_PATTERN = RegexUtil.pattern("\\w{1, 50}\\.(?:java|kt)");

			@Override
			public void visitSource(String source, String debug) {
				if (source == null || source.isBlank() || !SOURCE_NAME_PATTERN.matches(source))
					return;
				String name = initialClassState.getName();
				String sourceName = source.substring(0, Math.max(source.lastIndexOf(".java"), source.lastIndexOf(".kt")));
				String packageName = initialClassState.getPackageName();
				String restoredName = packageName == null ? sourceName : packageName + '/' + sourceName;
				context.getMappings().addClass(name, restoredName);
			}
		}, 0);
	}

	@Nonnull
	@Override
	public String name() {
		return "Source name restoration";
	}
}
