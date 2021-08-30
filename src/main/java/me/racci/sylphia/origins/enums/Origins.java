package me.racci.sylphia.origins.enums;

import java.util.ArrayList;
import java.util.List;

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
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getPowers() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getPower() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}


}
