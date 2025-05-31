package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.collections.func.UncheckedFunction;
import software.coley.lljzip.ZipIO;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.JvmZipReader;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

import java.util.List;

/**
 * Config for {@link ResourceImporter}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceImporterConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableObject<ZipStrategy> zipStrategy = new ObservableObject<>(ZipStrategy.JVM);
	private final ObservableBoolean skipRevisitedCenToLocalLinks = new ObservableBoolean(true);
	private final ObservableBoolean allowBasicJvmBaseOffsetZeroCheck = new ObservableBoolean(true);
	private final ObservableBoolean ignoreNaiveFileLengths = new ObservableBoolean(false);

	@Inject
	public ResourceImporterConfig() {
		super(ConfigGroups.SERVICE_IO, ResourceImporter.SERVICE_ID + CONFIG_SUFFIX);

		addValue(new BasicConfigValue<>("zip-strategy", ZipStrategy.class, zipStrategy));
		addValue(new BasicConfigValue<>("skip-revisited-cen-to-local-links", boolean.class, skipRevisitedCenToLocalLinks));
		addValue(new BasicConfigValue<>("allow-basic-base-offset-zero-check", boolean.class, allowBasicJvmBaseOffsetZeroCheck));
		addValue(new BasicConfigValue<>("ignore-naive-file-lengths", boolean.class, ignoreNaiveFileLengths));
	}

	/**
	 * @return ZIP strategy.
	 */
	@Nonnull
	public ObservableObject<ZipStrategy> getZipStrategy() {
		return zipStrategy;
	}

	/**
	 * When the {@link #getZipStrategy() ZIP strategy} is {@link ZipStrategy#JVM} this allows toggling
	 * skipping <i>"duplicate"</i> entries where multiple {@link CentralDirectoryFileHeader} can point to the
	 * same offset <i>({@link LocalFileHeader})</i>. Skipping is {@code true} by default.
	 *
	 * @return {@code true} when skipping N-to-1 mapping of
	 * {@link CentralDirectoryFileHeader} to {@link LocalFileHeader} for {@link ZipStrategy#JVM}.
	 */
	@Nonnull
	public ObservableBoolean getSkipRevisitedCenToLocalLinks() {
		return skipRevisitedCenToLocalLinks;
	}

	/**
	 * When the {@link #getZipStrategy() ZIP strategy} is {@link ZipStrategy#JVM} this allows toggling
	 * how the JVM base offset of the zip file is calculated. Normally the start of a ZIP file is calculated
	 * based off the logic in {@code ZipFile.Source#findEND()} which looks like:
	 * <pre>{@code
	 *  // ENDSIG matched, however the size of file comment in it does
	 *  // not match the real size. One "common" cause for this problem
	 *  // is some "extra" bytes are padded at the end of the zipfile.
	 *  // Let's do some extra verification, we don't care about the
	 *  // performance in this situation.
	 *  byte[] sbuf = new byte[4];
	 *  long cenpos = end.endpos - end.cenlen;
	 *  long locpos = cenpos - end.cenoff;
	 * }</pre>
	 * In some edge cases this results in {@code locpos} being {@code > 0} even when the file has no prefix/padding.
	 *
	 * @return {@code true} when defaulting to check for zero being the base JVM zip offset instead of the lookup
	 * based on the code in {@code ZipFile.Source#findEND()}.
	 */
	@Nonnull
	public ObservableBoolean getAllowBasicJvmBaseOffsetZeroCheck() {
		return allowBasicJvmBaseOffsetZeroCheck;
	}

	/**
	 * Post-processes naively read zip archives to expand file data to the next zip structure boundary.
	 * This is necessary in some cases where an archive composed entirely of local file headers has bogus
	 * file length values <i>(such as zero)</i> when file data appears after the file name in the zip structure.
	 *
	 * @return {@code true} to ignore the reported file lengths when using {@link ZipStrategy#NAIVE}.
	 */
	@Nonnull
	public ObservableBoolean getIgnoreNaiveFileLengths() {
		return ignoreNaiveFileLengths;
	}

	/**
	 * @return Mapping of input bytes to a ZIP archive model.
	 */
	@Nonnull
	public UncheckedFunction<byte[], ZipArchive> mapping() {
		ZipStrategy strategy = zipStrategy.getValue();
		if (strategy == ZipStrategy.JVM)
			return input -> ZipIO.read(input, new JvmZipReader(skipRevisitedCenToLocalLinks.getValue(),
					allowBasicJvmBaseOffsetZeroCheck.getValue()));
		if (strategy == ZipStrategy.STANDARD)
			return ZipIO::readStandard;
		return input -> {
			ZipArchive archive = ZipIO.readNaive(input);
			if (ignoreNaiveFileLengths.getValue()) {
				List<LocalFileHeader> items = archive.getLocalFiles();
				for (int i = 0; i < items.size(); i++) {
					LocalFileHeader lfh = items.get(i);
					long nextStart = i < items.size() - 1 ?
							items.get(i + 1).offset() :
							input.length;
					long expectedEnd = lfh.offset() + lfh.length();
					if (expectedEnd < nextStart) {
						// TODO: Subtract nextStart here if data-descriptor is present
						lfh.setFileDataEndOffset(nextStart);
					}
				}
			}
			return archive;
		};
	}

	/**
	 * Mirrors strategies available in {@link ZipIO}.
	 */
	public enum ZipStrategy {
		JVM,
		STANDARD,
		NAIVE
	}
}
