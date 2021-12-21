package me.wolfie.recaf.impl;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

public class workspace extends BaseLuaLibImpl {
    public static class testMethod extends ZeroArgFunction {
        public LuaValue call() {
            return LuaValue.valueOf(1);
        }
    }

    public static class awesomeMethod extends ZeroArgFunction {
        public LuaValue call() {
            return LuaValue.valueOf("cool string");
        }
    }
}

