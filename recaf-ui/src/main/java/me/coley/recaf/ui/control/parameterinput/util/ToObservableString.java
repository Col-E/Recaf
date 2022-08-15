package me.coley.recaf.ui.control.parameterinput.util;

import javafx.beans.value.ObservableStringValue;

import java.util.function.Function;

public interface ToObservableString<A> extends Function<A, ObservableStringValue> {}
