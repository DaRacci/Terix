package me.racci.sylphia.enums.origins;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public enum Origins implements Origin {

	//create dynamic adding for the loaded origins
	HUMAN,
	RACCOON;

	//create a dynamic list for the loaded origins
	public static List<Origins> getOrderedValues() {
		List<Origins> list = new ArrayList<>();
		list.add(Origins.HUMAN);
		list.add(Origins.RACCOON);
		return list;
	}

	@Override
	public String getDescription(Locale locale) {
		return null;
	}

	@Override
	public String getDisplayName(Locale locale) {
		return null;
	}
}
