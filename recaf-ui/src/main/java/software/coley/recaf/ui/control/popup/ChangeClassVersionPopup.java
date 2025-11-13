package software.coley.recaf.ui.control.popup;

import atlantafx.base.theme.Styles;
import com.google.common.util.concurrent.AtomicDouble;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.JavaDowngraderUtil;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.io.IOException;
import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;
import static software.coley.recaf.util.Lang.get;
import static software.coley.recaf.util.Lang.getBinding;

/**
 * Popup for initiating the upgrading or downgrading of all classes.
 *
 * @author Matt Coley
 */
public class ChangeClassVersionPopup extends RecafStage {
	private static final Logger logger = Logging.get(ChangeClassVersionPopup.class);
	private TargetClasses target;

	/**
	 * New version change popup.
	 */
	public ChangeClassVersionPopup() {
		Tab tabUp = new BoundTab(getBinding("menu.edit.changeversion.up"), ARROW_UP,
				createPane("up", (version, progressSink) -> transform(true, version, progressSink)));
		Tab tabDown = new BoundTab(getBinding("menu.edit.changeversion.down"), ARROW_DOWN,
				createPane("down", (version, progressSink) -> transform(false, version, progressSink)));
		TabPane tabs = new TabPane(tabUp, tabDown);
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

		setMinWidth(450);
		setMinHeight(200);
		setTitle(get("menu.edit.changeversion"));
		setScene(new RecafScene(tabs, 400, 150));
	}

	/**
	 * @param bundle
	 * 		Bundle to target.
	 * @param classInfo
	 * 		Class in bundle to target.
	 */
	public void setTargetClass(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo classInfo) {
		this.target = new TargetClasses.JvmClassFile(bundle, classInfo);
	}

	/**
	 * @param bundle
	 * 		Bundle to target.
	 * @param packageName
	 * 		Package in bundle to target.
	 */
	public void setTargetPackage(@Nonnull JvmClassBundle bundle, @Nonnull String packageName) {
		this.target = new TargetClasses.JvmPackage(bundle, packageName);
	}

	/**
	 * @param bundle
	 * 		Bundle to target.
	 */
	public void setTargetBundle(@Nonnull JvmClassBundle bundle) {
		this.target = new TargetClasses.JvmBundle(bundle);
	}

	private int transform(boolean upgrade, int targetJavaVersion, @Nonnull DoubleConsumer sink) {
		Map<String, ClassInfo> transformMap = new HashMap<>();
		target.each(c -> transformMap.put(c.getName(), c));

		int total = transformMap.size();
		AtomicDouble remaining = new AtomicDouble(total);

		ExecutorService service = ThreadPoolFactory.newFixedThreadPool("class-version-transform");
		if (upgrade) {
			transformMap.forEach((name, classInfo) -> {
				CompletableFuture.supplyAsync(() -> {
					try {
						if (classInfo instanceof JvmClassInfo jvmClass) {
							// Convert class file version to Java version.
							int classVersion = jvmClass.getVersion() - JavaVersion.VERSION_OFFSET;

							// Upgrade or downgrade when appropriate.
							if (classVersion < targetJavaVersion)
								return upgrade(targetJavaVersion, classInfo);
						}
						return null;
					} finally {
						sink.accept(1 - (remaining.addAndGet(-1) / total));
					}
				}, service).whenComplete((updated, error) -> {
					// Updated can be null when a class did not fit the upgrade criterion or if there was an error.
					if (updated != null)
						target.update(updated);
				});
			});
		} else {
			Map<String, byte[]> bytesMap = new HashMap<>(transformMap.size());
			Map<String, byte[]> resultsMap = new HashMap<>(transformMap.size());
			transformMap.forEach((name, classInfo) -> bytesMap.put(name, classInfo.asJvmClass().getBytecode()));
			CompletableFuture.runAsync(() -> {
				try {
					JavaDowngraderUtil.downgrade(targetJavaVersion, bytesMap, (name, downgradedBytes) -> {
						resultsMap.put(name, downgradedBytes);
						sink.accept(1 - (remaining.addAndGet(-1) / total));
					});
				} catch (IOException ex) {
					logger.error("Downgrade transformer failed to initialize", ex);
				}
			}, service).whenComplete((_, error) -> {
				resultsMap.forEach((name, bytes) -> {
					try {
						target.update(new JvmClassInfoBuilder(bytes).build());
					} catch (Throwable t) {
						logger.error("Failed updating workspace with downgraded class '{}'", name, t);
					}
				});
			});
		}

		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException ex) {
			logger.info("Class version transformation interrupted", ex);
		}
		return total;
	}

	@Nullable
	private static JvmClassInfo upgrade(int javaVersion, @Nonnull ClassInfo classInfo) {
		if (classInfo instanceof JvmClassInfo jvmClassInfo) {
			try {
				// Convert the target Java version to the intended class file version.
				int targetClassVersion = javaVersion + JavaVersion.VERSION_OFFSET;

				int baseClassVersion = jvmClassInfo.getVersion();
				if (baseClassVersion <= JavaVersion.VERSION_OFFSET + 7 && !classInfo.hasInterfaceModifier()) {
					// We need to use ASM's JSRInlinerAdapter to ensure JSR/RET instructions are removed from
					// the updated copy of the class file.
					ClassReader reader = jvmClassInfo.getClassReader();
					ClassWriter writer = new ClassWriter(reader, 0);
					reader.accept(new ClassVisitor(RecafConstants.getAsmVersion(), writer) {
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
							super.visit(targetClassVersion, access, name, signature, superName, interfaces);
						}

						@Override
						public MethodVisitor visitMethod(int access, String name, String descriptor,
						                                 String signature, String[] exceptions) {
							MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
							return new JSRInlinerAdapter(mv, access, name, descriptor, signature, exceptions);
						}
					}, jvmClassInfo.getClassReaderFlags());
					return new JvmClassInfoBuilder(writer.toByteArray()).build();
				} else {
					// No special handling needed, just update the version value.
					byte[] original = jvmClassInfo.getBytecode();
					byte[] copy = Arrays.copyOf(original, original.length);
					copy[7] = (byte) targetClassVersion;
					return jvmClassInfo.toJvmClassBuilder()
							.withBytecode(copy)
							.withVersion(targetClassVersion)
							.build();
				}
			} catch (Exception ex) {
				logger.error("Failed downgrading class '{}'", classInfo.getName(), ex);
				return null;
			}
		}
		return null;
	}

	@Nonnull
	private GridPane createPane(@Nonnull String suffix, @Nonnull UpdateHandler versionAction) {
		GridPane layout = new GridPane(8, 8);
		Label labelVersion = new BoundLabel(getBinding("java.targetversion"));
		VersionComboBox versionCombo = new VersionComboBox();
		ProgressBar progressBar = new ProgressBar(0);
		Button applyButton = new ActionButton(RUN, getBinding("menu.edit.changeversion." + suffix), () -> {
			versionCombo.setDisable(true);
			int targetVersion = versionCombo.getValue();
			CompletableFuture.supplyAsync(() -> versionAction.accept(targetVersion, progress -> {
				// Update progress bar when the handler reports progress
				FxThreadUtil.run(() -> progressBar.setProgress(progress));
			})).whenCompleteAsync((count, error) -> {
				if (error == null) {
					// Done with upgrade/downgrade, close the window
					logger.info("Changed {} classes to target version {}", count, targetVersion);
					hide();
				} else {
					logger.error("Failed to change class target version to {}", targetVersion, error);
				}
			}, FxThreadUtil.executor());
		});
		applyButton.disableProperty().bind(versionCombo.disabledProperty());

		versionCombo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		applyButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		progressBar.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		layout.add(labelVersion, 0, 0);
		layout.add(versionCombo, 1, 0);

		if (suffix.equals("down")) {
			Label labelNotice = new BoundLabel(getBinding("java.targetversion.notice." + suffix));
			labelNotice.getStyleClass().addAll(Styles.TEXT_SUBTLE, Styles.TEXT_ITALIC);
			layout.add(labelNotice, 0, layout.getRowCount(), 2, 1);
		}

		layout.add(applyButton, 0, layout.getRowCount(), 2, 1);
		layout.add(progressBar, 0, layout.getRowCount(), 2, 1);
		layout.setPadding(new Insets(10));
		layout.setAlignment(Pos.TOP_CENTER);
		return layout;
	}

	/**
	 * Outline of upgrade/downgrade process.
	 */
	private interface UpdateHandler {
		/**
		 * @param version
		 * 		Version to upgrade/downgrade to.
		 * @param progressSink
		 * 		Sink for progress reporting.
		 *
		 * @return Total number of classes updated.
		 */
		int accept(int version, @Nonnull DoubleConsumer progressSink);
	}

	/**
	 * Combo box to hold supported target versions.
	 */
	private static class VersionComboBox extends ComboBox<Integer> {
		private VersionComboBox() {
			int max = releaseCycleAvailability();
			for (int i = JavacCompiler.MIN_DOWNSAMPLE_VER; i <= max; i++)
				getItems().add(i);
			setValue(8);
			setConverter(ToStringConverter.from(String::valueOf));
		}

		/**
		 * @return Expected version of Java to exist for the current year, based on the 2-per-year release cycle.
		 */
		private static int releaseCycleAvailability() {
			// Java is now on a release cycle of 2 versions every year.
			// The year is based on 2020 to accommodate for 'early-access'
			int year = Year.now().getValue();
			return 17 + (year - 2020) * 2;
		}
	}

	/**
	 * Model of what classes to change.
	 */
	private sealed interface TargetClasses {
		void each(@Nonnull Consumer<ClassInfo> consumer);

		void update(@Nonnull ClassInfo classInfo);

		record JvmClassFile(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo classInfo) implements TargetClasses {

			@Override
			public void each(@Nonnull Consumer<ClassInfo> consumer) {
				consumer.accept(classInfo);
			}

			@Override
			public void update(@Nonnull ClassInfo classInfo) {
				bundle.put(classInfo.asJvmClass());
			}
		}

		record JvmPackage(@Nonnull JvmClassBundle bundle, @Nullable String packageName) implements TargetClasses {
			@Override
			public void each(@Nonnull Consumer<ClassInfo> consumer) {
				if (packageName == null || packageName.isEmpty())
					bundle.stream()
							.filter(c -> c.getPackageName() == null)
							.forEach(consumer);
				else
					bundle.stream()
							.filter(c -> {
								String itPackageName = c.getPackageName();
								return itPackageName != null && itPackageName.startsWith(packageName);
							})
							.forEach(consumer);
			}

			@Override
			public void update(@Nonnull ClassInfo classInfo) {
				bundle.put(classInfo.asJvmClass());
			}
		}

		record JvmBundle(@Nonnull JvmClassBundle bundle) implements TargetClasses {
			@Override
			public void each(@Nonnull Consumer<ClassInfo> consumer) {
				bundle.forEach(consumer);
			}

			@Override
			public void update(@Nonnull ClassInfo classInfo) {
				bundle.put(classInfo.asJvmClass());
			}
		}
	}
}
