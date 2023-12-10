package me.coley.recaf.ui.jfxbuilder.util.function;

import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.component.Component;

import java.util.function.Function;

public interface ToComponent<A,OutNode extends Node, Ct extends Component<OutNode, Ct>> extends Function<A, Ct> {}
