package io.getcoffee.ottopress.module;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dalvik.system.DexClassLoader;

public class ModuleLoader {

    public static Attributes.Name MANIFEST_CLASSNAME = new Attributes.Name("Ottopress-Main");

    public String verifyModule(File module) throws IOException {
        if(!(module.toString().endsWith(".apk") || module.toString().endsWith(".jar"))) {
            throw new IllegalArgumentException("Path is not a .jar or .apk");
        }
        JarFile moduleJar = null;
        try {
            moduleJar = new JarFile(module);
            Manifest moduleManifest = moduleJar.getManifest();
            String moduleMainClass = moduleManifest.getMainAttributes().getValue(MANIFEST_CLASSNAME);
            return moduleMainClass;
        } finally {
            if(moduleJar != null) {
                try {
                    moduleJar.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Module loadModule(File moduleFile, String moduleClassName, Context context) {
        File cacheDir = context.getCacheDir();
        try {
            ClassLoader loader = new DexClassLoader(moduleFile.toURI().toURL().toString(), cacheDir.getAbsolutePath(), null, ModuleLoader.class.getClassLoader());
            Class<?> unknownClass = Class.forName(moduleClassName, true, loader);
            if(Module.class.isAssignableFrom(unknownClass)) {
                Class<Module> moduleClass = (Class<Module>) unknownClass;
                return moduleClass.newInstance();
            }
            throw new InvalidModuleException();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvalidModuleException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
