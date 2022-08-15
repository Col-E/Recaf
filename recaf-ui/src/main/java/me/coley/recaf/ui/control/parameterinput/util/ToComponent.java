package me.coley.recaf.ui.control.parameterinput.util;

import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.Component;

import java.util.function.Function;

public interface ToComponent<A,OutNode extends Node, Ct extends Component<OutNode, Ct>> extends Function<A, Ct> {}
