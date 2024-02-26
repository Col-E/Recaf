package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny1FileWriter;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;

import java.util.List;

/**
 * Tiny-V1 mappings file implementation.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
@Dependent
public class TinyV1Mappings extends AbstractMappingFileFormat {
	public static final String NAME = "Tiny-V1";

	/**
	 * New tiny v1 instance.
	 */
	public TinyV1Mappings() {
		super(NAME, true, true);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) throws InvalidMappingException {
		return MappingFileFormat.parse(mappingText, Tiny1FileReader::read);
	}

	@Override
	public String exportText(@Nonnull Mappings mappings) throws InvalidMappingException {
		return MappingFileFormat.export(mappings, "intermediary", List.of("named"), Tiny1FileWriter::new);
	}
}
