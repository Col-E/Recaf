package me.coley.recaf.ui.control.hex;

import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.control.CollapsibleTabPane;
import me.coley.recaf.ui.control.hex.clazz.ClassOffsetMap;
import me.coley.recaf.ui.control.hex.clazz.HexClassInfo;

/**
 * Extension of the hex viewer for class files.
 *
 * @author Matt Coley
 */
public class HexClassView extends HexView implements ClassRepresentation {
	private HexClassInfo classOffsetInfo;
	private ClassInfo classInfo;

	@Override
	protected void addSideTabs(CollapsibleTabPane sideTabs) {
		if (classOffsetInfo == null) {
			classOffsetInfo = new HexClassInfo(this);
		}
		sideTabs.getTabs().add(classOffsetInfo.createClassInfoTab());
		super.addSideTabs(sideTabs);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof ClassInfo) {
			this.classInfo = (ClassInfo) newValue;
			onUpdate(classInfo.getValue());
			// Update class info tab
			ClassFileReader reader = new ClassFileReader();
			reader.setDropDupeAnnotations(false);
			reader.setDropEofAttributes(false);
			reader.setDropForwardVersioned(false);
			reader.setDropIllegalCpRefs(false);
			try {
				classOffsetInfo.onUpdate(new ClassOffsetMap(reader.read(classInfo.getValue())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public boolean supportsMemberSelection() {
		// TODO: Update CafeDude to support "type-factories" allowing different
		//       instance types to represent the same class model.
		//        - Standard format adhereing is the current representation
		//        - Allow a read-only that records file offsets of read data
		//           - Can use that to support member selection and much more in the hex view
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}
}
