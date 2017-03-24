package io.getcoffee.ottopress.module;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.getcoffee.ottopress.core.Device;

public abstract class Module {

    public abstract void onInitialize();
    public abstract void onClose();

    public abstract Drawable getMenuIcon();
    public abstract Fragment buildFragment(Device device);

    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Ottomodule {
        String author();
        String device();
        String version();
    }
}
