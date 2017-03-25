package io.getcoffee.ottopress.module;

import java.io.File;
import java.io.FileFilter;

class JarFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        return pathname.toString().endsWith(".jar") || pathname.toString().endsWith(".apk");
    }
}
