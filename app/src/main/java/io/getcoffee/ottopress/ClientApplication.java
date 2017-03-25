package io.getcoffee.ottopress;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.getcoffee.ottopress.core.RoomType;
import io.getcoffee.ottopress.core.RoomTypeParser;
import io.getcoffee.ottopress.module.ModuleManager;

public class ClientApplication extends Application {

    public final static String MODULES_PREFS = "io.getcoffee.ottopress.MODULE_PREF_KEY";
    public final static String CORE_PREFS = "io.getcoffee.ottopress.MODULE_PREF_KEY";
    public final static String MODULES_PREFS_PRELOAD = "preload";

    public SharedPreferences modulePrefs;
    public SharedPreferences corePrefs;
    public ModuleManager moduleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        modulePrefs = getApplicationContext().getSharedPreferences(MODULES_PREFS, Context.MODE_PRIVATE);
        corePrefs = getApplicationContext().getSharedPreferences(CORE_PREFS, Context.MODE_PRIVATE);
        moduleManager = new ModuleManager(getApplicationContext());

        initializeModules();
        initializeCore();
    }

    protected void initializeModules() {
        Set<String> commonModules = new HashSet<>();
        modulePrefs.getStringSet(MODULES_PREFS_PRELOAD, commonModules);
        int initSize = commonModules.size();
        for(String moduleName : commonModules) {
            boolean verified = moduleManager.loadModule(moduleName);
            if(!verified) {
               commonModules.remove(moduleName);
            }
        }
        if(initSize != commonModules.size()) {
            SharedPreferences.Editor editor = modulePrefs.edit();
            editor.putStringSet(MODULES_PREFS_PRELOAD, commonModules);
            editor.apply();
        }
    }

    protected void initializeCore() {
        RoomTypeParser roomTypeParser = new RoomTypeParser(getApplicationContext());
        Map<String, RoomType> roomTypes = roomTypeParser.parse(getApplicationContext().getResources().getXml(R.xml.room_types));
        System.out.println(roomTypes);
    }

}
