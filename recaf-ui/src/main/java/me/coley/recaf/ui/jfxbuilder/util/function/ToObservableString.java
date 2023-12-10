package me.coley.recaf.ui.jfxbuilder.util.function;

import javafx.beans.value.ObservableStringValue;

import java.util.function.Function;

public interface ToObservableString<A> extends Function<A, ObservableStringValue> {}
