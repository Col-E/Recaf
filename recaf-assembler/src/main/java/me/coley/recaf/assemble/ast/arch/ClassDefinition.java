package me.coley.recaf.assemble.ast.arch;

import java.util.List;

public class ClassDefinition extends AbstractMemberDefinition implements MemberDefinition{

    List<MethodDefinition> definedMethods;
    List<FieldDefinition> definedFields;

    String name;
    String superClass;
    List<String> interfaces;

    @Override
    public boolean isMethod() {
        return false;
    }

    @Override
    public boolean isClass() {
        return true;
    }

    @Override
    public String getDesc() {
        // Not relevant here
        return "";
    }

    @Override
    public String getName() {
        return name;
    }

    public List<MethodDefinition> getDefinedMethods() {
        return definedMethods;
    }

    public List<FieldDefinition> getDefinedFields() {
        return definedFields;
    }

    public String getSuperClass() {
        return superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    @Override
    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(name);
        if(superClass != null){
            sb.append("\nextends ").append(superClass);
        }
        if(interfaces != null && !interfaces.isEmpty()){
            for(int i = 0; i < interfaces.size(); i++){
                sb.append("implements ");
                sb.append(interfaces.get(i));
                if(i < interfaces.size() - 1){
                    sb.append("\n");
                }
            }
        }
        sb.append("\n");
        for(FieldDefinition field : definedFields){
            sb.append(field.print());
        }
        for(MethodDefinition method : definedMethods){
            sb.append(method.print());
        }
        return sb.toString();
    }
}
