package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import me.coley.event.Bus;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.event.ClassDirtyEvent;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

public class ReflectiveOpcodeSheet extends ReflectivePropertySheet {
	/**
	 * Opcode list editor that the opcode is displayed on.
	 */
	private final InsnListEditor list;

	public ReflectiveOpcodeSheet(InsnListEditor list, AbstractInsnNode insn) {
		// Do not call super, we need to set the list before we setup items
		this.list = list;
		refresh(insn);
		setSearchBoxVisible(false);
		setModeSwitcherVisible(false);
	}

	public void refresh(AbstractInsnNode insn) {
		Platform.runLater(() -> {
			getItems().clear();
			// setup items
			setupItems(insn);
			// allow swapping of object type in LDC's cst.
			if (insn.getType() == AbstractInsnNode.LDC_INSN) {
				addTypeSwitcher(insn);
			}
			addOpcodeSwitcher(insn);
		});
	}

	private void addTypeSwitcher(AbstractInsnNode insn) {
		Field field = getOpcode(insn);
		String name = field.getName();
		String group = "ui.bean.opcode";
		getItems().add(new SwitchTypeItem((LdcInsnNode) insn, group, name));
	}

	private void addOpcodeSwitcher(AbstractInsnNode insn) {
		Field field = getOpcode(insn);
		String name = field.getName();
		String group = "ui.bean.opcode";
		getItems().add(new SwitchOpcodeItem(list.getClassNode(), insn, field, group, name));
	}

	/**
	 * Item & editor for swapping opcodes of insns.
	 * 
	 * @author Matt
	 */
	public static class SwitchOpcodeItem extends ReflectiveItem {
		private ClassNode node;


		public SwitchOpcodeItem(ClassNode node, Object owner, Field field, String categoryKey, String translationKey) {
			super(owner, field, categoryKey, translationKey);
			this.node = node;
		}

		@Override
		protected Class<?> getEditorType() {
			return OpcodeSwitchEditor.class;
		}


		@Override
		public void setValue(Object value) {
			if (checkCaller() && !value.equals(getValue())) {
				super.setValue(value);
				Bus.post(new ClassDirtyEvent(node));
			}
		}

		
		public static class OpcodeSwitchEditor<K extends Integer> extends CustomEditor<K> {

			public OpcodeSwitchEditor(Item item) {
				super(item);
			}

			@Override
			public Node getEditor() {
				AbstractInsnNode insn = (AbstractInsnNode) item.getOwner();
				GridPane pane = new GridPane();
				pane.setPadding(new Insets(5, 5, 5, 5));
				pane.setVgap(5);
				pane.setHgap(5);
				// pane.setAlignment(Pos.CENTER);
				int x = 0, y = 0, max = 3;
				ToggleGroup tg = new ToggleGroup();
				for (String text : OpcodeUtil.typeToCodes(insn.getType())) {
					int value = OpcodeUtil.nameToOpcode(text);
					RadioButton radio = new RadioButton(text);
					if (value == insn.getOpcode()) {
						radio.setSelected(true);
					}
					radio.setOnAction(e -> {
						item.setValue(value);
					});
					tg.getToggles().add(radio);
					pane.add(radio, x, y);
					x++;
					if (x >= max) {
						x = 0;
						y++;
					}
				}

				TitledPane tp = new TitledPane(Lang.get("ui.bean.opcode.opcode.name"), pane);
				tp.setCollapsible(false);
				return tp;
			}

		}
	}

	/**
	 * Item & editor for switching the type of constant values in LDC nodes.
	 * 
	 * @author Matt
	 */
	public static class SwitchTypeItem extends ReflectiveItem {

		public SwitchTypeItem(LdcInsnNode owner, String categoryKey, String translationKey) {
			super(owner, getCST(owner), categoryKey, translationKey);
		}

		@Override
		protected Class<?> getEditorType() {
			return TypeSwitchEditor.class;
		}

		public static class TypeSwitchEditor<K extends Integer> extends CustomEditor<K> {

			public TypeSwitchEditor(Item item) {
				super(item);
			}

			@Override
			public Node getEditor() {
				LdcInsnNode ldc = (LdcInsnNode) item.getOwner();
				GridPane pane = new GridPane();
				pane.setPadding(new Insets(5, 5, 5, 5));
				pane.setVgap(5);
				pane.setHgap(5);
				int x = 0, y = 0, max = 3;
				ToggleGroup tg = new ToggleGroup();
				//@formatter:off
				List<LdcType> types = Arrays.asList(
					new LdcType.IntType(), 
					new LdcType.LongType(),
					new LdcType.FloatType(), 
					new LdcType.DoubleType(), 
					new LdcType.StringType(), 
					new LdcType.TypeType()
				);
				//@formatter:on
				for (LdcType type : types) {
					RadioButton radio = new RadioButton(type.text());
					// Works for primitives?
					if (ldc.cst.getClass().equals(type.getTypeClass())) {
						radio.setSelected(true);
					}
					radio.setOnAction(e -> {
						type.onSet(ldc);
						type.refresh(ldc, radio);
					});
					tg.getToggles().add(radio);
					pane.add(radio, x, y);
					x++;
					if (x >= max) {
						x = 0;
						y++;
					}
				}

				TitledPane tp = new TitledPane(Lang.get("ui.bean.opcode.cst.type"), pane);
				tp.setCollapsible(false);
				return tp;
			}

			/**
			 * Types that LDC can hold. <br>
			 * Technically it can store MethodHandles <i>(last time it checked,
			 * that worked)</i> but I don't think anyone would use it enough to
			 * excuse putting in the effort.
			 * 
			 * @author Matt
			 */
			private abstract static class LdcType {
				abstract String text();

				public void refresh(LdcInsnNode ldc, Node node) {
					while (!(node instanceof ReflectiveOpcodeSheet)) {
						node = node.parentProperty().getValue();
						if (node == null) {
							Thread.dumpStack();
							return;
						}
					}
					ReflectiveOpcodeSheet sheet = (ReflectiveOpcodeSheet) node;
					sheet.refresh(ldc);
				}

				abstract Class<?> getTypeClass();

				abstract void onSet(LdcInsnNode ldc);

				private static class IntType extends LdcType {
					@Override
					String text() {
						return "int";
					}

					@Override
					Class<?> getTypeClass() {
						return Integer.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						Object cst = ldc.cst;
						if (cst instanceof String) {
							try {
								ldc.cst = Integer.parseInt(cst.toString());
							} catch (Exception e) {
								ldc.cst = 0;
							}
						} else if (cst instanceof Number) {
							ldc.cst = ((Number) cst).intValue();
						} else {
							ldc.cst = 0;
						}
					}
				}

				private static class LongType extends LdcType {
					@Override
					String text() {
						return "long";
					}

					@Override
					Class<?> getTypeClass() {
						return Long.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						Object cst = ldc.cst;
						if (cst instanceof String) {
							try {
								ldc.cst = Long.parseLong(cst.toString());
							} catch (Exception e) {
								ldc.cst = 0L;
							}
						} else if (cst instanceof Number) {
							ldc.cst = ((Number) cst).longValue();
						} else {
							ldc.cst = 0L;
						}
					}
				}

				private static class FloatType extends LdcType {
					@Override
					String text() {
						return "float";
					}

					@Override
					Class<?> getTypeClass() {
						return Float.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						Object cst = ldc.cst;
						if (cst instanceof String) {
							try {
								ldc.cst = Float.parseFloat(cst.toString());
							} catch (Exception e) {
								ldc.cst = 0F;
							}
						} else if (cst instanceof Number) {
							ldc.cst = ((Number) cst).floatValue();
						} else {
							ldc.cst = 0F;
						}
					}
				}

				private static class DoubleType extends LdcType {
					@Override
					String text() {
						return "double";
					}

					@Override
					Class<?> getTypeClass() {
						return Double.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						Object cst = ldc.cst;
						if (cst instanceof String) {
							try {
								ldc.cst = Double.parseDouble(cst.toString());
							} catch (Exception e) {
								ldc.cst = 0D;
							}
						} else if (cst instanceof Number) {
							ldc.cst = ((Number) cst).doubleValue();
						} else {
							ldc.cst = 0D;
						}
					}
				}

				private static class StringType extends LdcType {
					@Override
					String text() {
						return "String";
					}

					@Override
					Class<?> getTypeClass() {
						return String.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						ldc.cst = ldc.cst.toString();
					}
				}

				private static class TypeType extends LdcType {
					@Override
					String text() {
						return "Type";
					}

					@Override
					Class<?> getTypeClass() {
						return Type.class;
					};

					@Override
					void onSet(LdcInsnNode ldc) {
						Object cst = ldc.cst;
						if (cst instanceof String) {
							Type t = TypeUtil.parse(cst.toString());
							if (t != null) {
								ldc.cst = t;
							}
						}
						ldc.cst = Type.getObjectType("java/lang/Type");
					}
				}
			}

		}
	}

	private static Field getOpcode(AbstractInsnNode insn) {
		Class<?> c = insn.getClass();
		while (c != AbstractInsnNode.class) {
			c = c.getSuperclass();
			// fail
			if (c == Object.class) {
				return null;
			}
		}
		try {
			return c.getDeclaredField("opcode");
		} catch (Exception e) {}
		return null;
	}

	private static Field getCST(LdcInsnNode insn) {
		Class<?> c = insn.getClass();
		try {
			return c.getDeclaredField("cst");
		} catch (Exception e) {}
		return null;
	}

	@Override
	protected void setupItems(Object instance) {
		for (Field field : Reflect.fields(instance.getClass())) {
			field.setAccessible(true);
			// Setup item & add to list
			String name = field.getName();
			String group = "ui.bean.opcode";
			getItems().add(new ReflectiveOpcodeItem(list, (AbstractInsnNode) instance, field, group, name.toLowerCase()));
		}
	}
}
