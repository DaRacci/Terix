package me.racci.sylphia.enums.originsettings;

import java.util.Locale;

@SuppressWarnings("unused")
public interface OriginSetting {

	String getDescription(Locale locale);

	String getDisplayName(Locale locale);

	String name();

	String toString();

}
