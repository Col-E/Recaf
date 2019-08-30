package me.coley.recaf.mapping;

import java.io.File;
import java.io.IOException;

/**
 * Enumeration of implemented mapping parsers.
 *
 * @author Matt
 */
public enum MappingImpl {
	SIMPLE, ENIGMA;

	/**
	 * @return New mappings  instance of the type.
	 *
	 * @throws IOException
	 * 		When the mappings file could not be loaded.
	 */
	public Mappings create(File file) throws IOException {
		switch(this) {
			case SIMPLE:
				return new SimpleMappings(file);
			case ENIGMA:
				return new EnigmaMappings(file);
		}
		throw new IllegalStateException("Unsupported mapping implementation?");
	}}
