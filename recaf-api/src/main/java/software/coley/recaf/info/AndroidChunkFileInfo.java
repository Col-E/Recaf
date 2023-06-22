package software.coley.recaf.info;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;

import javax.annotation.Nonnull;

/**
 * Outline of files that utilize the Android chunk format.
 *
 * @author Matt Coley
 * @see BinaryXmlFileInfo For {@code AndroidManifest.xml} contents.
 * @see ArscFileInfo For {@code resources.arsc} contents.
 */
public interface AndroidChunkFileInfo extends FileInfo {
	/**
	 * @return Model representation of the chunk file.
	 */
	@Nonnull
	BinaryResourceFile getChunkModel();
}
