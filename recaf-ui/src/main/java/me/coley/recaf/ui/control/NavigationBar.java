package me.coley.recaf.ui.control;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.ParseHitResult;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Navigation bar implementation. Helpful for easily navigating around a class.
 * @author yapht
 */
public class NavigationBar extends HBox {
    private static final Logger logger = Logging.get(NavigationBar.class);

    private static class NavigationSeparator extends Canvas {
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

    private static class MemberNavigationNode extends Label {
        private static class ScrollableContextMenu extends ContextMenu {
            public ScrollableContextMenu() {
                // https://stackoverflow.com/a/58542568
                addEventHandler(Menu.ON_SHOWING, e -> {
                    Node content = getSkin().getNode();
                    if (content instanceof Region) {
                        ((Region) content).setMaxHeight(getMaxHeight());
                    }
                });
            }
        }

        private final CommonClassInfo classInfo;
        private boolean isContextMenuAlreadyOpen = false;

        public MemberNavigationNode(String text, CommonClassInfo classInfo, Node icon) {
            super(text);
            this.classInfo = classInfo;

            setOnMouseClicked(event -> onMouseClicked());
            setGraphic(icon);
        }

        public MemberNavigationNode(String text, CommonClassInfo classInfo) {
            this(text, classInfo, Icons.getClassIcon(classInfo));
        }

        private void onMouseClicked() {
            // Don't allow multiple menus to be open at once
            if(isContextMenuAlreadyOpen)
                return;

            isContextMenuAlreadyOpen = true;

            ScrollableContextMenu menu = new ScrollableContextMenu();
            menu.setMaxHeight(500);
            menu.addEventHandler(Menu.ON_HIDING, e -> isContextMenuAlreadyOpen = false);

            // Stupid workaround for the menu not showing at the anchor
            menu.getItems().add(new MenuItem("hack"));
            menu.show(this, Side.BOTTOM, 0, 5);
            menu.getItems().clear();

            for(FieldInfo field : classInfo.getFields())
                menu.getItems().add(new ActionMenuItem(field.getName(), Icons.getFieldIcon(field), () -> CommonUX.openMember(classInfo, field)));

            for(MethodInfo method : classInfo.getMethods())
                menu.getItems().add(new ActionMenuItem(method.getName(), Icons.getMethodIcon(method), () -> CommonUX.openMember(classInfo, method)));

          }
    }

    private NavigationBar()  {
        getStyleClass().add("navbar");

        setAlignment(Pos.CENTER_LEFT);
        setHeight(30);
        setFillHeight(true);

        setVisible(false);
        managedProperty().bind(visibleProperty());

        RecafDockingManager docking = RecafDockingManager.getInstance();
        docking.addTabSelectionListener((region, tab) -> {
            if (tab instanceof ClassTab){
                ClassTab classTab = (ClassTab) tab;
                CommonClassInfo tabClassInfo = classTab.getClassRepresentation().getCurrentClassInfo();
                update(tabClassInfo, null);
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
        CommonClassInfo targetClass = area.getCurrentClassInfo();
        if (targetClass == null)
            return;
        NavigationBar navigationBar = NavigationBar.getInstance();
        Optional<ParseHitResult> infoAtPosition =area. declarationAtPosition(area.getCaretPosition());
        if (infoAtPosition.isPresent()) {
            ParseHitResult result = infoAtPosition.get();
            if (result.getInfo() instanceof MethodInfo) {
                navigationBar.update(targetClass, (MemberInfo) result.getInfo());
            } else if (result.getInfo() instanceof FieldInfo) {
                navigationBar.update(targetClass, (FieldInfo) result.getInfo());
            } else if (result.getInfo() instanceof CommonClassInfo){
                // Could either be the target class, or an inner class.
                navigationBar.update((CommonClassInfo) result.getInfo(), null);
            }
        } else {
            // Can't find a declaration, just use the class.
            navigationBar.update(targetClass, null);
        }
    }

    /**
     * Updates the navbar to show the components of classInfo and (optionally) memberInfo.
     * @param classInfo
     *  Class information to update the navbar with.
     * @param memberInfo
     *  Member info to show after the class node.
     */
    public void update(CommonClassInfo classInfo, MemberInfo memberInfo) {
        setVisible(true);
        managedProperty().bind(visibleProperty());

        String[] elements = classInfo.getName().split("/");

        if(elements.length == 0)
            return;

        getChildren().clear();
        for(int i = 0; i < elements.length; i++){
            boolean isLast = i == elements.length - 1;

            String elementText = elements[i];

            // Last element will always be a class
            if(isLast) {
                getChildren().add(new MemberNavigationNode(elementText, classInfo));
            } else {
                // Normal elements don't need icons
                getChildren().add(new Label(elementText));
                getChildren().add(new NavigationSeparator());
            }
        }

        if(memberInfo == null)
            return;

        getChildren().add(new NavigationSeparator());

        Node icon = null;
        if(memberInfo instanceof MethodInfo) {
            MethodInfo methodInfo = (MethodInfo)memberInfo;
            icon = Icons.getMethodIcon(methodInfo);
        } else if (memberInfo instanceof FieldInfo) {
            FieldInfo fieldInfo = (FieldInfo)memberInfo;
            icon = Icons.getFieldIcon(fieldInfo);
        } else {
            logger.error("Cannot find icon for unknown type: " + memberInfo.getName());
        }

        // Let people click on the method/field to bring up the member selection menu
        getChildren().add(new MemberNavigationNode(memberInfo.getName(), classInfo, icon));
    }

    /**
     * Clears all children and makes self invisible.
     */
    public void clear() {
        setVisible(false);
        managedProperty().bind(visibleProperty());
        getChildren().clear();
    }

    /**
     * Gets the navigation bar instance.
     * Note: There is only going to be one navigation bar for now until the docking system is reworked.
     */
    private static final NavigationBar instance = new NavigationBar();

    public static NavigationBar getInstance() {
        return instance;
    }
}
