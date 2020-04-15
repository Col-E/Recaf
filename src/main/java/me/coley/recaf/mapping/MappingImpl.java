package me.coley.recaf.mapping;

import me.coley.recaf.workspace.Workspace;

import java.io.File;
import java.io.IOException;

/**
 * Enumeration of implemented mapping parsers.
 *
 * @author Matt
 */
public enum MappingImpl {
	SIMPLE, ENIGMA, PROGUARD, SRG, TINY, TINY2;

	/**
	 * @param file
	 * 		File containing mappings.
	 * @param workspace
	 * 		Workspace to use for hierarchy lookups.
	 *
	 * @return New mappings  instance of the type.
	 *
	 * @throws IOException
	 * 		When the mappings file could not be loaded.
	 */
	public Mappings create(File file, Workspace workspace) throws IOException {
		Mappings mappings;
		switch(this) {
			case SIMPLE:
				mappings = new SimpleMappings(file, workspace);
				break;
			case ENIGMA:
				mappings = new EnigmaMappings(file, workspace);
				break;
			case PROGUARD:
				mappings = new ProguardMappings(file, workspace);
				break;
			case SRG:
				mappings = new SrgMappings(file, workspace);
				break;
			case TINY:
				mappings = new TinyV1Mappings(file, workspace);
				break;
			case TINY2:
				mappings = new TinyV2Mappings(file, workspace);
				break;
			default:
				throw new IllegalStateException("Unsupported mapping implementation?");
		}
		return mappings;
	}
}
