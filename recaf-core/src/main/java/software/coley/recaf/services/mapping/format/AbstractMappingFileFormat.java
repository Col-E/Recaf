package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;

/**
 * Common base for mapping file format values.
 *
 * @author Matt Coley
 */
public abstract class AbstractMappingFileFormat implements MappingFileFormat {
	private final String implementationName;
	private final boolean supportFieldTypeDifferentiation;
	private final boolean supportVariableTypeDifferentiation;

	protected AbstractMappingFileFormat(String implementationName,
										boolean supportFieldTypeDifferentiation,
										boolean supportVariableTypeDifferentiation) {
		this.implementationName = implementationName;
		this.supportFieldTypeDifferentiation = supportFieldTypeDifferentiation;
		this.supportVariableTypeDifferentiation = supportVariableTypeDifferentiation;
	}

	@Nonnull
	@Override
	public String implementationName() {
		return implementationName;
	}

	@Override
	public boolean doesSupportFieldTypeDifferentiation() {
		return supportFieldTypeDifferentiation;
	}

	@Override
	public boolean doesSupportVariableTypeDifferentiation() {
		return supportVariableTypeDifferentiation;
	}
}
