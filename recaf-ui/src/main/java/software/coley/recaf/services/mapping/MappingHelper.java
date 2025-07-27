package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.mapping.format.InvalidMappingException;
import software.coley.recaf.services.mapping.format.MappingFileFormat;
import software.coley.recaf.ui.menubar.MappingMenu;
import software.coley.recaf.ui.pane.MappingApplicationPane;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Shared mapping helper utility for UI logic.
 *
 * @author Matt Coley
 * @see MappingMenu
 * @see MappingApplicationPane
 */
@ApplicationScoped
public class MappingHelper {
	private static final Logger logger = Logging.get(MappingHelper.class);
	private final ExecutorService exportPool = ThreadPoolFactory.newSingleThreadExecutor("mapping-export");
	private final ExecutorService importPool = ThreadPoolFactory.newSingleThreadExecutor("mapping-import");
	private final MappingApplierService applierService;
	private final AggregateMappingManager aggregateMappingManager;

	@Inject
	public MappingHelper(@Nonnull MappingApplierService applierService, @Nonnull AggregateMappingManager aggregateMappingManager) {
		this.applierService = applierService;
		this.aggregateMappingManager = aggregateMappingManager;
	}

	@Nonnull
	public IntermediateMappings parse(@Nonnull MappingFileFormat format, @Nonnull Path mappingFile) throws IOException, InvalidMappingException {
		String mappingsText = Files.readString(mappingFile);
		IntermediateMappings parsedMappings = format.parse(mappingsText);
		logger.info("Loaded mappings from {} in {} format", mappingFile.getFileName(), format.implementationName());
		return parsedMappings;
	}

	public void applyMappings(@Nonnull MappingFileFormat format, @Nonnull Mappings mappings) {
		importPool.submit(() -> {
			try {
				MappingResults results = Objects.requireNonNull(applierService.inCurrentWorkspace()).applyToPrimaryResource(mappings);
				results.apply();
				logger.info("Applied mappings - Updated {} classes", results.getPostMappingPaths().size());
			} catch (Exception ex) {
				logger.error("Failed to read {} mappings", format.implementationName(), ex);
			}
		});
	}

	public void exportMappingsFile(@Nonnull MappingFileFormat format, @Nonnull Path mappingFile) {
		exportPool.submit(() -> {
			try {
				AggregatedMappings mappings = Objects.requireNonNull(aggregateMappingManager.getAggregatedMappings());
				String mappingsText = format.exportText(mappings);
				if (mappingsText != null) {
					Files.writeString(mappingFile, mappingsText);
					logger.info("Exporting mappings to {} in {} format", mappingFile.getFileName(), format.implementationName());
				} else {
					// We already checked for export support, so this should never happen
					throw new IllegalStateException("Mapping export shouldn't be null for format: " + format.implementationName());
				}
			} catch (Exception ex) {
				logger.error("Failed to write mappings in {} format to {}", format.implementationName(), mappingFile.getFileName(), ex);
			}
		});
	}
}
