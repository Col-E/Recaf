package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * Export consumer to write to a {@code byte[]}. Only supports {@link WorkspaceOutputType#FILE}.
 *
 * @author Matt Coley
 */
public class ByteArrayWorkspaceExportConsumer implements WorkspaceExportConsumer {
	private byte[] output;

	@Override
	public void write(@Nonnull byte[] bytes) throws IOException {
		if (output == null)
			output = bytes;
		else {
			int existingContentLength = output.length;
			int newContentLength = bytes.length;
			int mergedLength = existingContentLength + newContentLength;
			if (mergedLength < 0) // Overflow check
				throw new IllegalStateException("Content too large to write to a single byte[]");
			byte[] newOutput = new byte[mergedLength];
			System.arraycopy(output, 0, newOutput, 0, existingContentLength);
			System.arraycopy(bytes, 0, newOutput, existingContentLength, newContentLength);
			output = newOutput;
		}
	}

	@Override
	public void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) {
		throw new IllegalStateException("Directory export not supported in byte-array export consumer");
	}

	@Override
	public void commit() throws IOException {
		// no-op
	}

	/**
	 * @return Output content. May be {@code null} if nothing was in the workspace to write.
	 */
	@Nullable
	public byte[] getOutput() {
		return output;
	}
}
