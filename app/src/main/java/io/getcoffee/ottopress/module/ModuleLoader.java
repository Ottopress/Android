package io.getcoffee.ottopress.module;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dalvik.system.DexClassLoader;

public class ModuleLoader {

    public static Attributes.Name MODULE_CLASSNAME = new Attributes.Name("Ottopress-Main");
    public static List<URL> moduleURLS = new ArrayList<>();

    private static List<String> getModuleClasses(String path) {
        List<String> moduleClasses = new ArrayList<>();
        File[] files = new File(path).listFiles(new JarFilter());
        for(File potentialModule : files) {
            JarFile moduleJar = null;
            try {
                moduleJar = new JarFile(potentialModule);
                String moduleMainClass = testPotentialModule(moduleJar);
                moduleClasses.add(moduleMainClass);
                moduleURLS.add(potentialModule.toURI().toURL());
            } catch (IOException ex){
                ex.printStackTrace();
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
        return moduleClasses;
    }

    private static String testPotentialModule(JarFile potentialModule) throws IOException{
        Manifest moduleManifest = potentialModule.getManifest();
        String moduleMainClass = moduleManifest.getMainAttributes().getValue(MODULE_CLASSNAME);
        return moduleMainClass;
    }

    public static List<Module> loadClasses(String moduleFolder, Context context) {
        List<Module> modules = new ArrayList<>();
        List<String> moduleClasses = getModuleClasses(moduleFolder);
        File cacheDir = context.getCacheDir();
        for(int i = 0; i < moduleClasses.size(); i++) {
            try {
                ClassLoader loader = new DexClassLoader(moduleURLS.get(i).toString(), cacheDir.getAbsolutePath(), null, ModuleLoader.class.getClassLoader());
                Class<?> unknownClass = Class.forName(moduleClasses.get(i), true, loader);
                if(Module.class.isAssignableFrom(unknownClass)) {
                    Class<Module> moduleClass = (Class<Module>) unknownClass;
                    Module module = moduleClass.newInstance();
                    modules.add(module);
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return modules;
    }

}
