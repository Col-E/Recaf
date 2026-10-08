package software.coley.recaf.workspace.model.bundle;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.file.HiddenApiData;

import java.util.List;

/**
 * Metadata retained for an individual DEX entry.
 *
 * @param classOrder
 * 		Original class-definition order.
 * 		Order plays a role in hidden-API data, which is our primary reason for retaining it.
 * @param hiddenApiLayout
 * 		Original encoded class/member layout used by hidden-API data.
 * @param hiddenApiData
 * 		Original hidden-API, or {@code null} when absent or discarded.
 * @param entryProperties
 * 		Selected source ZIP-entry properties.
 *
 * @author Matt Coley
 */
public record AndroidDexMetadata(@Nonnull List<String> classOrder,
                                 @Nonnull List<DexClassLayout> hiddenApiLayout,
                                 @Nullable HiddenApiData hiddenApiData,
                                 @Nonnull DexEntryProperties entryProperties) {
	/**
	 * @return Empty metadata.
	 */
	@Nonnull
	public static AndroidDexMetadata empty() {
		return new AndroidDexMetadata(List.of(), List.of(), null, DexEntryProperties.empty());
	}

	/**
	 * @return Copy without hidden-API contents.
	 */
	@Nonnull
	public AndroidDexMetadata withoutHiddenApiData() {
		return new AndroidDexMetadata(classOrder, List.of(), null, entryProperties);
	}

	/**
	 * Encoded class/member shape used to validate hidden-API offsets and flags.
	 *
	 * @param typeDescriptor
	 * 		Class type descriptor.
	 * @param staticFields
	 * 		Static fields as {@code name + descriptor}.
	 * @param instanceFields
	 * 		Instance fields as {@code name + descriptor}.
	 * @param directMethods
	 * 		Direct methods as {@code name + descriptor}.
	 * @param virtualMethods
	 * 		Virtual methods as {@code name + descriptor}.
	 */
	public record DexClassLayout(@Nonnull String typeDescriptor,
	                             @Nonnull List<String> staticFields,
	                             @Nonnull List<String> instanceFields,
	                             @Nonnull List<String> directMethods,
	                             @Nonnull List<String> virtualMethods) {}

	/**
	 * Selected properties copied from the source ZIP entry.
	 *
	 * @param compressionMethod
	 * 		Source ZIP compression method, or {@code null} when unknown.
	 * @param comment
	 * 		Source ZIP entry comment.
	 * @param creationTimeMillis
	 * 		Source entry creation time.
	 * @param modificationTimeMillis
	 * 		Source entry modification time.
	 * @param accessTimeMillis
	 * 		Source entry access time.
	 */
	public record DexEntryProperties(@Nullable Integer compressionMethod,
	                                 @Nullable String comment,
	                                 @Nullable Long creationTimeMillis,
	                                 @Nullable Long modificationTimeMillis,
	                                 @Nullable Long accessTimeMillis) {
		@Nonnull
		public static DexEntryProperties empty() {
			return new DexEntryProperties(null, null, null, null, null);
		}
	}
}
