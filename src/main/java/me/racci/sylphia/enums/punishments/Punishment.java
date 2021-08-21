package me.racci.sylphia.enums.punishments;

import java.util.Locale;

@SuppressWarnings("unused")
public interface Punishment {

    String getDescription(Locale locale);

    String getDisplayName(Locale locale);

    String name();

    String toString();

}
