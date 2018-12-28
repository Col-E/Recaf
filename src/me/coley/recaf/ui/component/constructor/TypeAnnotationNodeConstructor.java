package me.coley.recaf.ui.component.constructor;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.TypeAnnotationNode;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Misc;

public class TypeAnnotationNodeConstructor extends HBox implements Constructor<TypeAnnotationNode> {
	TextField annoDesc = new TextField();
	TextField annoType = new TextField();
	ComboBox<RefType> comboRef = new ComboBox<>(JavaFX.observableList(RefType.values()));

	public TypeAnnotationNodeConstructor() {
		comboRef.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.ref.tooltip")));
		annoDesc.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.desc.tooltip")));
		annoType.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.type.tooltip")));
		// annoDesc.setOnAction((e) -> add(annoDesc, annoType, comboRef, view));
		// annoType.setOnAction((e) -> add(annoDesc, annoType, comboRef, view));
		getChildren().addAll(comboRef, annoDesc, annoType);
	}

	@Override
	public TypeAnnotationNode get() {
		if (comboRef.getValue() == null) {
			Logging.error(Lang.get("misc.invalidtype.typeref"), true);
			return null;
		}
		String desc = annoDesc.textProperty().get();
		String type = annoType.textProperty().get();
		if (desc == null || desc.isEmpty() || !TypeUtil.isStandard(desc)) {
			Logging.error(Lang.get("misc.invalidtype.standard"), true);
			return null;
		}
		try {
			if (type == null || type.isEmpty()) {
				Logging.error(Lang.get("misc.invalidtype.typepath"), true);
				return null;
			}
			TypePath typePath = TypePath.fromString(type);
			return new TypeAnnotationNode(comboRef.getValue().value, typePath, desc);
		} catch (IllegalArgumentException e) {
			Logging.error(Lang.get("misc.invalidtype.typepath"), true);
		} catch (Exception e) {
			Logging.error(e, true);
		}
		return null;
	}

	@Override
	public void reset() {
		annoDesc.textProperty().setValue("");
		annoType.textProperty().setValue("");
	}

	private static final Map<Integer, RefType> lookup = new HashMap<>();

	public enum RefType {
		//@formatter:off
		CLASS_TYPE_PARAMETER(TypeReference.CLASS_TYPE_PARAMETER),
		METHOD_TYPE_PARAMETER(TypeReference.METHOD_TYPE_PARAMETER),
		CLASS_EXTENDS(TypeReference.CLASS_EXTENDS),
		CLASS_TYPE_PARAMETER_BOUND(TypeReference.CLASS_TYPE_PARAMETER_BOUND),
		METHOD_TYPE_PARAMETER_BOUND(TypeReference.METHOD_TYPE_PARAMETER_BOUND),
		FIELD(TypeReference.FIELD),
		METHOD_RETURN(TypeReference.METHOD_RETURN),
		METHOD_RECEIVER(TypeReference.METHOD_RECEIVER),
		METHOD_FORMAL_PARAMETER(TypeReference.METHOD_FORMAL_PARAMETER),
		THROWS(TypeReference.THROWS),
		LOCAL_VARIABLE(TypeReference.LOCAL_VARIABLE),
		RESOURCE_VARIABLE(TypeReference.RESOURCE_VARIABLE),
		EXCEPTION_PARAMETER(TypeReference.EXCEPTION_PARAMETER),
		INSTANCEOF(TypeReference.INSTANCEOF),
		NEW(TypeReference.NEW),
		CONSTRUCTOR_REFERENCE(TypeReference.CONSTRUCTOR_REFERENCE),
		METHOD_REFERENCE(TypeReference.METHOD_REFERENCE),
		CAST(TypeReference.CAST),
		CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT(TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT),
		METHOD_INVOCATION_TYPE_ARGUMENT(TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT),
		CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT(TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT),
		METHOD_REFERENCE_TYPE_ARGUMENT(TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT),
		UNKNOWN(-1);
		//@formatter:on

		private final int value;

		RefType(int value) {
			this.value = value;
			lookup.put(value, this);
		}

		@Override
		public String toString() {
			return Lang.get(Misc.getTranslationKey("ui.bean.typeannotation.reftype", this));
		}

		public static RefType fromSort(int sort) {
			return lookup.getOrDefault(sort, UNKNOWN);
		}
	}
}