package software.coley.recaf.ui.pane.editing.assembler;

import dev.xdark.blw.type.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.input.MouseButton;
import me.darknet.assembler.compile.analysis.AnalysisUtils;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextMenuProvider;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;

import java.util.UUID;

/**
 * Cell for rendering {@link ClassType}.
 *
 * @param <S>
 * 		Table-view generic type.
 *
 * @author Matt Coley
 */
public class TypeTableCell<S> extends TableCell<S, ClassType> {
	/** Special value used to represent null types. Randomized to prevent abuse. */
	static final ClassType NULL_TYPE = Types.instanceTypeFromInternalName(UUID.randomUUID().toString());
	private final CellConfigurationService cellConfigurationService;
	private final TextFormatConfig formatConfig;
	private final Workspace workspace;

	public TypeTableCell(@Nonnull CellConfigurationService cellConfigurationService,
						 @Nonnull TextFormatConfig formatConfig,
						 @Nonnull Workspace workspace) {
		this.cellConfigurationService = cellConfigurationService;
		this.formatConfig = formatConfig;
		this.workspace = workspace;
	}

	@Override
	protected void updateItem(ClassType type, boolean empty) {
		super.updateItem(type, empty);
		if (empty || type == null) {
			setText(null);
			setGraphic(null);
		} else {
			configureType(type);
		}
	}

	private void configureType(@Nonnull ClassType type) {
		CellData data = getTypeData(type);
		setGraphic(data.graphic);
		setText(data.text);
		setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				// Lazily populate context menus when secondary click is prompted.
				if (getContextMenu() == null) setContextMenu(data.contextSupplier.makeMenu());
			}
		});
		setOpacity(data.disabled ? 0.35 : 1);
	}

	@Nonnull
	private CellData getTypeData(@Nullable ClassType type) {
		Node graphic;
		String text;
		ContextMenuProvider contextSupplier = null;
		boolean disabled = false;
		if (type == NULL_TYPE || type == null) {
			graphic = Icons.getIconView(Icons.UNINITIALIZED);
			text = "null";
		} else if (type == Types.VOID || type == Types.BOX_VOID) {
			disabled = true;
			graphic = Icons.getIconView(Icons.UNINITIALIZED);
			text = "void";
		} else if (type instanceof PrimitiveType primitiveType) {
			disabled = primitiveType.kind() == PrimitiveKind.T_VOID;
			graphic = Icons.getIconView(Icons.PRIMITIVE);
			text = primitiveType.name();
		} else if (type instanceof InstanceType instanceType) {
			String typeName = instanceType.internalName();
			ClassPathNode classPath = workspace.findClass(typeName);
			if (classPath != null) {
				graphic = cellConfigurationService.graphicOf(classPath);
				text = cellConfigurationService.textOf(classPath);
				contextSupplier = () -> cellConfigurationService.contextMenuOf(ContextualAssemblerComponent.CONTEXT_SOURCE, classPath);
			} else {
				graphic = Icons.getIconView(Icons.CLASS);
				text = formatConfig.filter(typeName);
			}
		} else if (type instanceof ArrayType arrayType) {
			CellData componentModel = getTypeData(arrayType.componentType());
			graphic = Icons.getIconView(Icons.ARRAY);
			text = componentModel.text + "[]".repeat(arrayType.dimensions());
			contextSupplier = componentModel.contextSupplier;
		} else {
			text = null;
			graphic = null;
		}
		return new CellData(text, graphic, contextSupplier, disabled);
	}

	private record CellData(String text, Node graphic, ContextMenuProvider contextSupplier, boolean disabled) {}
}
