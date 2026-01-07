package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file).
 * <br>
 * Retrieved 2011-09-23 from:
 * <a href="http://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java/672775#672775">
 * Windows shortcut (.lnk) parser in Java?</a> - Originally called LnkParser
 *
 * @author crysxd - Removing Apache VFS dependency
 * @author Headerified - Refactoring and comments
 * @author Stefan Cordes - Adaptation
 * @author Matt Coley - Minor additions
 */
public class ShortcutUtil {
	private boolean isDirectory;
	private boolean isLocal;
	private String realFile;

	/**
	 * @param path
	 * 		Path to follow.
	 * @param followDepth
	 * 		Maximum depth to follow symbolic links.
	 *
	 * @return Target of shortcut/symbolic link.
	 *
	 * @throws IOException
	 * 		When the shortcut could not be parsed.
	 */
	@Nonnull
	public static Path follow(Path path, int followDepth) throws IOException {
		try {
			// First check if it's a windows 'lnk' shortcut
			if (isPotentialValidLink(path))
				path = path.getFileSystem().getPath(new ShortcutUtil(path).getRealFilename());

			// Then traverse any symbolic links up to the given depth
			int symLevel = 0;
			while (true) {
				boolean visited = false;

				// Follow symbolic links
				while (Files.isSymbolicLink(path) && symLevel < followDepth) {
					path = Files.readSymbolicLink(path);
					symLevel++;
					visited = true;
				}

				// Follow symbolic links via real path resolution (as a fallback, since nio can be inconsistent)
				// This is primarily observed to occur with directory links vs file links.
				while (!path.equals(path.toRealPath())) {
					path = path.toRealPath();
					symLevel++;
					visited = true;
				}

				if (!visited)
					break;
			}
			return path;
		} catch (ParseException ex) {
			throw new IOException("Failed to parse shortcut/sym-link for path: " + path, ex);
		}
	}

	/**
	 * Provides a quick test to see if this could be a valid link.
	 *
	 * @param path
	 * 		The potential link
	 *
	 * @return {@code true} if the path may be a link, {@code false} otherwise
	 */
	public static boolean isPotentialValidLink(final Path path) {
		if (!Files.exists(path))
			return false;
		String extension = IOUtil.getExtension(path);
		if (!"lnk".equalsIgnoreCase(extension))
			return false;
		final int minimumLength = 0x64;
		try (InputStream fis = Files.newInputStream(path)) {
			if (fis.available() < minimumLength)
				return false;
			int bufSize = 64;
			byte[] buf = new byte[bufSize];
			int offset = 0;
			while (bufSize != 0) {
				int read = fis.read(buf, offset, bufSize);
				if (read == -1)
					break;
				bufSize -= read;
				offset += read;
			}
			return fis.available() >= minimumLength &&
					isMagicPresent(IOUtil.toByteArray(fis, Arrays.copyOf(buf, offset)));
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * @param path
	 * 		Path reference to shortcut.
	 *
	 * @throws IOException
	 * 		If the path cannot be read.
	 * @throws ParseException
	 * 		If the link cannot be read.
	 */
	public ShortcutUtil(final Path path) throws IOException, ParseException {
		try (InputStream in = Files.newInputStream(path)) {
			parseLink(IOUtil.toByteArray(in));
		}
	}

	/**
	 * @return the name of the filesystem object pointed to by this shortcut
	 */
	@Nonnull
	public String getRealFilename() {
		return realFile;
	}

	/**
	 * Tests if the shortcut points to a local resource.
	 *
	 * @return true if the 'local' bit is set in this shortcut, false otherwise
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * Tests if the shortcut points to a directory.
	 *
	 * @return true if the 'directory' bit is set in this shortcut, false otherwise
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	private static boolean isMagicPresent(final byte[] link) {
		final int magic = 0x0000004C;
		final int magicOffset = 0x00;
		return link.length >= 32 && bytesToDword(link, magicOffset) == magic;
	}

	/**
	 * Gobbles up link data by parsing it and storing info in member fields
	 *
	 * @param link
	 * 		all the bytes from the .lnk file
	 */
	private void parseLink(final byte[] link) throws ParseException {
		try {
			if (!isMagicPresent(link))
				throw new ParseException("Invalid shortcut; magic is missing", 0);

			// get the flags byte
			final byte flags = link[0x14];

			// get the file attributes byte
			final int fileAttsOffset = 0x18;
			final byte fileAtts = link[fileAttsOffset];
			final byte isDirMask = (byte) 0x10;
			isDirectory = (fileAtts & isDirMask) > 0;

			// if the shell settings are present, skip them
			final int shellOffset = 0x4c;
			final byte hasShellMask = (byte) 0x01;
			int shellLen = 0;
			if ((flags & hasShellMask) > 0) {
				// the plus 2 accounts for the length marker itself
				shellLen = bytesToWord(link, shellOffset) + 2;
			}

			// get to the file settings
			final int fileStart = 0x4c + shellLen;

			final int fileLocationInfoFlagOffsetOffset = 0x08;
			final int fileLocationInfoFlag =
					link[fileStart + fileLocationInfoFlagOffsetOffset];
			isLocal = (fileLocationInfoFlag & 2) == 0;
			// get the local volume and local system values
			//final int localVolumeTable_offset_offset = 0x0C;
			final int basenameOffsetOffset = 0x10;
			final int networkVolumeTableOffsetOffset = 0x14;
			final int finalnameOffsetOffset = 0x18;
			final int finalnameOffset = link[fileStart + finalnameOffsetOffset] + fileStart;
			final String finalname = getNullDelimitedString(link, finalnameOffset);
			if (isLocal) {
				final int basenameOffset = link[fileStart + basenameOffsetOffset] + fileStart;
				final String basename = getNullDelimitedString(link, basenameOffset);
				realFile = basename + finalname;
			} else {
				final int networkVolumeTableOffset =
						link[fileStart + networkVolumeTableOffsetOffset] + fileStart;
				final int shareNameOffsetOffset = 0x08;
				final int shareNameOffset =
						link[networkVolumeTableOffset + shareNameOffsetOffset] + networkVolumeTableOffset;
				final String shareName = getNullDelimitedString(link, shareNameOffset);
				realFile = shareName + "\\" + finalname;
			}
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw new ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0);
		}
	}

	@Nonnull
	private static String getNullDelimitedString(final byte[] bytes, final int off) {
		// count bytes until the null character (0)
		int len = 0;
		while (bytes[off + len] != 0)
			len++;
		return new String(bytes, off, len);
	}

	/*
	 * Convert two bytes into a short note, this is little endian because it's
	 * for an Intel only OS.
	 */
	private static int bytesToWord(final byte[] bytes, final int off) {
		return ((bytes[off + 1] & 0xff) << 8) | (bytes[off] & 0xff);
	}

	private static int bytesToDword(final byte[] bytes, final int off) {
		return (bytesToWord(bytes, off + 2) << 16) | bytesToWord(bytes, off);
	}
}