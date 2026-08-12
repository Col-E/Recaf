package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.io.LargeByteArray;
import software.coley.recaf.util.io.LargeOutputStream;

import java.io.IOException;

/**
 * Export consumer to write to a {@code LargeOutputStream}. Only supports {@link WorkspaceOutputType#FILE}.
 *
 * @author Matt Coley
 */
public class ByteArrayWorkspaceExportConsumer implements WorkspaceExportConsumer {
	private LargeOutputStream output = new LargeOutputStream();

	@Override
	public void write(@Nonnull byte[] bytes) throws IOException {
		output.write(bytes);
	}

	@Override
	public void write(@Nonnull LargeByteArray data) throws IOException {
		output.write(data);
	}

	@Override
	public void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) {
		throw new IllegalStateException("Directory export not supported in byte-array export consumer");
	}

	@Override
	public void commit() throws IOException {
		// no-op
	}

	@Nonnull
	public LargeOutputStream getOutput() {
		return output;
	}
}
