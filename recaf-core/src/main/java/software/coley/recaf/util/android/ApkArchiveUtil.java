package software.coley.recaf.util.android;

import com.android.tools.build.apkzlib.zip.AlignmentRule;
import com.android.tools.build.apkzlib.zip.AlignmentRules;
import com.android.tools.build.apkzlib.zip.ExtraField;
import com.android.tools.build.apkzlib.zip.StoredEntry;
import com.android.tools.build.apkzlib.zip.ZFile;
import com.android.tools.build.apkzlib.zip.ZFileOptions;
import jakarta.annotation.Nonnull;
import software.coley.lljzip.ZipIO;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for assisting with writing APK archives.
 *
 * @author Matt Coley
 */
public final class ApkArchiveUtil {
	private static final int NATIVE_LIBRARY_ALIGNMENT = 16 * 1024;
	private static final AlignmentRule APK_ALIGNMENT_RULE = AlignmentRules.compose(
			path -> path.length() >= 3 && path.regionMatches(true, path.length() - 3, ".so", 0, 3)
					? NATIVE_LIBRARY_ALIGNMENT : AlignmentRule.NO_ALIGNMENT, AlignmentRules.constant(4));

	private ApkArchiveUtil() {}

	/**
	 * Aligns uncompressed entry data in an APK archive.
	 *
	 * @param zipBytes
	 * 		ZIP archive bytes. Assumed to have no leading/prefix bytes.
	 *
	 * @return Aligned archive bytes.
	 *
	 * @throws IOException
	 * 		When the archive cannot be aligned or the temporary file cannot be removed.
	 */
	@Nonnull
	public static byte[] align(@Nonnull byte[] zipBytes) throws IOException {
		Path temporaryFile = Files.createTempFile("recaf-apk-alignment-", ".zip");
		IOException failure = null;
		try {
			Files.write(temporaryFile, zipBytes);
			ZFileOptions options = new ZFileOptions()
					.setCoverEmptySpaceUsingExtraField(false) // Messes with our ExtraFieldTime parsing, use other alignment method.
					.setAlignmentRule(APK_ALIGNMENT_RULE);
			try (ZFile zipFile = ZFile.openReadWrite(temporaryFile.toFile(), options)) {
				// Record local extra fields before realignment.
				Map<String, ExtraField> localExtras = new HashMap<>();
				for (StoredEntry entry : zipFile.entries())
					localExtras.put(entry.getCentralDirectoryHeader().getName(), entry.getLocalExtra());

				// Realign the archive. This will rewrite the ZIP file, but also remove any local extra fields...
				zipFile.realign();

				// So we have to go back and restore those fields.
				for (StoredEntry entry : zipFile.entries()) {
					String name = entry.getCentralDirectoryHeader().getName();
					ExtraField localExtra = localExtras.get(name);
					if (localExtra != null && localExtra != entry.getLocalExtra())
						entry.setLocalExtra(localExtra);
				}
			}
			byte[] alignedBytes = Files.readAllBytes(temporaryFile);

			// After alignment sometimes the local file headers and central directory headers
			// can have their data-descriptor flags (bit 3 of the general-purpose bit flag) desync.
			normalizeDataDescriptorFlags(alignedBytes);

			return alignedBytes;
		} catch (IOException ex) {
			failure = ex;
			throw ex;
		} catch (RuntimeException ex) {
			failure = new IOException("Failed to align APK archive", ex);
			throw failure;
		} finally {
			try {
				Files.deleteIfExists(temporaryFile);
			} catch (IOException cleanupFailure) {
				if (failure != null)
					failure.addSuppressed(cleanupFailure);
				else
					throw cleanupFailure;
			}
		}
	}

	/**
	 * Copies the data-descriptor flag from each local header to its central-directory header.
	 * apkzlib can leave these bits out of sync after realignment, and apksig rejects the resulting APK.
	 * Only the flag bit is changed, so aligned entry offsets remain intact.
	 *
	 * @param zipBytes
	 * 		Aligned ZIP archive bytes.
	 *
	 * @throws IOException
	 * 		When the archive cannot be parsed.
	 */
	private static void normalizeDataDescriptorFlags(byte[] zipBytes) throws IOException {
		ZipArchive archive = ZipIO.readJvm(zipBytes);
		for (LocalFileHeader localFile : archive.getLocalFiles()) {
			CentralDirectoryFileHeader central = localFile.getLinkedDirectoryFileHeader();
			if (central == null)
				continue;

			int localFlagOffset = Math.toIntExact(localFile.offset() + 6);
			int centralFlagOffset = Math.toIntExact(central.offset() + 8);
			int localFlags = zipBytes[localFlagOffset] & 0xff;
			int centralFlags = zipBytes[centralFlagOffset] & 0xff;
			zipBytes[centralFlagOffset] = (byte) ((centralFlags & ~0x08) | (localFlags & 0x08));
		}
	}
}
