package me.racci.sylphia.origins.enums.specials;

import java.util.ArrayList;
import java.util.List;

public enum Specials implements Special {

	SLOWFALLING,
	NIGHTVISION,
	JUMPBOOST;

	@Override
	public String getDescription() {
//		return Lang.getMessage(this.valueOf(this.name() + "_DESC"));
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
//		return Lang.getMessage(Specials.valueOf(this.name().toUpperCase() + "_NAME"));
	}

	public static List<Specials> getOrderedValues() {
		List<Specials> list = new ArrayList<>();
		list.add(Specials.NIGHTVISION);
		list.add(Specials.SLOWFALLING);
		list.add(Specials.JUMPBOOST);
		return list;
	}
}
