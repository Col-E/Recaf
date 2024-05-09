package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.collections.func.UncheckedConsumer;
import software.coley.recaf.analytics.logging.Logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility for creating simple ZIP files.
 *
 * @author Matt Coley
 */
public class ZipCreationUtils {
	private static final byte[] EMPTY = new byte[0];
	private static final Logger logger = Logging.get(ZipCreationUtils.class);

	/**
	 * @param name
	 * 		Entry name.
	 * @param content
	 * 		Entry value.
	 *
	 * @return ZIP bytes, containing the single entry.
	 *
	 * @throws IOException
	 * 		When the content cannot be written.
	 */
	public static byte[] createSingleEntryZip(String name, byte[] content) throws IOException {
		return createZip(zos -> {
			zos.putNextEntry(new ZipEntry(name));
			zos.write(content);
			zos.closeEntry();
		});
	}

	/**
	 * @param entryMap
	 * 		Map of entry name --> contents.
	 *
	 * @return ZIP bytes, containing given entries.
	 *
	 * @throws IOException
	 * 		When the content cannot be written.
	 */
	public static byte[] createZip(Map<String, byte[]> entryMap) throws IOException {
		return createZip(zos -> {
			for (Map.Entry<String, byte[]> entry : entryMap.entrySet()) {
				zos.putNextEntry(new ZipEntry(entry.getKey()));
				zos.write(entry.getValue());
				zos.closeEntry();
			}
		});
	}

	/**
	 * @param consumer
	 * 		Action to do on the ZIP stream.
	 *
	 * @return ZIP bytes.
	 *
	 * @throws IOException
	 * 		When the action fails.
	 */
	public static byte[] createZip(UncheckedConsumer<ZipOutputStream> consumer) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			consumer.accept(zos);
		}
		return baos.toByteArray();
	}

	/**
	 * Resetting the tracked names allows you to write duplicate entries to a ZIP file.
	 *
	 * @param zos
	 * 		ZIP stream to reset name tracking of.
	 */
	private static void resetNames(ZipOutputStream zos) {
		try {
			Field field = ReflectUtil.getDeclaredField(ZipOutputStream.class, "names");
			Collection<?> names = (Collection<?>) field.get(zos);
			names.clear();
		} catch (Exception ex) {
			logger.error("Failed to reset ZIP name tracking: {}", zos, ex);
		}
	}


	/**
	 * @return New ZIP builder.
	 */
	public static ZipBuilder builder() {
		return new ZipBuilder();
	}

	/**
	 * Copied from {@code java.util.zip.ZipUtils}.
	 *
	 * @param time
	 * 		Time from extra field at some offset.
	 *
	 * @return NIO time representation.
	 */
	public static FileTime winTimeToFileTime(long time) {
		return FileTime.from(time / 10 + -11644473600000000L /* windows epoch */, TimeUnit.MICROSECONDS);
	}

	/**
	 * Copied from {@code java.util.zip.ZipUtils}.
	 *
	 * @param utime
	 * 		Time from extra field at some offset.
	 *
	 * @return NIO time representation.
	 */
	public static FileTime unixTimeToFileTime(long utime) {
		return FileTime.from(utime, TimeUnit.SECONDS);
	}

	/**
	 * Helper to create zip files.
	 */
	public static class ZipBuilder {
		private static final int MAX_DIR_DEPTH = 64;
		private final List<Entry> entries = new ArrayList<>();
		private boolean createDirectories;

		/**
		 * Enables creation of directory entries.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ZipBuilder createDirectories() {
			createDirectories = true;
			return this;
		}

		/**
		 * @param name
		 * 		Entry name.
		 * @param content
		 * 		Entry contents.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ZipBuilder add(@Nonnull String name, @Nonnull byte[] content) {
			return add(name, content, true, null, -1, -1, -1);
		}

		/**
		 * @param name
		 * 		Entry name.
		 * @param content
		 * 		Entry contents.
		 * @param compression
		 * 		Compression flag.
		 * @param comment
		 * 		Optional comment.
		 * @param createTime
		 * 		Creation time.
		 * @param modifyTime
		 * 		Modification time.
		 * @param accessTime
		 * 		Access time.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ZipBuilder add(@Nonnull String name, @Nonnull byte[] content, boolean compression,
							  @Nullable String comment, long createTime, long modifyTime, long accessTime) {
			return add(new Entry(name, content, compression, comment, null, createTime, modifyTime, accessTime));
		}

		/**
		 * @param entry
		 * 		Entry to add.
		 *
		 * @return Builder.
		 */
		@Nonnull
		private ZipBuilder add(@Nonnull Entry entry) {
			entries.add(entry);
			return this;
		}

		/**
		 * @return Generated ZIP.
		 *
		 * @throws IOException
		 * 		When the content cannot be written.
		 */
		@Nonnull
		public byte[] bytes() throws IOException {
			return createZip(zos -> {
				Set<String> dirsVisited = new HashSet<>();
				CRC32 crc = new CRC32();
				for (Entry entry : entries) {
					String key = entry.name;
					byte[] content = entry.content;

					// Write directories for upcoming entries if necessary
					// - Ugly, but does the job.
					if (createDirectories && key.contains("/")) {
						// Record directories
						String parent = key;
						List<String> toAdd = new ArrayList<>();
						do {
							// Abort if the max-dir depth is reached.
							if (toAdd.size() > MAX_DIR_DEPTH) {
								toAdd.clear();
								break;
							}
							parent = parent.substring(0, parent.lastIndexOf('/'));
							if (dirsVisited.add(parent)) {
								toAdd.add(0, parent + '/');
							} else break;
						} while (parent.contains("/"));
						// Put directories in order of depth
						for (String dir : toAdd) {
							// Update CRC
							crc.reset();
							crc.update(EMPTY);

							// Add the entry
							// We use STORED for directories so that the DEFLATE header doesn't clutter the
							// LocalFileHeader data store. Using STORE keeps it empty.
							ZipEntry dirEntry = new ZipEntry(dir);
							dirEntry.setSize(0);
							dirEntry.setCompressedSize(0);
							dirEntry.setMethod(ZipEntry.STORED);
							dirEntry.setCrc(crc.getValue());
							zos.putNextEntry(dirEntry);
							zos.closeEntry();
						}
					}

					// Update CRC
					crc.reset();
					crc.update(content);

					// Write ZIP entry
					//  - Always use STORED for empty files to save space.
					boolean doStore = entry.content.length == 0 || !entry.compression;
					int level = doStore ? ZipEntry.STORED : ZipEntry.DEFLATED;
					ZipEntry zipEntry = new ZipEntry(key);
					zipEntry.setMethod(level);
					zipEntry.setCrc(crc.getValue());
					if (doStore) {
						zipEntry.setSize(content.length);
						zipEntry.setCompressedSize(content.length);
					}
					if (entry.comment != null) zipEntry.setComment(entry.comment);
					if (entry.extra != null) zipEntry.setExtra(entry.extra);
					if (entry.creationTime >= 0L) zipEntry.setCreationTime(FileTime.fromMillis(entry.creationTime));
					if (entry.modifyTime >= 0L) zipEntry.setLastModifiedTime(FileTime.fromMillis(entry.modifyTime));
					if (entry.accessTime >= 0L) zipEntry.setLastAccessTime(FileTime.fromMillis(entry.accessTime));

					zos.putNextEntry(zipEntry);
					zos.write(content);
					zos.closeEntry();

					// Reset to allow name hacks
					resetNames(zos);
				}
			});
		}

		public static class Entry {
			private final String name;
			private final byte[] content;
			private final boolean compression;
			private final String comment;
			private final byte[] extra;
			private final long creationTime;
			private final long modifyTime;
			private final long accessTime;

			private Entry(@Nonnull String name,
						  @Nonnull byte[] content,
						  boolean compression,
						  @Nullable String comment,
						  @Nullable byte[] extra,
						  long creationTime,
						  long modifyTime,
						  long accessTime
			) {
				this.name = name;
				this.content = content;
				this.compression = compression;
				this.comment = comment;
				this.extra = extra;
				this.creationTime = creationTime;
				this.modifyTime = modifyTime;
				this.accessTime = accessTime;
			}
		}
	}
}
