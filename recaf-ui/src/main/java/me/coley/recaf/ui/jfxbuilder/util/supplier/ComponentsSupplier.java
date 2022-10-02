package me.coley.recaf.ui.jfxbuilder.util.supplier;

import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.component.Component;

import java.util.function.Supplier;

public interface ComponentsSupplier
	<OutNode extends Node, Ct extends Component<OutNode, Ct>>
	extends Supplier<Ct[]> {}
