package me.racci.sylphia.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record CustomMessageKey(String path) implements MessageKey {

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

}
