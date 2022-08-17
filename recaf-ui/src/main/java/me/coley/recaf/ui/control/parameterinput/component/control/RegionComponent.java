package me.coley.recaf.ui.control.parameterinput.component.control;

public interface RegionComponent<Self extends RegionComponent<Self>> {

	Self minWidth(double minWidth);
	Self minHeight(double minHeight);
	Self maxWidth(double maxWidth);
	Self maxHeight(double maxHeight);
	Self prefWidth(double prefWidth);
	Self prefHeight(double prefHeight);
	Self maxSize(double maxWidth, double maxHeight);
	Self minSize(double minWidth, double minHeight);
	Self prefSize(double prefWidth, double prefHeight);
	Self snapToPixel(boolean snapToPixel);
	Self padding(double top, double right, double bottom, double left);
}
