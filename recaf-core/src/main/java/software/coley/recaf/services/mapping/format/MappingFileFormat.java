package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitOrder;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;

/**
 * Interface to use for explicit file format implementations of {@link Mappings}.
 * <br>
 * <h2>Relevant noteworthy points</h2>
 * <b>Incomplete mappings</b>: Not all mapping formats are complete in their representation. Some may omit the
 * descriptor of fields <i>(Because at the source level, overloaded names are illegal within the same class)</i>.
 * So while the methods defined here will always be provided all of this information, each implementation may have to
 * do more than a flat one-to-one lookup in these cases.
 * <br><br>
 * <b>Implementations do not need to be complete to partially work</b>: Some mapping formats do not support renaming
 * for variable names in methods. This is fine, because any method in this interface can be implemented as a no-op by
 * returning {@code null}.
 *
 * @author Matt Coley
 */
public interface MappingFileFormat {
	/**
	 * @return Name of the mapping format implementation.
	 */
	@Nonnull
	String implementationName();

	/**
	 * @param mappingsText
	 * 		Text of the mappings to parse.
	 *
	 * @return Intermediate mappings from parsed text.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	@Nonnull
	IntermediateMappings parse(@Nonnull String mappingsText) throws InvalidMappingException;

	/**
	 * Some mapping formats do not include field types since name overloading is illegal at the source level of Java.
	 * It's valid in the bytecode but the mapping omits this info since it isn't necessary information for mapping
	 * that does not support name overloading.
	 *
	 * @return {@code true} when field mappings include the type descriptor in their lookup information.
	 */
	boolean doesSupportFieldTypeDifferentiation();

	/**
	 * Some mapping forats do not include variable types since name overloading is illegal at the source level of Java.
	 * Variable names are not used by the JVM at all so their names can be anything at the bytecode level. So including
	 * the type makes it easier to reverse mappings.
	 *
	 * @return {@code true} when variable mappings include the type descriptor in their lookup information.
	 */
	boolean doesSupportVariableTypeDifferentiation();

	/**
	 * @return {@code true} when exporting the current mappings to text is supported.
	 *
	 * @see #exportText(Mappings)
	 */
	default boolean supportsExportText() {
		return true;
	}

	/**
	 * @param mappings
	 * 		Mappings to write with the current format.
	 *
	 * @return Exported mapping text in the current format. {@code null} if exporting to the format is unsupported.
	 *
	 * @throws InvalidMappingException
	 * 		When writing the mappings encounters any failure.
	 */
	@Nullable
	default String exportText(@Nonnull Mappings mappings) throws InvalidMappingException {
		return null;
	}

	/**
	 * A utility for utilizing mapping-io to parse mapping text formats.
	 *
	 * @param mappingText
	 * 		Text of mapping to parse.
	 * @param visitor
	 * 		Visitor pointing to a mapping-io format reader.
	 *
	 * @return Intermediate mapping representation of the parsed text.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	@Nonnull
	static IntermediateMappings parse(@Nonnull String mappingText, @Nonnull MappingTreeReader visitor) throws InvalidMappingException {
		// Populate the mapping-io model
		MemoryMappingTree tree = new MemoryMappingTree();
		StringReader reader = new StringReader(mappingText);
		try {
			visitor.read(reader, tree);
		} catch (IOException ex) {
			throw new InvalidMappingException(ex);
		}

		// Create our mapping model.
		IntermediateMappings mappings = new IntermediateMappings();

		// Mapping IO supports multiple namespaces for outputs.
		// This is only really used in the 'tiny' format. Generally speaking the input columns look like:
		//   obfuscated, intermediate, clean
		// or:
		//   intermediate, clean
		// We want everything to map to the final column, rather than their notion of the first
		// column mapping to one of the following columns.
		int namespaceCount = tree.getDstNamespaces().size();
		int finalNamespace = namespaceCount - 1;
		for (MappingTree.ClassMapping cm : tree.getClasses()) {
			String finalClassName = cm.getDstName(finalNamespace);

			// Add the base case: input --> final output name
			mappings.addClass(cm.getSrcName(), finalClassName);

			// Add destination[n] --> final output name, where n < destinations.length - 1.
			// This is how we handle cases like 'intermediate --> clean' despite both of those
			// being "output" columns.
			if (namespaceCount > 1)
				for (int i = 0; i < finalNamespace; i++)
					mappings.addClass(cm.getDstName(i), finalClassName);
			for (MappingTree.FieldMapping fm : cm.getFields()) {
				String finalFieldName = fm.getDstName(finalNamespace);

				// Base case, like before.
				mappings.addField(cm.getSrcName(), fm.getSrcDesc(), fm.getSrcName(), finalFieldName);

				// Support extra namespaces, like before.
				if (namespaceCount > 1)
					for (int i = 0; i < finalNamespace; i++)
						mappings.addField(cm.getDstName(i), fm.getDesc(i), fm.getDstName(i), finalFieldName);
			}
			for (MappingTree.MethodMapping mm : cm.getMethods()) {
				String finalMethodName = mm.getDstName(finalNamespace);

				// Same idea as field handling.
				mappings.addMethod(cm.getSrcName(), mm.getSrcDesc(), mm.getSrcName(), finalMethodName);
				if (namespaceCount > 1)
					for (int i = 0; i < finalNamespace; i++)
						mappings.addMethod(cm.getDstName(i), mm.getDstDesc(i), mm.getDstName(i), finalMethodName);
			}
		}
		return mappings;
	}

	/**
	 * A utility for utilizing mapping-io to write mapping text formats.
	 *
	 * @param mappings
	 * 		Mappings to export to text.
	 * @param writerFactory
	 * 		Factory to create a mapping-io format writer.
	 *
	 * @return Text representation of mappings in the format provided by the writer factory.
	 *
	 * @throws InvalidMappingException
	 * 		When writing the mappings encounters any failure.
	 */
	@Nonnull
	static String export(@Nonnull Mappings mappings, @Nonnull Function<StringWriter, MappingVisitor> writerFactory) throws InvalidMappingException {
		return export(mappings, "in", List.of("out"), writerFactory);
	}

	/**
	 * A utility for utilizing mapping-io to write mapping text formats.
	 *
	 * @param mappings
	 * 		Mappings to export to text.
	 * @param inputNamespace
	 * 		Input column name.
	 * @param outputNamespaces
	 * 		Output column names.
	 * @param writerFactory
	 * 		Factory to create a mapping-io format writer.
	 *
	 * @return Text representation of mappings in the format provided by the writer factory.
	 *
	 * @throws InvalidMappingException
	 * 		When writing the mappings encounters any failure.
	 */
	@Nonnull
	static String export(@Nonnull Mappings mappings, @Nonnull String inputNamespace,
						 @Nonnull List<String> outputNamespaces, @Nonnull Function<StringWriter, MappingVisitor> writerFactory) throws InvalidMappingException {
		MemoryMappingTree tree = new MemoryMappingTree();
		IntermediateMappings intermediate = mappings.exportIntermediate();
		try {
			tree.visitNamespaces(inputNamespace, outputNamespaces);
			for (ClassMapping classMapping : intermediate.getClasses().values()) {
				String classOriginalName = classMapping.getOldName();
				tree.visitClass(classOriginalName);
				tree.visitDstName(MappedElementKind.CLASS, 0, classMapping.getNewName());

				List<FieldMapping> fieldMappings = intermediate.getClassFieldMappings(classOriginalName);
				for (FieldMapping fieldMapping : fieldMappings) {
					tree.visitField(fieldMapping.getOldName(), fieldMapping.getDesc());
					tree.visitDstName(MappedElementKind.FIELD, 0, fieldMapping.getNewName());
				}

				List<MethodMapping> methodMappings = intermediate.getClassMethodMappings(classOriginalName);
				for (MethodMapping methodMapping : methodMappings) {
					tree.visitMethod(methodMapping.getOldName(), methodMapping.getDesc());
					tree.visitDstName(MappedElementKind.METHOD, 0, methodMapping.getNewName());
				}
			}

			StringWriter sw = new StringWriter();
			MappingVisitor writer = writerFactory.apply(sw);
			tree.accept(writer, VisitOrder.createByInputOrder());
			return sw.toString();
		} catch (Throwable t) {
			throw new InvalidMappingException(t);
		}
	}
}
