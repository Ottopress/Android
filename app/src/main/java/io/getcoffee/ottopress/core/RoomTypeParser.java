package io.getcoffee.ottopress.core;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RoomTypeParser {

    private static final String ns = null;
    private Context context;

    public RoomTypeParser(Context context) {
        this.context = context;
    }

    public Map<String, RoomType> parse(XmlPullParser parser) {
        try {
            parser.next();
            parser.next();
            return read(parser);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, RoomType> read(XmlPullParser parser) throws IOException, XmlPullParserException {
        Map<String, RoomType> roomTypes = new HashMap<>();
        parser.require(XmlPullParser.START_TAG, ns, "types");
        while(parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if(name.equals("type")) {
                RoomType roomType = readRoomType(parser);
                roomTypes.put(roomType.name, roomType);
            } else {
                skip(parser);
            }
        }
        return roomTypes;
    }

    public RoomType readRoomType(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "type");
        String name = null;
        Drawable icon = null;
        while(parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            switch(tagName) {
                case "name":
                    name = readTag(parser, tagName);
                    break;
                case "icon":
                    //icon = readIcon(parser);
                    break;
                default:
                    skip(parser);
            }
        }
        return new RoomType(name, icon);
    }

    private Drawable readIcon(XmlPullParser parser) throws IOException, XmlPullParserException {
        String iconName = readTag(parser, "icon");
        int iconResID = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        return ContextCompat.getDrawable(context, iconResID);
    }

    private String readTag(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String content = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return content;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if(parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
