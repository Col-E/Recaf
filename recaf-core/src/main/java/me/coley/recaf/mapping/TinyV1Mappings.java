package me.coley.recaf.mapping;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * Tiny-V1 mappings file implementation.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
public class TinyV1Mappings extends MappingsAdapter {
	private final Logger logger = Logging.get(TinyV1Mappings.class);

	/**
	 * New tiny v1 instance.
	 */
	public TinyV1Mappings() {
		super("Tiny V1", true, true);
	}

	@Override
	public void parse(String mappingText) {
		String[] lines = mappingText.split("[\n\r]");
		int lineNum = 0;
		for (String line : lines) {
			lineNum++;
			// Skip initial header
			if (line.startsWith("v1\t"))
				continue;
			String[] args = line.trim().split("\t");
			String type = args[0];
			try {
				switch (type) {
					case "CLASS": {
						String oldClass = args[1];
						String newClass = args[2];
						addClass(oldClass, newClass);
						break;
					}
					case "FIELD": {
						String oldOwner = args[1];
						String oldName = args[3];
						String newName = args[4];
						addField(oldOwner, oldName, newName);
						break;
					}
					case "METHOD": {
						String oldOwner = args[1];
						String oldDesc = args[2];
						String oldName = args[3];
						String newName = args[4];
						addMethod(oldOwner, oldName, oldDesc, newName);
						break;
					}
					default: {
						logger.error("Failed to parse mapping type {} at line {}.", type, lineNum);
						break;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				logger.error("Failed parsing line {}.", lineNum);
				break;
			}
		}
	}
}
