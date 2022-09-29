package me.coley.recaf.ui.control;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.parse.ParseHitResult;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Navigation bar implementation. Helpful for easily navigating around a class.
 *
 * @author yapht
 */
public class NavigationBar extends HBox {
	private static final Logger logger = Logging.get(NavigationBar.class);
	private static final long EXPAND_ANIM_MS = 450;
	private boolean lastShownState;
	private CommonClassInfo lastClassInfo;

	/**
	 * Deny public construction.
	 */
	private NavigationBar() {
		getStyleClass().add("navbar");
		setAlignment(Pos.CENTER_LEFT);
		setMaxHeight(30);

		managedProperty().bind(visibleProperty());

		RecafDockingManager docking = RecafDockingManager.getInstance();
		docking.addTabSelectionListener((region, tab) -> {
			if (tab instanceof ClassTab) {
				ClassTab classTab = (ClassTab) tab;
				CommonClassInfo tabClassInfo = classTab.getClassRepresentation().getCurrentClassInfo();
				update(tabClassInfo, null);
			}
		});
		docking.addTabClosureListener(tab -> {
			if (tab instanceof ClassTab) {
				ClassTab classTab = (ClassTab) tab;
				CommonClassInfo tabClassInfo = classTab.getClassRepresentation().getCurrentClassInfo();
				if (lastClassInfo == tabClassInfo) {
					clear();
				}
			}
		});
	}

	/**
	 * Tries to update the NavBar with the method or field the caret is placed on.
	 *
	 * @param area
	 * 		Java area the to search in.
	 */
	public void tryUpdateNavbar(JavaArea area) {
		// Hide if config disables displaying the navigation bar
		if (!shouldShow()) {
			animateShown(false);
			return;
		}
		CommonClassInfo targetClass = area.getCurrentClassInfo();
		if (targetClass == null)
			return;
		NavigationBar navigationBar = NavigationBar.getInstance();
		Optional<ParseHitResult> infoAtPosition = area.declarationAtPosition(area.getCaretPosition());
		if (infoAtPosition.isPresent()) {
			ParseHitResult result = infoAtPosition.get();
			if (result.getInfo() instanceof MethodInfo) {
				MethodInfo method = (MethodInfo) result.getInfo();
				CommonClassInfo declarator = getDeclarator(targetClass, method);
				navigationBar.update(declarator, method);
			} else if (result.getInfo() instanceof FieldInfo) {
				FieldInfo field = (FieldInfo) result.getInfo();
				CommonClassInfo declarator = getDeclarator(targetClass, field);
				navigationBar.update(declarator, field);
			} else if (result.getInfo() instanceof CommonClassInfo) {
				// Could either be the target class, or an inner class.
				CommonClassInfo clazz = (CommonClassInfo) result.getInfo();
				navigationBar.update(clazz, null);
			}
		} else {
			// Can't find a declaration, just use the class.
			navigationBar.update(targetClass, null);
		}
	}

	/**
	 * Some selections belong to fields/methods of inner classes. In these cases the 'current' class is not
	 * the correct parent of the selection, so we need to look up the {@link CommonClassInfo} of the inner.
	 *
	 * @param fallback
	 * 		Class result to use as fallback if the declarator of the member cannot be found.
	 * @param member
	 * 		Member to find declaring type of.
	 *
	 * @return Class that declares the member, or the given fallback.
	 */
	private CommonClassInfo getDeclarator(CommonClassInfo fallback, MemberInfo member) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null) {
			String owner = member.getOwner();
			CommonClassInfo result = workspace.getResources().getClass(owner);
			if (result == null)
				result = workspace.getResources().getDexClass(owner);
			if (result != null)
				return result;
		}
		return fallback;
	}

	/**
	 * Updates the navbar to show the components of classInfo and (optionally) memberInfo.
	 *
	 * @param classInfo
	 * 		Class information to update the navbar with.
	 * @param memberInfo
	 * 		Member info to show after the class node.
	 */
	public void update(CommonClassInfo classInfo, MemberInfo memberInfo) {
		this.lastClassInfo = classInfo;

		// Hide if config disables displaying the navigation bar
		if (!shouldShow()) {
			animateShown(false);
			return;
		}

		String[] elements = classInfo.getName().split("/");
		if (elements.length == 0)
			return;

		getChildren().clear();
		for (int i = 0; i < elements.length; i++) {
			boolean isLast = i == elements.length - 1;
			String elementText = TextDisplayUtil.shortenEscapeLimit(elements[i]);

			// Last element will always be a class.
			if (isLast) {
				// Let people click on the class to bring up the member selection menu.
				Node icon = Icons.getClassIcon(classInfo);
				getChildren().add(new ClassMemberNavigationNode(elementText, icon, classInfo));
			} else {
				// Normal elements don't need icons since they're package names.
				getChildren().add(new Label(elementText));
				getChildren().add(new NavigationSeparator());
			}
		}

		// No member selected, nothing left to do.
		if (memberInfo == null)
			return;

		getChildren().add(new NavigationSeparator());
		Node icon = null;
		if (memberInfo instanceof MethodInfo) {
			MethodInfo methodInfo = (MethodInfo) memberInfo;
			icon = Icons.getMethodIcon(methodInfo);
		} else if (memberInfo instanceof FieldInfo) {
			FieldInfo fieldInfo = (FieldInfo) memberInfo;
			icon = Icons.getFieldIcon(fieldInfo);
		} else {
			logger.error("Cannot find icon for unknown type: " + memberInfo.getName());
		}
		// Let people click on the method/field to bring up the member selection menu
		getChildren().add(new MemberActionNode(memberInfo.getName(), icon, classInfo, memberInfo));
		animateShown(true);
	}

	/**
	 * Handle animation of sliding the navigation bar out from under the {@link me.coley.recaf.ui.window.MainMenu}.
	 *
	 * @param shown
	 *        {@code true} to open the bar.
	 *        {@code false} to close the bar.
	 */
	private void animateShown(boolean shown) {
		if (shown == lastShownState)
			return;
		else if (!shouldShow())
			shown = false;
		setVisible(true);
		lastShownState = shown;
		Transition expand = new Transition() {
			{
				setCycleDuration(Duration.millis(EXPAND_ANIM_MS));
			}

			@Override
			protected void interpolate(double frac) {
				Parent parent = getParent();
				if (parent instanceof Region)
					((Region) parent).setPrefHeight(30 + frac * 30);
			}
		};
		expand.setInterpolator(Interpolator.EASE_BOTH);
		expand.setRate(shown ? 1 : -1);
		expand.play();
	}

	/**
	 * @return Utility call to {@link me.coley.recaf.config.container.DisplayConfig#showSelectionNavbar}.
	 */
	private static boolean shouldShow() {
		return Configs.display().showSelectionNavbar;
	}

	/**
	 * Clears all children and makes self invisible.
	 */
	public void clear() {
		animateShown(false);
	}

	/**
	 * Gets the navigation bar instance.
	 * Note: There is only going to be one navigation bar for now until the docking system is reworked.
	 */
	private static final NavigationBar instance = new NavigationBar();

	/**
	 * @return Singleton instance of nav-bar.
	 */
	public static NavigationBar getInstance() {
		return instance;
	}

	/**
	 * Simple separator drawn as "&gt;"
	 *
	 * @author yapht
	 */
	public static class NavigationSeparator extends Canvas {
		public NavigationSeparator() {
			setWidth(5);
			setHeight(15);

			double pointY = (getHeight() / 2);
			double pointX = getWidth() - 1;

			// Draw a grey chevron
			GraphicsContext gc = getGraphicsContext2D();
			gc.setStroke(Color.rgb(90, 90, 90));
			gc.strokeLine(0, 0, pointX, pointY);
			gc.strokeLine(pointX, pointY, 0, getHeight());
		}
	}

	/**
	 * Nav-bar entry for {@link CommonClassInfo}.
	 *
	 * @author yapht
	 */
	private static class ClassMemberNavigationNode extends Label {
		private final CommonClassInfo classInfo;
		private boolean isContextMenuAlreadyOpen = false;

		/**
		 * @param text
		 * 		Name of class.
		 * @param icon
		 * 		Icon of class.
		 * @param classInfo
		 * 		The class represented.
		 */
		public ClassMemberNavigationNode(String text, Node icon, CommonClassInfo classInfo) {
			super(text, icon);
			this.classInfo = classInfo;
			// Setup showing menu
			setOnMouseClicked(this::showMenu);
		}

		private void showMenu(MouseEvent event) {
			// Don't allow multiple menus to be open at once.
			if (isContextMenuAlreadyOpen)
				return;
			isContextMenuAlreadyOpen = true;
			// Create new context menu.
			ScrollableContextMenu menu = new ScrollableContextMenu();
			menu.setMaxHeight(500);
			menu.addEventHandler(Menu.ON_HIDING, e -> isContextMenuAlreadyOpen = false);
			// Stupid workaround for the menu not showing at the anchor.
			menu.getItems().add(new MenuItem("hack"));
			menu.show(this, Side.BOTTOM, 0, 5);
			menu.getItems().clear();
			// Show the other class members.
			for (FieldInfo field : classInfo.getFields())
				menu.getItems().add(new ActionMenuItem(field.getName(), Icons.getFieldIcon(field), () -> CommonUX.openMember(classInfo, field)));
			for (MethodInfo method : classInfo.getMethods())
				menu.getItems().add(new ActionMenuItem(method.getName(), Icons.getMethodIcon(method), () -> CommonUX.openMember(classInfo, method)));
		}
	}

	/**
	 * Nav-bar entry for {@link FieldInfo} and {@link MethodInfo}.
	 *
	 * @author Matt Coley
	 */
	private static class MemberActionNode extends Label {
		private final CommonClassInfo classInfo;
		private final MemberInfo memberInfo;
		private boolean isContextMenuAlreadyOpen = false;

		/**
		 * @param text
		 * 		Name of member.
		 * @param icon
		 * 		Icon of member.
		 * @param classInfo
		 * 		The class declaring the member.
		 * @param memberInfo
		 * 		The member represented.
		 */
		public MemberActionNode(String text, Node icon, CommonClassInfo classInfo, MemberInfo memberInfo) {
			super(text, icon);
			this.classInfo = classInfo;
			this.memberInfo = memberInfo;
			// Setup showing menu
			setOnMouseClicked(this::showMenu);
		}

		private void showMenu(MouseEvent event) {
			// Don't allow multiple menus to be open at once.
			if (isContextMenuAlreadyOpen)
				return;
			isContextMenuAlreadyOpen = true;
			// Create standard member context menus.
			ContextMenu menu;
			if (memberInfo.isField()) {
				menu = ContextBuilder.forField(classInfo, (FieldInfo) memberInfo)
						.setDeclaration(true)
						.build();
			} else {
				menu = ContextBuilder.forMethod(classInfo, (MethodInfo) memberInfo)
						.setDeclaration(true)
						.build();
			}
			menu.addEventHandler(Menu.ON_HIDING, e -> isContextMenuAlreadyOpen = false);
			menu.show(this, Side.BOTTOM, 0, 5);
		}
	}

}
