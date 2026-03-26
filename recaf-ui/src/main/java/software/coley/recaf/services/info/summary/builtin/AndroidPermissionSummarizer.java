package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import com.google.devrel.gmscore.tools.apk.arsc.ChunkWithChunks;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import com.google.devrel.gmscore.tools.apk.arsc.XmlStartElementChunk;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.Label;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Summarizer that shows requested permissions from an Android application.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidPermissionSummarizer implements ResourceSummarizer {
	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.permissions"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});

		// Visit Android manifest for permissions.
		int[] found = {0};
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource currentResource = resourceQueue.remove();
			FileInfo manifest = currentResource.getFileBundle().get("AndroidManifest.xml");
			if (manifest instanceof BinaryXmlFileInfo manifestXmlInfo) {
				BinaryResourceFile chunkModel = manifestXmlInfo.getChunkModel();

				// Extract string pool chunk to pull values from.
				StringPoolChunk stringChunk = manifestXmlInfo.getStringPoolChunk();
				if (stringChunk == null)
					continue;

				// Walk through all chunks and their children to find permission attributes.
				Queue<Chunk> chunkQueue = new ArrayDeque<>(chunkModel.getChunks());
				while (!chunkQueue.isEmpty()) {
					Chunk chunk = chunkQueue.remove();
					if (chunk instanceof XmlStartElementChunk start) {
						// If the chunk is a permission chunk then we want to look for the "name" attribute.
						// Otherwise, we look for the "permission" attribute.
						boolean isPermissionChunk = "uses-permission".equals(start.getName()) || "permission".equals(start.getName());
						for (XmlAttribute attribute : start.getAttributes()) {
							String name = stringChunk.getString(attribute.nameIndex());
							if (!(isPermissionChunk ? "name" : "permission").equals(name))
								continue;
							String permission = stringChunk.getString(attribute.rawValueIndex());
							found[0]++;
							batch.add(() -> consumer.appendSummary(new Label(permission)));
						}
					}

					// Add children
					if (chunk instanceof ChunkWithChunks chunkWithChunks)
						chunkQueue.addAll(chunkWithChunks.getChunks().values());
				}
			}
			resourceQueue.addAll(currentResource.getEmbeddedResources().values());
		}

		if (found[0] > 0) {
			batch.execute();
			return true;
		}
		return false;
	}
}
