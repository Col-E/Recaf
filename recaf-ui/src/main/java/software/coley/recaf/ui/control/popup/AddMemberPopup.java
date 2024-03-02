package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import me.darknet.assembler.util.DescriptorUtil;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.*;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;

import java.awt.*;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.function.Consumer;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;

public class AddMemberPopup extends RecafStage {

    private final TextField nameInput = new TextField();
    private final TextField descInput = new TextField();

    // modifiers
    private final CheckBox publicCheck = new CheckBox("Public");
    private final CheckBox privateCheck = new CheckBox("Private");
    private final CheckBox protectedCheck = new CheckBox("Protected");
    private final CheckBox staticCheck = new CheckBox("Static");
    private final CheckBox finalCheck = new CheckBox("Final");
    private final Label output = new Label();

    private boolean isMethod;

    private final BooleanProperty isIllegalDescriptor = new SimpleBooleanProperty(false);
    private final BooleanProperty isIllegalName = new SimpleBooleanProperty(false);
    private final BooleanProperty nameConflict = new SimpleBooleanProperty(false);

    public AddMemberPopup(@Nonnull Consumer<ClassMember> memberConsumer) {
        Button add = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(memberConsumer));
        Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
        add.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
        cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

        add.disableProperty().bind(isIllegalName.or(isIllegalDescriptor).or(nameConflict));

        BooleanBinding shouldShowOutput = isIllegalName.or(isIllegalDescriptor).or(nameConflict);
        output.visibleProperty().bind(shouldShowOutput);
        isIllegalName.addListener((obs, ov, nv) -> { if(nv) output.setText("Illegal name"); });
        isIllegalDescriptor.addListener((obs, ov, nv) -> { if(nv) output.setText("Illegal descriptor"); });
        nameConflict.addListener((obs, ov, nv) -> { if(nv) output.setText("Name conflict"); });

        this.isIllegalName.bind(nameInput.textProperty().map(text -> {
            if (text.isBlank()) return true;

            // cannot have ';', '[', '.' or '/' in name
            return text.contains(";") || text.contains("[")
                    || text.contains(".") || text.contains("/");
        }));

        this.nameInput.setPromptText("Name");
        this.descInput.setPromptText("Descriptor");

        // Layout
        HBox controlButtons = new HBox(add, cancel);
        controlButtons.setSpacing(10);
        controlButtons.setPadding(new Insets(10, 0, 10, 0));
        controlButtons.setAlignment(Pos.CENTER_RIGHT);

        // add a spacer
        HBox spacer = new HBox();
        spacer.setPadding(new Insets(10, 0, 10, 0));
        spacer.setAlignment(Pos.CENTER_LEFT);

        HBox modifiers = new HBox(publicCheck, privateCheck, protectedCheck, staticCheck, finalCheck);
        modifiers.setSpacing(10);
        modifiers.setPadding(new Insets(10, 0, 10, 0));
        modifiers.setAlignment(Pos.CENTER_LEFT);

        HBox inputs = new HBox(nameInput, descInput);
        inputs.setSpacing(10);
        inputs.setAlignment(Pos.CENTER_LEFT);
        // make it so the inputs change size with the window
        HBox.setHgrow(nameInput, Priority.ALWAYS);
        HBox.setHgrow(descInput, Priority.ALWAYS);

        VBox layout = new VBox(inputs, spacer, modifiers, controlButtons, new Group(output));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(10));

        setScene(new RecafScene(layout, 300, 150));
    }

    private void accept(Consumer<ClassMember> memberConsumer) {
        if (nameConflict.get()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        int access = 0;
        if (publicCheck.isSelected()) access |= Modifier.PUBLIC;
        if (privateCheck.isSelected()) access |= Modifier.PRIVATE;
        if (protectedCheck.isSelected()) access |= Modifier.PROTECTED;
        if (staticCheck.isSelected()) access |= Modifier.STATIC;
        if (finalCheck.isSelected()) access |= Modifier.FINAL;

        ClassMember member;
        if (isMethod) {
            member = new BasicMethodMember(nameInput.textProperty().get(),
                    descInput.textProperty().get(), null, access,
                    Collections.emptyList(), Collections.emptyList());
        } else {
            member = new BasicFieldMember(nameInput.textProperty().get(),
                    descInput.textProperty().get(), null, access, null);
        }

        memberConsumer.accept(member);

        hide();
    }

    public AddMemberPopup forMethod(ClassInfo info) {
        this.isMethod = true;


        this.nameConflict.bind(nameInput.textProperty().map(name -> {
            for (MethodMember method : info.getMethods()) {
                if (method.getName().equals(name) && method.getDescriptor().equals(descInput.textProperty().get())) {
                    return true;
                }
            }
            return false;
        }));

        this.isIllegalDescriptor.bind(descInput.textProperty().map(
                text -> text.isBlank() || !DescriptorUtil.isValidMethodDescriptor(text)));

        return this;
    }

    public AddMemberPopup forField(ClassInfo info) {
        this.isMethod = false;

        this.nameConflict.bind(nameInput.textProperty().map(name -> {
            for (FieldMember field : info.getFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
            return false;
        }));

        this.isIllegalDescriptor.bind(descInput.textProperty().map(
                text -> text.isBlank() || !DescriptorUtil.isValidFieldDescriptor(text)));

        return this;
    }

}
