package me.coley.recaf.ui.control.parameterinput.util;

import javafx.scene.Node;

import java.util.function.Function;

public interface ToNode<A> extends Function<A, Node> {}
