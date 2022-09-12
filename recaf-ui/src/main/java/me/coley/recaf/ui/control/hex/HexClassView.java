package me.coley.recaf.ui.control.hex;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.ClassMember;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.control.CollapsibleTabPane;
import me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfo;
import me.coley.recaf.ui.control.hex.clazz.ClassOffsetInfoType;
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
	public void populateSideTabs(CollapsibleTabPane tabPane) {
		if (classOffsetInfo == null) {
			classOffsetInfo = new HexClassInfo(this);
		}
		tabPane.getTabs().add(classOffsetInfo.createClassInfoTab());
		super.populateSideTabs(tabPane);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof ClassInfo) {
			this.classInfo = (ClassInfo) newValue;
			onUpdate(classInfo.getValue());
			// Update class info tab
			ClassFileReader reader = new ClassFileReader();
			reader.setDropDupeAnnotations(false);
			reader.setDropForwardVersioned(false);
			try {
				if (classOffsetInfo != null)
					classOffsetInfo.onUpdate(new ClassOffsetMap(reader.read(classInfo.getValue())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public boolean supportsMemberSelection() {
		return true;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return classInfo != null;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		ClassOffsetMap offsetMapWrapper = classOffsetInfo.getOffsetMap();
		for (ClassOffsetInfo info : offsetMapWrapper.getMap().values()) {
			ClassOffsetInfoType type = info.getType();
			// If the type is FIELDS or METHODS then we know it's child values will all be either
			// fields or methods. So we'll loop over those.
			if ((memberInfo.isField() && type == ClassOffsetInfoType.FIELDS) ||
					(memberInfo.isMethod() && type == ClassOffsetInfoType.METHODS)) {
				// Iterate and match
				ClassFile cf = info.getClassFile();
				for (ClassOffsetInfo infoChild : info.getChildren()) {
					ClassMember member = (ClassMember) infoChild.getValue();
					String name = cf.getPool().getUtf(member.getNameIndex());
					String desc = cf.getPool().getUtf(member.getTypeIndex());
					if (memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
						selectRange(EditableHexLocation.RAW, infoChild.getStart(), infoChild.getEnd());
						return;
					}
				}
			}
		}
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}
}
