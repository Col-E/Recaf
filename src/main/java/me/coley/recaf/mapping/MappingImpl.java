package me.coley.recaf.mapping;

import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Enumeration of implemented mapping parsers.
 *
 * @author Matt
 */
public enum MappingImpl {
	SIMPLE("Simple"),
	ENIGMA("Enigma"),
	PROGUARD("Proguard"),
	SRG("SRG"),
	TSRG("TSRG"),
	TINY("Tiny V1"),
	TINY2("Tiny V2"),
	JADX("JADX");

	private final String display;

	MappingImpl(String display) {
		this.display = display;
	}

	/**
	 * @return Display string.
	 */
	public String getDisplay() {
		return display;
	}

	/**
	 * @param path
	 * 		A path to a file containing mappings.
	 * @param workspace
	 * 		Workspace to use for hierarchy lookups.
	 *
	 * @return New mappings  instance of the type.
	 *
	 * @throws IOException
	 * 		When the mappings file could not be loaded.
	 */
	public Mappings create(Path path, Workspace workspace) throws IOException {
		Mappings mappings;
		switch(this) {
			case SIMPLE:
				mappings = new SimpleMappings(path, workspace);
				break;
			case ENIGMA:
				mappings = new EnigmaMappings(path, workspace);
				break;
			case PROGUARD:
				mappings = new ProguardMappings(path, workspace);
				break;
			case SRG:
				mappings = new SrgMappings(path, workspace);
				break;
			case TSRG:
				mappings = new TSrgMappings(path, workspace);
				break;
			case TINY:
				mappings = new TinyV1Mappings(path, workspace);
				break;
			case TINY2:
				mappings = new TinyV2Mappings(path, workspace, TinyV2Mappings.TinyV2SubType.OBF_TO_CLEAN);
				break;
			case JADX:
				mappings = new JadxMappings(path, workspace);
				break;
			default:
				throw new IllegalStateException("Unsupported mapping implementation?");
		}
		return mappings;
	}
}
