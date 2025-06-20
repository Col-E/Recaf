package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.kotlin.model.KtClass;
import software.coley.recaf.util.kotlin.model.KtFunction;
import software.coley.recaf.util.kotlin.model.KtProperty;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;

/**
 * A transformer that renames classes and members based on Kotlin metadata.
 *
 * @author Matt Coley
 */
@Dependent
public class KotlinNameRestorationTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		AggregatedMappings mappings = context.getMappings();
		String ownerName = initialClassState.getName();
		KotlinMetadataCollectionTransformer metadata = context.getJvmTransformer(KotlinMetadataCollectionTransformer.class);
		KtClass ktClass = metadata.getKtClass(ownerName);
		if (ktClass == null) {
			// No metadata model, but see if we were able to extract a name from some other kind of data from
			// our collection transformation pass.
			String mappedOwner = metadata.getKtFallbackMapping(ownerName);
			if (mappedOwner != null)
				mappings.addClass(ownerName, mappedOwner);
			return;
		}

		// Sadly, the kotlin meta-data is unordered, so we can only be sure about name mappings
		// when there are only EXACT descriptor matches.
		if (ktClass.getName() != null)
			mappings.addClass(ownerName, ktClass.getName());
		for (KtProperty property : ktClass.getProperties()) {
			String propertyName = property.getName();
			if (propertyName == null)
				continue;

			// Check for field match
			FieldMember field = metadata.getField(initialClassState, property);
			if (field != null)
				mappings.addField(ownerName, field.getDescriptor(), field.getName(), propertyName);

			// Check for getter match
			MethodMember method = metadata.getFieldGetter(initialClassState, property);
			if (method != null) {
				if (method.getName().equals(propertyName))
					continue;
				if (!propertyName.startsWith("get") && !propertyName.startsWith("is") && !propertyName.startsWith("do"))
					propertyName = "get" + StringUtil.uppercaseFirstChar(propertyName);
				mappings.addMethod(ownerName, method.getDescriptor(), method.getName(), propertyName);
			}
		}
		for (KtFunction function : ktClass.getFunctions()) {
			if (function.getName() == null)
				continue;

			// Check for method match
			MethodMember method = metadata.getMethod(initialClassState, function);
			if (method != null)
				mappings.addMethod(ownerName, method.getDescriptor(), method.getName(), function.getName());
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Kotlin name restoration";
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> dependencies() {
		return Collections.singleton(KotlinMetadataCollectionTransformer.class);
	}
}
