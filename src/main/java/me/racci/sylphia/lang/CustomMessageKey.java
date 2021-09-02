package me.racci.sylphia.lang;

import java.util.Objects;

public record CustomMessageKey(String path) implements MessageKey {

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

}
