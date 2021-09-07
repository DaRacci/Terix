package me.racci.sylphia.lang;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface MessageKey {

    String getPath();

    static Set<MessageKey> values() {
        Set<MessageKey> keys = new HashSet<>();
        keys.addAll(Arrays.asList(Command.values()));
        keys.addAll(Arrays.asList(GUI.values()));
        keys.addAll(Arrays.asList(Prefix.values()));
        keys.addAll(Arrays.asList(Origins.values()));
        keys.addAll(Arrays.asList(Colours.values()));
        return keys;
    }
}
