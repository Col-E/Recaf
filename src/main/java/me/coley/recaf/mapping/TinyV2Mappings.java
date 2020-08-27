package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.util.Log.trace;

/**
 * Tiny-V2 mappings file implementation.
 * <br>
 * <a href="https://github.com/FabricMC/tiny-remapper/issues/9">[Specification of format]</a>
 *
 * @author Matt
 */
public class TinyV2Mappings extends FileMappings {
	private static final String FAIL = "Invalid Tiny-V2 mappings, ";
	private final TinyV2SubType subType;

	/**
	 * Constructs mappings from a given file.
	 *
	 * @param path
	 * 		A path to a file containing Tiny-V2 styled mappings.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 * @param subType
	 * 		Tiny V2 direction type for mapping.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	public TinyV2Mappings(Path path, Workspace workspace, TinyV2SubType subType) throws IOException {
		super(path, workspace, false);
		this.subType = subType;
		read(path.toFile());
	}

	@Override
	protected Map<String, String> parse(String text) {
		Map<String, String> map = new HashMap<>();
		String[] lines = StringUtil.splitNewline(text);
		int line = 0;
		String currentClass = null;
		for(String lineStr : lines) {
			line++;
			// Skip initial header
			if (lineStr.startsWith("tiny\t"))
				continue;
			String lineStrTrim = lineStr.trim();
			int strIndent = lineStr.indexOf(lineStrTrim);
			String[] args = lineStrTrim.split("\t");
			String type = args[0];
			try {
				// A note on the "intermediate" values... I have seen cases of the format where this column
				// does not exist... so the fix here will be to check for the number of columns. If there are
				// enough, we assume it contains the intermediate in the middle. Otherwise, there is none.
				switch(type) {
					case "c":
						// TinyV2 reuses "c" for "comment" too
						// These are indented to indicate they belong to members/types, so skip em.
						if (strIndent > 0)
							continue;
						// [1] = current
						// [2*] = intermediate
						// [3] = renamed
						int[] clsRenameIndices = subType.getFromXToYOffsets(Context.CLASS, args.length);
						currentClass = args[clsRenameIndices[0]];
						String renamedClass = args[clsRenameIndices[1]];
						map.put(currentClass, renamedClass);
						break;
					case "f":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");
						// [1] = desc
						// [2] = current
						// [3*] = intermediate
						// [4] = renamed
						int[] fldRenameIndices = subType.getFromXToYOffsets(Context.FIELD, args.length);
						String currentField = args[fldRenameIndices[0]];
						String renamedField = args[fldRenameIndices[1]];
						map.put(currentClass + "." + currentField, renamedField);
						break;
					case "m":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");
						// [1] = desc
						// [2] = current
						// [3*] = intermediate
						// [4] = renamed
						int[] mtdRenameIndices = subType.getFromXToYOffsets(Context.METHOD, args.length);
						String methodType = args[1];
						String currentMethod = args[mtdRenameIndices[0]];
						String renamedMethod = args[mtdRenameIndices[1]];
						map.put(currentClass + "." + currentMethod + methodType, renamedMethod);
						break;
					default:
						trace("Unknown Tiny-V2 mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch(IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
			}
		}
		return map;
	}

	/**
	 * Subtype for TinyV2 handling.
	 *
	 * @author Matt
	 */
	public enum TinyV2SubType {
		OBF_TO_CLEAN("Obfuscated to named"),
		OBF_TO_INTERMEDIATE("Obfuscated to intermediate"),
		INTERMEDIATE_TO_CLEAN("Intermediate to named"),
		INTERMEDIATE_TO_OBF("Intermediate to obfuscated"),
		CLEAN_TO_INTERMEDIATE("Named to intermediate"),
		CLEAN_TO_OBF("Named to obfuscated");

		private final String display;

		TinyV2SubType(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}

		/**
		 * @param ctx
		 * 		Mapping context, class, field, or method.
		 * @param columns
		 * 		The number of columns in the row.
		 * 		Used to determine if the input matches the specs
		 * 		<i>(And if not, limit the return value to be inside the range of columns)</i>.
		 *
		 * @return Pair of integers for the before name and after name indices.
		 */
		public int[] getFromXToYOffsets(Context ctx, int columns) {
			int base = ctx == Context.CLASS ? 1 : 2;
			// Get offsets from base for sort of context
			int from = -1;
			int to = -1;
			switch (this) {
				case OBF_TO_INTERMEDIATE:
					from = 0;
					to = 1;
					break;
				case OBF_TO_CLEAN:
					from = 0;
					to = 2;
					break;
				case INTERMEDIATE_TO_CLEAN:
					from = 1;
					to = 2;
					break;
				case INTERMEDIATE_TO_OBF:
					from = 1;
					to = 1;
					break;
				case CLEAN_TO_OBF:
					from = 2;
					to = 0;
					break;
				case CLEAN_TO_INTERMEDIATE:
					from = 1;
					to = 0;
					break;
				default:
					throw new IllegalStateException();
			}
			// Cap indices if no intermediate column exists
			if (!hasIntermediateColumn(ctx, columns)) {
				from = Math.min(1, from);
				to = Math.min(1, to);
			}
			return new int[]{base+from, base+to};
		}

		private boolean hasIntermediateColumn(Context ctx, int columns) {
			switch (ctx){
				case CLASS:
					return columns >= 4;
				case FIELD:
				case METHOD:
					return columns >= 5;
				default:
					throw new IllegalStateException();
			}
		}
	}

	private enum Context {
		CLASS, FIELD, METHOD;
	}
}
