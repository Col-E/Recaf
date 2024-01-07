package software.coley.recaf.ui.pane.editing.assembler;

import dev.xdark.blw.type.*;
import jakarta.annotation.Nonnull;
import javafx.scene.control.TableCell;
import me.darknet.assembler.compile.analysis.AnalysisUtils;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Cell for rendering {@link ClassType}.
 *
 * @param <S>
 * 		Table-view generic type.
 *
 * @author Matt Coley
 */
public class TypeTableCell<S> extends TableCell<S, ClassType> {
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
		boolean disabled = false;
		if (type == AnalysisUtils.NULL) {
			setGraphic(Icons.getIconView(Icons.UNINITIALIZED));
			setText("null");
		} else if (type == Types.VOID || type == Types.BOX_VOID) {
			disabled = true;
			setGraphic(Icons.getIconView(Icons.UNINITIALIZED));
			setText("void");
		} else if (type instanceof PrimitiveType primitiveType) {
			disabled = primitiveType.kind() == PrimitiveKind.T_VOID;
			setGraphic(Icons.getIconView(Icons.PRIMITIVE));
			setText(primitiveType.name());
		} else if (type instanceof InstanceType instanceType) {
			String typeName = instanceType.internalName();
			ClassPathNode classPath = workspace.findClass(typeName);
			if (classPath != null) {
				cellConfigurationService.configure(this, classPath, ContextualAssemblerComponent.CONTEXT_SOURCE);
			} else {
				setGraphic(Icons.getIconView(Icons.CLASS));
				setText(formatConfig.filter(typeName));
			}
		} else if (type instanceof ArrayType arrayType) {
			ClassType componentType = arrayType.componentType();
			configureType(componentType);
		}
		setOpacity(disabled ? 0.35 : 1);
	}
}
