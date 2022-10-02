package me.coley.recaf.ui.jfxbuilder.component.attribute;

public interface CSSStyleable<Self extends CSSStyleable<Self>> {
	Self styleSheet(String stylePath);
}
