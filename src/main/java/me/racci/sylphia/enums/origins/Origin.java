package me.racci.sylphia.enums.origins;

import java.util.Locale;

@SuppressWarnings("unused")
public interface Origin {

	//ImmutableList<Supplier<Ability>> getPowers();

	String getDescription(Locale locale);

	String getDisplayName(Locale locale);

	String name();

	String toString();

}
