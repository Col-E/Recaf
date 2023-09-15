package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.BasicBinaryXmlFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;

/**
 * Builder for {@link BasicBinaryXmlFileInfo}.
 *
 * @author Matt Coley
 */
public class BinaryXmlFileInfoBuilder extends ChunkFileInfoBuilder<BinaryXmlFileInfoBuilder> {
	public BinaryXmlFileInfoBuilder() {
		// empty
	}

	public BinaryXmlFileInfoBuilder(BinaryXmlFileInfo xmlInfo) {
		super(xmlInfo);
	}

	@Nonnull
	@Override
	public BasicBinaryXmlFileInfo build() {
		return new BasicBinaryXmlFileInfo(this);
	}
}
