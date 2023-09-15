package software.coley.recaf.workspace.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Info;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.io.ByteSource;

import java.io.IOException;

/**
 * Service outline for creating various {@link Info} types from a basic name, {@link ByteSource} pair.
 *
 * @author Matt Coley
 */
public interface InfoImporter extends Service {
	String SERVICE_ID = "info-importer";

	/**
	 * @param name
	 * 		Name to pass for {@link Info#getName()} if it cannot be inferred from the content source.
	 * @param source
	 * 		Source of content to read data from.
	 *
	 * @return Info instance.
	 *
	 * @throws IOException
	 * 		When the content cannot be read.
	 */
	@Nonnull
	Info readInfo(String name, ByteSource source) throws IOException;
}
