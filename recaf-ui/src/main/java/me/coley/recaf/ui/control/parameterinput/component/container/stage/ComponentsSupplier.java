package me.coley.recaf.ui.control.parameterinput.component.container.stage;

import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.Component;

import java.util.function.Supplier;

public interface ComponentsSupplier
	<OutNode extends Node, Ct extends Component<OutNode, Ct>>
	extends Supplier<Ct[]> {}
