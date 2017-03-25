package io.getcoffee.ottopress.module;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {

    public static final String MODULE_DIR = "modules/";
    Map<String, Module> moduleMap;
    private final Context context;
    private final ModuleLoader loader;

    public ModuleManager(Context context) {
        moduleMap = new HashMap<>();
        this.context = context;
        this.loader = new ModuleLoader();
    }

    public boolean loadModule(String moduleName) {
        File potentialModule = new File(context.getFilesDir().getAbsolutePath() + moduleName);
        try {
            String moduleClassName = loader.verifyModule(potentialModule);
            Module module = loader.loadModule(potentialModule, moduleClassName, context);
            if(module != null) {
                moduleMap.put(module.getDevice(), module);
                return true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public Module getModule(String deviceID) {
        return moduleMap.get(deviceID);
    }
}
