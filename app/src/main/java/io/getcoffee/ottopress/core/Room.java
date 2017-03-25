package io.getcoffee.ottopress.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import java.util.Collection;

import io.getcoffee.ottopress.ClientApplication;

public class Room {

    public static final String CORE_PREFS_ROOM_TYPES = "room_types";

    public RoomType core;
    public String modifier;

    public Room() {

    }

}
