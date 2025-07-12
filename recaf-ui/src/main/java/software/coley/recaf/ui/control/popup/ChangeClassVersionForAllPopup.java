package software.coley.recaf.ui.control.popup;

import com.google.common.util.concurrent.AtomicDouble;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import net.raphimc.javadowngrader.JavaDowngrader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;
import static software.coley.recaf.util.Lang.get;
import static software.coley.recaf.util.Lang.getBinding;

/**
 * Popup for initiating the upgrading or downgrading of all classes.
 *
 * @author Matt Coley
 */
@Dependent
public class ChangeClassVersionForAllPopup extends RecafStage {
	private static final Logger logger = Logging.get(ChangeClassVersionForAllPopup.class);
	private JvmClassBundle targetBundle;

	@Inject
	public ChangeClassVersionForAllPopup(@Nonnull Workspace workspace) {
		targetBundle = workspace.getPrimaryResource().getJvmClassBundle();

		Tab tabUp = new BoundTab(getBinding("menu.edit.changeversion.up"), ARROW_UP, createPane("up", this::upgrade));
		Tab tabDown = new BoundTab(getBinding("menu.edit.changeversion.down"), ARROW_DOWN, createPane("down", this::downgrade));
		TabPane tabs = new TabPane(tabUp, tabDown);
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

		setMinWidth(450);
		setMinHeight(200);
		setTitle(get("menu.edit.changeversion"));
		setScene(new RecafScene(tabs, 400, 150));
	}

	private int upgrade(int target, @Nonnull DoubleConsumer sink) {
		int classTarget = JavaVersion.VERSION_OFFSET + target;

		List<JvmClassInfo> targets = targetBundle.stream()
				.filter(c -> c.getVersion() < classTarget)
				.collect(Collectors.toCollection(ArrayList::new));
		int total = targets.size();
		AtomicDouble remaining = new AtomicDouble(target);

		ExecutorService service = ThreadPoolFactory.newFixedThreadPool("class-upgrade");
		for (JvmClassInfo classInfo : targets) {
			CompletableFuture.supplyAsync(() -> {
				try {
					int baseClassVersion = classInfo.getVersion();
					JvmClassInfo result;
					if (baseClassVersion <= JavaVersion.VERSION_OFFSET + 7 && !classInfo.hasInterfaceModifier()) {
						// We need to use ASM's JSRInlinerAdapter to ensure JSR/RET instructions are removed from
						// the updated copy of the class file.
						ClassReader reader = classInfo.getClassReader();
						ClassWriter writer = new ClassWriter(reader, 0);
						reader.accept(new ClassVisitor(RecafConstants.getAsmVersion(), writer) {
							@Override
							public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
								super.visit(classTarget, access, name, signature, superName, interfaces);
							}

							@Override
							public MethodVisitor visitMethod(int access, String name, String descriptor,
															 String signature, String[] exceptions) {
								MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
								return new JSRInlinerAdapter(mv, access, name, descriptor, signature, exceptions);
							}
						}, classInfo.getClassReaderFlags());
						result = new JvmClassInfoBuilder(writer.toByteArray()).build();
					} else {
						// No special handling needed, just update the version value.
						byte[] original = classInfo.getBytecode();
						byte[] copy = Arrays.copyOf(original, original.length);
						copy[7] = (byte) classTarget;
						result = classInfo.toJvmClassBuilder().withBytecode(copy).withVersion(classTarget).build();
					}
					sink.accept(1 - (remaining.addAndGet(-1) / total));
					return result;
				} catch (Exception ex) {
					logger.error("Failed upgrading class '{}'", classInfo.getName(), ex);
					return null;
				} finally {
					sink.accept(1 - (remaining.addAndGet(-1) / total));
				}
			}, service).whenComplete((updated, error) -> {
				if (updated != null)
					targetBundle.put(updated);
			});
		}
		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException ex) {
			logger.info("Upgrade interrupted", ex);
		}
		return total;
	}

	private int downgrade(int target, @Nonnull DoubleConsumer sink) {
		int classTarget = JavaVersion.VERSION_OFFSET + target;

		List<JvmClassInfo> targets = targetBundle.stream()
				.filter(c -> c.getVersion() > classTarget)
				.collect(Collectors.toCollection(ArrayList::new));
		int total = targets.size();
		AtomicDouble remaining = new AtomicDouble(target);

		ExecutorService service = ThreadPoolFactory.newFixedThreadPool("class-downgrade");
		for (JvmClassInfo classInfo : targets) {
			CompletableFuture.supplyAsync(() -> {
				try {
					ClassNode node = new ClassNode();
					ClassReader reader = classInfo.getClassReader();
					reader.accept(node, 0);
					JavaDowngrader.downgrade(node, target);
					ClassWriter writer = new ClassWriter(reader, 0);
					node.accept(writer);
					return new JvmClassInfoBuilder(writer.toByteArray()).build();
				} catch (Exception ex) {
					logger.error("Failed downgrading class '{}'", classInfo.getName(), ex);
					return null;
				} finally {
					sink.accept(1 - (remaining.addAndGet(-1) / total));
				}
			}, service).whenComplete((updated, error) -> {
				if (updated != null)
					targetBundle.put(updated);
			});
		}
		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException ex) {
			logger.info("Downgrade interrupted", ex);
		}
		return total;
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
		layout.add(applyButton, 0, 2, 2, 1);
		layout.add(progressBar, 0, 3, 2, 1);
		layout.setPadding(new Insets(10));
		layout.setAlignment(Pos.TOP_CENTER);
		return layout;
	}

	/**
	 * @param targetBundle
	 * 		Bundle to target for version changing.
	 */
	public void setTargetBundle(@Nonnull JvmClassBundle targetBundle) {
		this.targetBundle = targetBundle;
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
}
