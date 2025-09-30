package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.gen.filter.IncludeKeywordNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonAsciiNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonJavaIdentifierNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeWhitespaceNameFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.mapping.gen.naming.IncrementingNameGenerator;
import software.coley.recaf.services.mapping.gen.naming.NameGenerator;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that renames classes and members that are not valid Java identifiers.
 *
 * @author Matt Coley
 */
@Dependent
public class IllegalNameMappingTransformer implements JvmClassTransformer {
	private static final NameGeneratorFilter ILLEGAL_NAME_FILTER =
			new IncludeWhitespaceNameFilter(new IncludeNonAsciiNameFilter(new IncludeKeywordNameFilter(new IncludeNonJavaIdentifierNameFilter(null))));
	private static final NameGenerator NAME_GENERATOR = new IncrementingNameGenerator();

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Create mappings for classes and members with illegal names.
		// Anything that already has a name in the current mappings will be ignored.
		IntermediateMappings mappings = context.getMappings();
		String ownerName = initialClassState.getName();
		if (ILLEGAL_NAME_FILTER.shouldMapClass(initialClassState) && mappings.getMappedClassName(ownerName) == null)
			mappings.addClass(ownerName, NAME_GENERATOR.mapClass(initialClassState));
		for (FieldMember field : initialClassState.getFields()) {
			String fieldDesc = field.getDescriptor();
			String fieldName = field.getName();
			if (ILLEGAL_NAME_FILTER.shouldMapField(initialClassState, field) && mappings.getMappedFieldName(ownerName, fieldDesc, fieldName) == null)
				mappings.addField(ownerName, fieldDesc, field.getName(), NAME_GENERATOR.mapField(initialClassState, field));
		}
		for (MethodMember method : initialClassState.getMethods()) {
			String methodDesc = method.getDescriptor();
			String methodName = method.getName();
			if (ILLEGAL_NAME_FILTER.shouldMapMethod(initialClassState, method) && mappings.getMappedMethodName(ownerName, methodDesc, methodName) == null)
				mappings.addMethod(ownerName, methodDesc, method.getName(), NAME_GENERATOR.mapMethod(initialClassState, method));
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Illegal name mapping";
	}
}
