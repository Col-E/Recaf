package me.coley.recaf.ui.control;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

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
                addEventHandler(Menu.ON_SHOWING, e -> {
                    Node content = getSkin().getNode();
                    if (content instanceof Region) {
                        ((Region) content).setMaxHeight(getMaxHeight());
                    }
                });
            }
        }

        private final CommonClassInfo classInfo;

        public MemberNavigationNode(String text, CommonClassInfo classInfo) {
            super(text);
            this.classInfo = classInfo;

            setOnMouseClicked(event -> onMouseClicked());
            setGraphic(Icons.getClassIcon(classInfo));
        }

        public MemberNavigationNode(String text, CommonClassInfo classInfo, Node icon) {
            super(text);
            this.classInfo = classInfo;

            setOnMouseClicked(event -> onMouseClicked());
            setGraphic(icon);
        }

        private void onMouseClicked() {
            ScrollableContextMenu menu = new ScrollableContextMenu();
            menu.setMaxHeight(500);

            // Stupid workaround for the menu not showing at the anchor
            menu.getItems().add(new MenuItem("hack"));
            menu.show(this, Side.BOTTOM, 0, 5);
            menu.getItems().clear();

            for(MethodInfo method : classInfo.getMethods())
                menu.getItems().add(new ActionMenuItem(method.getName(), Icons.getMethodIcon(method), () -> CommonUX.openMember(classInfo, method)));

            for(FieldInfo field : classInfo.getFields())
                menu.getItems().add(new ActionMenuItem(field.getName(), Icons.getFieldIcon(field), () -> CommonUX.openMember(classInfo, field)));
        }
    }

    private NavigationBar()  {
        setStyle(
                "-fx-spacing: 8px;" +
                "-fx-padding: 4 0 4 8;");
        setAlignment(Pos.CENTER_LEFT);
        setHeight(30);
        setFillHeight(true);

        setVisible(false);
        managedProperty().bind(visibleProperty());
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
