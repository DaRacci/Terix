package me.racci.sylphia.configuration;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class OptionValue {

    private Object value;

    public OptionValue(Object value) {
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
        if (!(value instanceof Integer var1x)) {
            return (double) value;
        }
        else {
            return var1x.doubleValue();
        }
    }

    public boolean asBoolean() {
        return (boolean) value;
    }

    public String asString() {
        if (value instanceof String string) {
            return string;
        }
        else {
            return String.valueOf(value);
        }
    }

    public List<String> asList() {
        List<String> stringList = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object obj : (List<?>) value) {
                if (obj instanceof String string) {
                    stringList.add(string);
                }
            }
        }
        return stringList;
    }
}
