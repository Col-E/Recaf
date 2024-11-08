package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.Objects;
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
		Label title = new BoundLabel(Lang.getBinding("service.analysis.entry-points"));
		title.getStyleClass().addAll(Styles.TITLE_4);
		consumer.appendSummary(title);

		// Visit JVM classes
		int[] found = {0};
		resource.jvmClassBundleStream().forEach(bundle -> {
			bundle.forEach(cls -> {
				List<MethodMember> entryMethods = cls.getMethods().stream()
						.filter(this::isJvmEntry)
						.toList();
				if (!entryMethods.isEmpty()) {
					Supplier<JvmClassInfo> classLookup = () -> Objects.requireNonNullElse(bundle.get(cls.getName()), cls);

					// Add entry for class
					String classDisplay = textService.getJvmClassInfoTextProvider(workspace, resource, bundle, cls).makeText();
					Node classIcon = iconService.getJvmClassInfoIconProvider(workspace, resource, bundle, cls).makeIcon();
					Label classLabel = new Label(classDisplay, classIcon);
					classLabel.setCursor(Cursor.HAND);
					classLabel.setOnMouseEntered(e -> classLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
					classLabel.setOnMouseExited(e -> classLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
					classLabel.setOnMouseClicked(e -> actions.gotoDeclaration(workspace, resource, bundle, classLookup.get()));
					consumer.appendSummary(classLabel);

					// Add entries for methods
					for (MethodMember method : entryMethods) {
						String methodDisplay = textService.getMethodMemberTextProvider(workspace, resource, bundle, cls, method).makeText();
						Node methodIcon = iconService.getClassMemberIconProvider(workspace, resource, bundle, cls, method).makeIcon();
						Label methodLabel = new Label(methodDisplay);
						methodLabel.setCursor(Cursor.HAND);
						methodLabel.setGraphic(methodIcon);
						methodLabel.setPadding(new Insets(2, 2, 2, 15));
						methodLabel.setOnMouseEntered(e -> methodLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
						methodLabel.setOnMouseExited(e -> methodLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
						methodLabel.setOnMouseClicked(e -> {
							actions.gotoDeclaration(workspace, resource, bundle, classLookup.get())
									.requestFocus(method);
						});
						consumer.appendSummary(methodLabel);
						found[0]++;
					}
				}
			});
		});

		if (found[0] == 0) {
			consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none")));
		}

		return true;
	}

	private boolean isJvmEntry(MethodMember method) {
		return method.hasModifierMask(PUBLIC | STATIC) &&
				method.getName().equals("main") &&
				method.getDescriptor().equals("([Ljava/lang/String;)V");
	}
}
