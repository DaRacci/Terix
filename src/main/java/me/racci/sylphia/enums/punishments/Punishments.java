package me.racci.sylphia.enums.punishments;

import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.SkillMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum Punishments implements Punishment {

	BANS,
	KICKS,
	MUTES,
	WARNS;

	@Override
	public String getDescription(Locale locale) {
		return Lang.getMessage(SkillMessage.valueOf(this.name() + "_DESC"));
	}

	@Override
	public String getDisplayName(Locale locale) {
		return Lang.getMessage(SkillMessage.valueOf(this.name().toUpperCase() + "_NAME"));
	}

	public static List<Punishments> getOrderedValues() {
		List<Punishments> list = new ArrayList<>();
		list.add(Punishments.BANS);
		list.add(Punishments.KICKS);
		list.add(Punishments.MUTES);
		list.add(Punishments.WARNS);
		return list;
	}
	
}