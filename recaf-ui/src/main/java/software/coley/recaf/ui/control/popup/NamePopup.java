package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Generic name input popup.
 *
 * @author Matt Coley
 */
public class NamePopup extends RecafStage {
	private final BooleanProperty nameConflict = new SimpleBooleanProperty(false);
	private final BooleanProperty isIllegalValue = new SimpleBooleanProperty(false);
	private final Label output = new Label();
	private final TextField nameInput = new TextField();
	private final Button accept;
	private String initialText;

	/**
	 * @param nameConsumer
	 * 		Consumer to handle accepted inputs.
	 */
	public NamePopup(@Nonnull Consumer<String> nameConsumer) {
		// Handle user accepting input
		isIllegalValue.bind(nameInput.textProperty().map(text -> {
			// Cannot be blank/empty
			if (text.isBlank()) return true;

			// Cannot end with a slash
			char last = text.charAt(text.length() - 1);
			return last == '/';
		}));
		nameInput.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				accept(nameConsumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		accept = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(nameConsumer));
		Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		accept.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
		cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

		// Layout
		HBox buttons = new HBox(accept, cancel);
		buttons.setSpacing(10);
		buttons.setPadding(new Insets(10, 0, 10, 0));
		buttons.setAlignment(Pos.CENTER_RIGHT);
		VBox layout = new VBox(nameInput, buttons, new Group(output));
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setScene(new RecafScene(layout, 400, 150));
	}

	/**
	 * @param nameConsumer
	 * 		Action to run on accept.
	 */
	private void accept(@Nonnull Consumer<String> nameConsumer) {
		// Do nothing if conflict detected
		if (nameConflict.get() || isIllegalValue.get()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		String text = EscapeUtil.unescapeStandard(nameInput.getText());
		CompletableFuture.runAsync(() -> nameConsumer.accept(text));
		hide();
	}

	/**
	 * @param bundle
	 * 		Target bundle the package will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forPackageCopy(@Nonnull Bundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-package"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-package-error"));

		// Bind conflict property
		nameConflict.bind(nameInput.textProperty().map(dirName -> bundleHasDirectory(bundle, dirName)));
		BooleanBinding notContainingPackageOrEqualToInitial = nameConflict.or(nameInput.textProperty().isEqualTo(initialText));
		accept.disableProperty().bind(isIllegalValue.or(notContainingPackageOrEqualToInitial));
		output.visibleProperty().bind(notContainingPackageOrEqualToInitial);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the package will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forPackageRename(@Nonnull Bundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-package"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-package-error"));

		// Bind conflict property
		nameConflict.bind(nameInput.textProperty().map(dirName -> bundleHasDirectory(bundle, dirName)));
		BooleanBinding notContainingPackageOrEqualToInitial = nameConflict.or(nameInput.textProperty().isEqualTo(initialText));
		accept.disableProperty().bind(isIllegalValue.or(notContainingPackageOrEqualToInitial));
		output.visibleProperty().bind(notContainingPackageOrEqualToInitial);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the class will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forClassCopy(@Nonnull ClassBundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-class"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-class-error"));

		// Bind conflict property
		nameConflict.bind(nameInput.textProperty().map(bundle::containsKey));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the class will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forClassRename(@Nonnull ClassBundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-class"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-class-error"));

		// Bind conflict property
		nameConflict.bind(nameInput.textProperty().map(bundle::containsKey));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the file will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forFileRename(@Nonnull FileBundle bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-file"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-file-error"));

		// Bind conflict property
		nameConflict.bind(nameInput.textProperty().map(bundle::containsKey));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param declaringClass
	 * 		Class the field is declared in.
	 * 		Used to check for name overlap.
	 * @param member
	 * 		Current field info.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forFieldRename(@Nonnull ClassInfo declaringClass, @Nonnull ClassMember member) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-field"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-field-error"));

		// Bind conflict property
		String descriptor = member.getDescriptor();
		nameConflict.bind(nameInput.textProperty().map(name -> declaringClass.getDeclaredField(name, descriptor) != null));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param declaringClass
	 * 		Class the field is declared in.
	 * 		Used to check for name overlap.
	 * @param member
	 * 		Current field info.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forFieldCopy(@Nonnull ClassInfo declaringClass, @Nonnull ClassMember member) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-field"));
		output.textProperty().bind(Lang.getBinding("dialog.header.copy-field-error"));

		// Bind conflict property
		String descriptor = member.getDescriptor();
		nameConflict.bind(nameInput.textProperty().map(name -> declaringClass.getDeclaredField(name, descriptor) != null));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param declaringClass
	 * 		Class the method is declared in.
	 * 		Used to check for name overlap.
	 * @param member
	 * 		Current method info.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forMethodRename(@Nonnull ClassInfo declaringClass, @Nonnull ClassMember member) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-method"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-method-error"));

		// Bind conflict property
		String descriptor = member.getDescriptor();
		nameConflict.bind(nameInput.textProperty().map(name -> declaringClass.getDeclaredMethod(name, descriptor) != null));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param declaringClass
	 * 		Class the method is declared in.
	 * 		Used to check for name overlap.
	 * @param member
	 * 		Current method info.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forMethodCopy(@Nonnull ClassInfo declaringClass, @Nonnull ClassMember member) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-method"));
		output.textProperty().bind(Lang.getBinding("dialog.header.copy-method-error"));

		// Bind conflict property
		String descriptor = member.getDescriptor();
		nameConflict.bind(nameInput.textProperty().map(name -> declaringClass.getDeclaredMethod(name, descriptor) != null));
		accept.disableProperty().bind(isIllegalValue.or(nameConflict));
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the directory will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forDirectoryRename(@Nonnull Bundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-directory"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-directory-warning"));

		// Bind conflict property, but only as a warning since merging is allowed.
		nameConflict.bind(nameInput.textProperty().map(dirName -> bundleHasDirectory(bundle, dirName)));
		accept.disableProperty().bind(isIllegalValue);
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the directory will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forDirectoryCopy(@Nonnull Bundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-directory"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-directory-warning"));

		// Bind conflict property, but only as a warning since merging is allowed.
		nameConflict.bind(nameInput.textProperty().map(dirName -> bundleHasDirectory(bundle, dirName)));
		accept.disableProperty().bind(isIllegalValue);
		output.visibleProperty().bind(nameConflict);
		return this;
	}

	/**
	 * @param binding
	 * 		Title binding.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup withTitle(@Nonnull StringBinding binding) {
		titleProperty().bind(binding);
		return this;
	}

	@Nonnull
	public NamePopup withInitialPathName(@Nonnull String name) {
		initialText = name;

		// Handle escaping names with newlines and such
		nameInput.setText(EscapeUtil.escapeStandard(name));

		// Need to get focus before selecting range.
		// When the input gets focus, it resets the selection.
		// But if we focus it, then set the selection we're good.
		nameInput.requestFocus();

		// Select the last portion of the name.
		nameInput.selectRange(name.lastIndexOf('/') + 1, name.length());
		return this;
	}

	@Nonnull
	public NamePopup withInitialName(@Nonnull String name) {
		initialText = name;

		// Handle escaping names with newlines and such
		nameInput.setText(EscapeUtil.escapeStandard(name));

		// Need to get focus before selecting range.
		// When the input gets focus, it resets the selection.
		// But if we focus it, then set the selection we're good.
		nameInput.requestFocus();

		// Select the name.
		nameInput.selectAll();
		return this;
	}

	private static boolean bundleHasDirectory(@Nonnull Bundle<?> bundle, String dirName) {
		String dirNameWithSlash = dirName + "/";
		return bundle.keySet().stream().anyMatch(file -> file.startsWith(dirNameWithSlash));
	}
}
