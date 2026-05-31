package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.input.MouseButton;
import org.objectweb.asm.Type;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextMenuProvider;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;

import java.util.UUID;

/**
 * Cell for rendering {@link Type}.
 *
 * @param <S>
 * 		Table-view generic type.
 *
 * @author Matt Coley
 */
public class TypeTableCell<S> extends TableCell<S, Type> {
	/** Special value used to represent null types. Randomized to prevent abuse. */
	static final Type NULL_TYPE = Type.getObjectType(UUID.randomUUID().toString());
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
	protected void updateItem(Type type, boolean empty) {
		super.updateItem(type, empty);
		if (empty || type == null) {
			setText(null);
			setGraphic(null);
		} else {
			configureType(type);
		}
	}

	private void configureType(@Nonnull Type type) {
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
	private CellData getTypeData(@Nullable Type type) {
		Node graphic;
		String text;
		ContextMenuProvider contextSupplier = null;
		boolean disabled = false;
		if (type == NULL_TYPE || type == null) {
			graphic = Icons.getIconView(Icons.UNINITIALIZED);
			text = "null";
		} else if (type.getSort() == Type.VOID || Types.BOX_VOID.equals(type)) {
			disabled = true;
			graphic = Icons.getIconView(Icons.UNINITIALIZED);
			text = "void";
		} else if (Types.isPrimitive(type)) {
			graphic = Icons.getIconView(Icons.PRIMITIVE);
			text = type.getClassName();
		} else if (type.getSort() == Type.OBJECT) {
			String typeName = type.getInternalName();
			ClassPathNode classPath = workspace.findClass(typeName);
			if (classPath != null) {
				graphic = cellConfigurationService.graphicOf(classPath);
				text = cellConfigurationService.textOf(classPath);
				contextSupplier = () -> cellConfigurationService.contextMenuOf(ContextualAssemblerComponent.CONTEXT_SOURCE, classPath);
			} else {
				graphic = Icons.getIconView(Icons.CLASS);
				text = formatConfig.filter(typeName);
			}
		} else if (type.getSort() == Type.ARRAY) {
			CellData elementModel = getTypeData(type.getElementType());
			graphic = Icons.getIconView(Icons.ARRAY);
			text = elementModel.text + "[]".repeat(type.getDimensions());
			contextSupplier = elementModel.contextSupplier;
		} else {
			text = null;
			graphic = null;
		}
		return new CellData(text, graphic, contextSupplier, disabled);
	}

	private record CellData(String text, Node graphic, ContextMenuProvider contextSupplier, boolean disabled) {}
}
