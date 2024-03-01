package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;

import java.util.List;

/**
 * Tiny-V2 mappings file implementation.
 *
 * @author Matt Coley
 */
@Dependent
public class TinyV2Mappings extends AbstractMappingFileFormat {
	public static final String NAME = "Tiny-V2";

	/**
	 * New tiny v2 instance.
	 */
	public TinyV2Mappings() {
		super(NAME, true, true);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) throws InvalidMappingException {
		return MappingFileFormat.parse(mappingText, Tiny2FileReader::read);
	}

	@Override
	public String exportText(@Nonnull Mappings mappings) throws InvalidMappingException {
		return MappingFileFormat.export(mappings, "intermediary", List.of("named"), writer -> new Tiny2FileWriter(writer, true));
	}
}
