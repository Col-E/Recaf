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
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * Summarizer that shows entry-points.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class EntryPointSummarizer implements ResourceSummarizer {
	private final TextProviderService textService;
	private final IconProviderService iconService;
	private final Actions actions;

	@Inject
	public EntryPointSummarizer(@Nonnull TextProviderService textService,
	                            @Nonnull IconProviderService iconService,
	                            @Nonnull Actions actions) {
		this.textService = textService;
		this.iconService = iconService;
		this.actions = actions;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.entry-points"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});

		// Visit JVM classes, Android manifest for entry points.
		int[] found = {0};
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		while (!resourceQueue.isEmpty()) {
			// Doing this queue because embedded resources need to properly know which resource they belong to.
			WorkspaceResource currentResource = resourceQueue.remove();
			currentResource.jvmAllClassBundleStream().forEach(bundle -> {
				bundle.forEach(cls -> {
					List<MethodMember> entryMethods = cls.getMethods().stream()
							.filter(this::isJvmEntry)
							.toList();
					if (!entryMethods.isEmpty()) {
						found[0]++;
						batch.add(() -> {
							Supplier<JvmClassInfo> classLookup = () -> Objects.requireNonNullElse(bundle.get(cls.getName()), cls);

							// Add entry for class
							String classDisplay = textService.getJvmClassInfoTextProvider(workspace, currentResource, bundle, cls).makeText();
							Node classIcon = iconService.getJvmClassInfoIconProvider(workspace, currentResource, bundle, cls).makeIcon();
							Label classLabel = new Label(classDisplay, classIcon);
							classLabel.setCursor(Cursor.HAND);
							classLabel.setOnMouseEntered(e -> classLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
							classLabel.setOnMouseExited(e -> classLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
							classLabel.setOnMouseClicked(e -> actions.gotoDeclaration(workspace, currentResource, bundle, classLookup.get()));
							consumer.appendSummary(classLabel);

							// Add entries for methods
							for (MethodMember method : entryMethods) {
								String methodDisplay = textService.getMethodMemberTextProvider(workspace, currentResource, bundle, cls, method).makeText();
								Node methodIcon = iconService.getClassMemberIconProvider(workspace, currentResource, bundle, cls, method).makeIcon();
								Label methodLabel = new Label(methodDisplay);
								methodLabel.setCursor(Cursor.HAND);
								methodLabel.setGraphic(methodIcon);
								methodLabel.setPadding(new Insets(2, 2, 2, 15));
								methodLabel.setOnMouseEntered(e -> methodLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
								methodLabel.setOnMouseExited(e -> methodLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
								methodLabel.setOnMouseClicked(e -> {
									actions.gotoDeclaration(workspace, currentResource, bundle, classLookup.get())
											.requestFocus(method);
								});
								consumer.appendSummary(methodLabel);
							}
						});
					}
				});
			});

			// Look for Android entry points in the manifest.
			FileInfo manifest = currentResource.getFileBundle().get("AndroidManifest.xml");
			if (manifest instanceof BinaryXmlFileInfo manifestXmlInfo) {
				BinaryResourceFile chunkModel = manifestXmlInfo.getChunkModel();

				// Extract string pool chunk to pull values from.
				StringPoolChunk stringChunk = manifestXmlInfo.getStringPoolChunk();
				if (stringChunk == null)
					continue;

				// Walk chunks to find main activity and other entry points.
				Queue<Chunk> chunkQueue = new ArrayDeque<>(chunkModel.getChunks());
				while (!chunkQueue.isEmpty()) {
					Chunk chunk = chunkQueue.remove();

					// Look for activities in the manifest, as those are entry points.
					if (chunk instanceof XmlStartElementChunk start && "activity".equals(start.getName())) {
						for (XmlAttribute attribute : start.getAttributes()) {
							String name = stringChunk.getString(attribute.nameIndex());
							if (!"name".equals(name))
								continue;
							String activityName = stringChunk.getString(attribute.rawValueIndex()).replace('.', '/');
							ClassPathNode activityPath = workspace.findAndroidClass(activityName);
							if (activityPath == null)
								continue;

							found[0]++;
							batch.add(() -> {
								AndroidClassBundle bundle = activityPath.getValueOfType(AndroidClassBundle.class);
								AndroidClassInfo cls = activityPath.getValue().asAndroidClass();
								Supplier<AndroidClassInfo> classLookup = () -> Objects.requireNonNullElse(bundle.get(activityName), cls);

								// Add entry for class
								String classDisplay = textService.getAndroidClassInfoTextProvider(workspace, currentResource, bundle, cls).makeText();
								Node classIcon = iconService.getAndroidClassInfoIconProvider(workspace, currentResource, bundle, cls).makeIcon();
								Label classLabel = new Label(classDisplay, classIcon);
								classLabel.setCursor(Cursor.HAND);
								classLabel.setOnMouseEntered(e -> classLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
								classLabel.setOnMouseExited(e -> classLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
								classLabel.setOnMouseClicked(e -> actions.gotoDeclaration(workspace, currentResource, bundle, classLookup.get()));
								consumer.appendSummary(classLabel);
							});
						}
					}

					// Add children
					if (chunk instanceof ChunkWithChunks chunkWithChunks)
						chunkQueue.addAll(chunkWithChunks.getChunks().values());
				}
			}

			// Queue up embedded resources.
			resourceQueue.addAll(currentResource.getEmbeddedResources().values());
		}

		if (found[0] == 0)
			batch.add(() -> consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none"))));

		batch.execute();

		return true;
	}

	private boolean isJvmEntry(MethodMember method) {
		return method.hasModifierMask(PUBLIC | STATIC) &&
				method.getName().equals("main") &&
				method.getDescriptor().equals("([Ljava/lang/String;)V");
	}
}
