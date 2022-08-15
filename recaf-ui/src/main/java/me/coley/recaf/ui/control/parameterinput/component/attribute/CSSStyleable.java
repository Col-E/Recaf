package me.coley.recaf.ui.control.parameterinput.component.attribute;

public interface CSSStyleable<Self extends CSSStyleable<Self>> {
	Self styleSheet(String stylePath);
}
