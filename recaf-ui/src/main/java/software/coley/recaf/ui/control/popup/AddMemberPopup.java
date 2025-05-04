package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import me.darknet.assembler.util.DescriptorUtil;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.*;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.IconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.function.Consumer;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Generic popup for adding a new field/method to a class.
 *
 * @author Justus Garbe
 */
public class AddMemberPopup extends RecafStage {
	private final TextField nameInput = new TextField();
	private final TextField descInput = new TextField();
	private final CheckBox publicCheck = new CheckBox("Public");
	private final CheckBox privateCheck = new CheckBox("Private");
	private final CheckBox protectedCheck = new CheckBox("Protected");
	private final CheckBox staticCheck = new CheckBox("Static");
	private final CheckBox finalCheck = new CheckBox("Final");
	private final Label output = new Label();
	private final BooleanProperty isIllegalDescriptor = new SimpleBooleanProperty(false);
	private final BooleanProperty isIllegalName = new SimpleBooleanProperty(false);
	private final BooleanProperty nameConflict = new SimpleBooleanProperty(false);
	private boolean isMethod;

	/**
	 * New generic member popup.
	 *
	 * @param memberConsumer
	 * 		Consumer to add the member outline to a target class.
	 *
	 * @see #forField(ClassInfo)
	 * @see #forMethod(ClassInfo)
	 */
	public AddMemberPopup(@Nonnull Consumer<ClassMember> memberConsumer) {
		Button add = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(memberConsumer));
		Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		add.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
		cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

		add.disableProperty().bind(isIllegalName.or(isIllegalDescriptor).or(nameConflict));

		publicCheck.setGraphic(Icons.getIconView(Icons.ACCESS_PUBLIC));
		privateCheck.setGraphic(Icons.getIconView(Icons.ACCESS_PRIVATE));
		protectedCheck.setGraphic(Icons.getIconView(Icons.ACCESS_PROTECTED));
		staticCheck.setGraphic(Icons.getIconView(Icons.ACCESS_STATIC));
		finalCheck.setGraphic(Icons.getIconView(Icons.ACCESS_FINAL));

		BooleanBinding shouldShowOutput = isIllegalName.or(isIllegalDescriptor).or(nameConflict);
		output.visibleProperty().bind(shouldShowOutput);
		isIllegalName.addListener((obs, ov, nv) -> {
			if (nv) output.setText(Lang.get("dialog.warn.illegal-name"));
		});
		isIllegalDescriptor.addListener((obs, ov, nv) -> {
			if (nv) output.setText(Lang.get("dialog.warn.illegal-desc"));
		});
		nameConflict.addListener((obs, ov, nv) -> {
			if (nv)
				output.setText(Lang.get(isMethod ? "dialog.warn.method-conflict" : "dialog.warn.field-conflict"));
		});

		isIllegalName.bind(nameInput.textProperty().map(text -> {
			if (text.isBlank()) return true;

			// Cannot have ';', '[', '.' or '/' in name
			return text.indexOf(';') >= 0 || text.indexOf('[') >= 0 ||
					text.indexOf('.') >= 0 || text.indexOf('/') >= 0;
		}));

		nameInput.setPromptText(Lang.get("dialog.input.name"));
		descInput.setPromptText(Lang.get("dialog.input.desc"));

		nameInput.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER && !add.isDisable()) {
			 	accept(memberConsumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		descInput.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER && !add.isDisable()) {
				 accept(memberConsumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});

		// Layout
		HBox controlButtons = new HBox(add, cancel);
		controlButtons.setSpacing(10);
		controlButtons.setPadding(new Insets(10, 0, 10, 0));
		controlButtons.setAlignment(Pos.CENTER_RIGHT);

		// Add a spacer
		HBox spacer = new HBox();
		spacer.setPadding(new Insets(10, 0, 10, 0));
		spacer.setAlignment(Pos.CENTER_LEFT);

		// Only one accessibility modifier can be selected at a time
		VBox accessibilityModifiers = new VBox(publicCheck, privateCheck, protectedCheck);
		accessibilityModifiers.setPadding(new Insets(0, 15, 0, 0));
		ChangeListener<Boolean> singleAccessibilityEnforcement = (ov, old, cur) -> {
			if (cur)
				accessibilityModifiers.getChildren().forEach(child -> {
					if (child instanceof CheckBox check && check.selectedProperty() != ov)
						check.setSelected(false);
				});
		};
		publicCheck.selectedProperty().addListener(singleAccessibilityEnforcement);
		privateCheck.selectedProperty().addListener(singleAccessibilityEnforcement);
		protectedCheck.selectedProperty().addListener(singleAccessibilityEnforcement);
		HBox modifiers = new HBox(accessibilityModifiers, staticCheck, finalCheck);
		modifiers.setSpacing(10);
		modifiers.setPadding(new Insets(10, 0, 10, 0));
		modifiers.setAlignment(Pos.TOP_LEFT);

		HBox inputs = new HBox(nameInput, descInput);
		inputs.setSpacing(10);
		inputs.setAlignment(Pos.CENTER_LEFT);

		// Make it so the inputs change size with the window
		HBox.setHgrow(nameInput, Priority.ALWAYS);
		HBox.setHgrow(descInput, Priority.ALWAYS);

		VBox layout = new VBox(inputs, spacer, modifiers, controlButtons, new Group(output));
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setMinWidth(500);
		setMinHeight(230);
		setScene(new RecafScene(layout, 500, 230));
	}

	private void accept(@Nonnull Consumer<ClassMember> memberConsumer) {
		int access = 0;
		if (publicCheck.isSelected()) access |= Modifier.PUBLIC;
		if (privateCheck.isSelected()) access |= Modifier.PRIVATE;
		if (protectedCheck.isSelected()) access |= Modifier.PROTECTED;
		if (staticCheck.isSelected()) access |= Modifier.STATIC;
		if (finalCheck.isSelected()) access |= Modifier.FINAL;

		String name = nameInput.textProperty().get();
		String desc = descInput.textProperty().get();
		ClassMember member;
		if (isMethod) {
			member = new BasicMethodMember(name, desc, null, access, Collections.emptyList());
		} else {
			member = new BasicFieldMember(name, desc, null, access, null);
		}

		memberConsumer.accept(member);

		hide();
	}


	/**
	 * Configures the popup for adding a method.
	 *
	 * @param info
	 * 		Class to add the method to.
	 *
	 * @return Self.
	 */
	@Nonnull
	public AddMemberPopup forMethod(@Nonnull ClassInfo info) {
		isMethod = true;

		nameConflict.bind(nameInput.textProperty().map(name -> {
			String desc = descInput.textProperty().get();
			for (MethodMember method : info.getMethods()) {
				if (method.getName().equals(name) && method.getDescriptor().equals(desc)) {
					return true;
				}
			}
			return false;
		}));

		isIllegalDescriptor.bind(descInput.textProperty().map(text -> text.isBlank() ||
				!DescriptorUtil.isValidMethodDescriptor(text)));

		setTitle(Lang.get("dialog.title.add-method"));

		descInput.setText("()V");

		return this;
	}

	/**
	 * Configures the popup for adding a field.
	 *
	 * @param info
	 * 		Class to add the field to.
	 *
	 * @return Self.
	 */
	@Nonnull
	public AddMemberPopup forField(@Nonnull ClassInfo info) {
		isMethod = false;

		nameConflict.bind(nameInput.textProperty().map(name -> {
			for (FieldMember field : info.getFields()) {
				if (field.getName().equals(name)) {
					return true;
				}
			}
			return false;
		}));

		isIllegalDescriptor.bind(descInput.textProperty().map(text -> text.isBlank() ||
				!DescriptorUtil.isValidFieldDescriptor(text)));

		setTitle(Lang.get("dialog.title.add-field"));

		descInput.setText("Ljava/lang/Object;");

		return this;
	}
}
