package me.racci.sylphia.enums;

import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class PathValue {

    private Object value;

    public PathValue(Object value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public int asInt() {
        return (int) value;
    }

    public double asDouble() {
        if(value instanceof Double doubleValue) {
            return doubleValue;
        } else {
            return ((Integer) value).doubleValue();
        }
    }

    public boolean asBoolean() {
        return (boolean) value;
    }

    public String asString() {
        if(value instanceof String string) {
            return string;
        } else {
            return String.valueOf(value);
        }
    }

    public List<String> asList() {
        List<String> stringList = new ArrayList<>();
        if(value instanceof List<?> list) {
            for(Object obj : list) {
                if(obj instanceof String string) {
                    stringList.add(string);
                }
            }
        }
        return stringList;
    }

    public Sound asSound() {
        if (value instanceof Sound sound) {
            return sound;
        } else {
            return null;
        }
    }

//    public PotionEffect asPotionEffect() {
//
//    }
}
