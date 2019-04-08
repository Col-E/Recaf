package me.coley.recaf.ui.component;

import org.objectweb.asm.*;
import org.objectweb.asm.Label;

import static org.objectweb.asm.Opcodes.*;

import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import me.coley.event.*;
import me.coley.recaf.DependencyChecks;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.event.*;
import me.coley.recaf.util.*;

/**
 * Pane displaying logging information.
 * 
 * @author Matt
 */
public class LoggingPane extends BorderPane {
	private final ListView<LogEvent> list = new ListView<>();

	public LoggingPane() {
		Bus.subscribe(this);
		setCenter(list);
		Skin<?> skin = createSkin();
		if (skin != null) {
			list.setSkin(skin);
		}

		// Click-to-toggle log expansion
		list.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				try {
					Refreshable r = (Refreshable) list.getSkin();
					r.refresh();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
		// Log rendering
		list.setCellFactory(param -> new ListCell<LogEvent>() {
			@Override
			public void updateItem(LogEvent item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					// Reset 'hidden' items
					setGraphic(null);
					setText(null);
				} else {
					// Get icon for quick level identification
					Image fxImage = Icons.getLog(item.getLevel());
					ImageView imageView = new ImageView(fxImage);
					setGraphic(imageView);
					// Set log, check if it should be collapsed.
					LogEvent selected = list.selectionModelProperty().getValue().getSelectedItem();
					boolean isSelected = (selected != null) && selected.equals(item);
					if (isSelected) {
						setText(item.getMessage());
					} else {
						String substr = item.getMessage();
						if (substr.contains("\n")) {
							substr = substr.substring(0, substr.indexOf("\n")) + ("... (+)");
						}
						setText(substr);
					}
				}
			}
		});
	}

	@Listener
	public void onLog(LogEvent event) {
		// print if within logging detail level
		if (event.getLevel().ordinal() >= ConfDisplay.instance().loglevel.ordinal()) {
			list.getItems().add(event);
			Threads.runFx(() -> {
				list.scrollTo(list.getItems().size() - 1);
			});

		}
	}

	public ListView<LogEvent> getLoggingView() {
		return list;
	}

	/**
	 * @return Runtime generated skin class.
	 */
	private Skin<?> createSkin() {
		// In JDK-8:
		// - extends com.sun.javafx.scene.control.skin.ListViewSkin
		// In JDK-9+:
		// - extends javafx.scene.control.skin.ListViewSkin
		try {
			return (Skin<?>) generateSkin(DependencyChecks.getVersion() == 1.8, list);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		// Ideally this would just be a simple multi-release jar with different
		// code for the versions, but there is no way to have that done with
		// current maven tools as far as I'm aware... So runtime class defintion
		// it is! Yay!
	}

	/**
	 * Auto-generated code via ASMifier. Generates the skin class and chooses
	 * the appropriate superName based on the current runtime version.
	 * 
	 * @param jdk8
	 *            {@code true} if the current runtime version is jdk8.
	 *            {@code false} if the runtime version is higher.
	 * @param list
	 *            The ListView that the skin will be applied to.
	 * @return Skin for the ListView.
	 * @throws Exception
	 *             Thrown if defining the class fails.
	 */
	public static Object generateSkin(boolean jdk8, ListView<LogEvent> list) throws Exception {
		String name = "me/coley/recaf/ui/component/LoggingPane$RefreshableSkin";
		String superName = jdk8 ? "com/sun/javafx/scene/control/skin/ListViewSkin" : "javafx/scene/control/skin/ListViewSkin";
		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		cw.visit(jdk8 ? V1_8 : V9, ACC_SUPER | ACC_PUBLIC, name, "L" + superName + "<Lme/coley/recaf/event/LogEvent;>;", ""
				+ superName + "", new String[] { "me/coley/recaf/ui/component/LoggingPane$Refreshable" });
		cw.visitSource("LoggingPane.java", null);
		cw.visitInnerClass("me/coley/recaf/ui/component/LoggingPane$RefreshableSkin", "me/coley/recaf/ui/component/LoggingPane",
				"RefreshableSkin", ACC_STATIC);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljavafx/scene/control/ListView;)V",
					"(Ljavafx/scene/control/ListView<Lme/coley/recaf/event/LogEvent;>;)V", null);
			mv.visitCode();
			Label label0 = new Label();
			mv.visitLabel(label0);
			mv.visitLineNumber(104, label0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "" + superName + "", "<init>", "(Ljavafx/scene/control/ListView;)V", false);
			Label label1 = new Label();
			mv.visitLabel(label1);
			mv.visitLineNumber(105, label1);
			mv.visitInsn(RETURN);
			Label label2 = new Label();
			mv.visitLabel(label2);
			mv.visitLocalVariable("this", "Lme/coley/recaf/ui/component/LoggingPane$RefreshableSkin;", null, label0, label2, 0);
			mv.visitLocalVariable("listView", "Ljavafx/scene/control/ListView;",
					"Ljavafx/scene/control/ListView<Lme/coley/recaf/event/LogEvent;>;", label0, label2, 1);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "refresh", "()V", null, null);
			mv.visitCode();
			Label label0 = new Label();
			mv.visitLabel(label0);
			mv.visitLineNumber(111, label0);
			mv.visitVarInsn(ALOAD, 0);
			// Get protected flow field
			String flowOwner = jdk8 ? "com/sun/javafx/scene/control/skin/VirtualContainerBase"
					: "javafx/scene/control/skin/VirtualContainerBase";
			String flowDesc = jdk8 ? "Lcom/sun/javafx/scene/control/skin/VirtualFlow;"
					: "Ljavafx/scene/control/skin/VirtualFlow;";
			mv.visitFieldInsn(GETFIELD, flowOwner, "flow", flowDesc);
			mv.visitVarInsn(ASTORE, 1);
			Label label1 = new Label();
			mv.visitLabel(label1);
			mv.visitLineNumber(112, label1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("recreateCells");
			mv.visitMethodInsn(INVOKESTATIC, "me/coley/recaf/util/Reflect", "invoke",
					"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
			mv.visitInsn(POP);
			Label label2 = new Label();
			mv.visitLabel(label2);
			mv.visitLineNumber(113, label2);
			mv.visitInsn(RETURN);
			Label label3 = new Label();
			mv.visitLabel(label3);
			mv.visitLocalVariable("this", "Lme/coley/recaf/ui/component/LoggingPane$RefreshableSkin;", null, label0, label3, 0);
			mv.visitLocalVariable("flow", "Ljava/lang/Object;", null, label1, label3, 1);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		cw.visitEnd();
		byte[] clazz = cw.toByteArray();

		return Define.create(name.replace("/", "."), clazz, new Class[] { list.getClass() }, new Object[] { list });
	}

	public static interface Refreshable {
		void refresh();
	}
}
