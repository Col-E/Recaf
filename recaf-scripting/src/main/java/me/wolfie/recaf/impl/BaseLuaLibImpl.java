package me.wolfie.recaf.impl;

import me.wolfie.recaf.ScriptEngine;
import org.luaj.vm2.LuaTable;

public class BaseLuaLibImpl extends LuaTable {
    public BaseLuaLibImpl() {
        ScriptEngine.initialize(this);
    }
}



