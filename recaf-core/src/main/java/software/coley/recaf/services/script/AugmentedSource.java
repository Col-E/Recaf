package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;

/**
 * Model for some input source code, with the original source, the augmented source, and mappings between the two.
 *
 * @param originalSource
 * 		Original provided source.
 * @param augmentedSource
 * 		Augmented source based on the original, with changes to facilitate additional features,
 * 		or to fit the expected shape of a Java class.
 * @param packageName
 * 		Final package name of the class source.
 * @param originalToAugmentedOffsets
 * 		Mapping from original offsets to augmented offsets.
 * @param augmentedLineToOriginalLines
 * 		Mapping from 1-based augmented lines to 1-based original lines.
 */
public record AugmentedSource(@Nonnull String originalSource,
                              @Nonnull String augmentedSource,
                              @Nonnull String packageName,
                              @Nonnull int[] originalToAugmentedOffsets,
                              @Nonnull int[] augmentedLineToOriginalLines) {
	/**
	 * @param originalOffset
	 * 		Offset in the original source.
	 *
	 * @return Offset in the augmented source.
	 */
	public int mapOriginalToAugmented(int originalOffset) {
		if (originalOffset < 0)
			return originalOffset;
		if (originalOffset >= originalToAugmentedOffsets.length)
			return originalToAugmentedOffsets[originalToAugmentedOffsets.length - 1];
		return originalToAugmentedOffsets[originalOffset];
	}

	/**
	 * @param augmentedLine
	 * 		1-based line number in the augmented source.
	 *
	 * @return 1-based line number in the original source.
	 */
	public int mapAugmentedLineToOriginal(int augmentedLine) {
		if (augmentedLine < 1)
			return augmentedLine;
		if (augmentedLine >= augmentedLineToOriginalLines.length)
			return augmentedLineToOriginalLines[augmentedLineToOriginalLines.length - 1];
		return augmentedLineToOriginalLines[augmentedLine];
	}

	/**
	 * @return Internal package name.
	 */
	@Nonnull
	public String packageInternalName() {
		return packageName.replace('.', '/');
	}

	/**
	 * @param newAugmentedSource
	 * 		Replacement augmented source.
	 *
	 * @return Copy with a new augmented source.
	 */
	@Nonnull
	public AugmentedSource withAugmentedSource(@Nonnull String newAugmentedSource) {
		return new AugmentedSource(originalSource, newAugmentedSource, packageName,
				originalToAugmentedOffsets, augmentedLineToOriginalLines);
	}
}
