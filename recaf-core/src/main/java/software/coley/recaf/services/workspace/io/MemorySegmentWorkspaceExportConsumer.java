package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.MemorySegmentUtil;
import software.coley.recaf.util.io.LargeOutputStream;

import java.io.IOException;
import java.lang.foreign.MemorySegment;

/**
 * Export consumer to write to a {@code MemorySegment}. Only supports {@link WorkspaceOutputType#FILE}.
 *
 * @author Matt Coley
 */
public class MemorySegmentWorkspaceExportConsumer implements WorkspaceExportConsumer {
	private LargeOutputStream output = new LargeOutputStream();

	@Override
	public void write(@Nonnull byte[] bytes) throws IOException {
		output.write(bytes);
	}

	@Override
	public void write(@Nonnull MemorySegment data) throws IOException {
		for (var chunk : MemorySegmentUtil.toChunks(data)) {
			output.write(chunk);
		}
	}

	@Override
	public void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) {
		throw new IllegalStateException("Directory export not supported in byte-array export consumer");
	}

	@Override
	public void writeRelative(@Nonnull String relative, @Nonnull MemorySegment bytes) {
		throw new IllegalStateException("Directory export not supported in byte-array export consumer");
	}

	@Override
	public void commit() throws IOException {
		// no-op
	}

	@Nonnull
	public MemorySegment getOutput() {
		return output.toMemorySegment();
	}
}
