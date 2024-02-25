package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;

import java.io.IOException;
import java.io.Reader;

/**
 * Outlines the read process from a given reader into the given visitor.
 * Should point to a file-reader method from mapping-io.
 * For instance: {@link Tiny1FileReader#read(Reader, MappingVisitor)}.
 *
 * @author Matt Coley
 * @see MappingFileFormat#parse(String, MappingTreeReader)
 */
public interface MappingTreeReader {
	/**
	 * @param reader
	 * 		Reader containing the mapping file text.
	 * @param visitor
	 * 		Mapping output visitor.
	 *
	 * @throws IOException
	 * 		When any mapping parse errors occur.
	 */
	void read(@Nonnull Reader reader, @Nonnull MappingVisitor visitor) throws IOException;
}
