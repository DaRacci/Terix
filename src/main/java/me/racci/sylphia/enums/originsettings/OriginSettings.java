package me.racci.sylphia.enums.originsettings;

import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.SkillMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public enum OriginSettings implements OriginSetting {

	SLOWFALLING,
	NIGHTVISION,
	JUMPBOOST;

	@Override
	public String getDescription(Locale locale) {
		return Lang.getMessage(SkillMessage.valueOf(this.name() + "_DESC"));
	}

	@Override
	public String getDisplayName(Locale locale) {
		return Lang.getMessage(SkillMessage.valueOf(this.name().toUpperCase() + "_NAME"));
	}

	public static List<OriginSettings> getOrderedValues() {
		List<OriginSettings> list = new ArrayList<>();
		list.add(OriginSettings.NIGHTVISION);
		list.add(OriginSettings.SLOWFALLING);
		list.add(OriginSettings.JUMPBOOST);
		return list;
	}
}
