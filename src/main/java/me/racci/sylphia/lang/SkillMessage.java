package me.racci.sylphia.lang;

import me.racci.sylphia.enums.origins.Origin;
import me.racci.sylphia.enums.origins.Origins;

import java.util.Locale;

public enum SkillMessage implements MessageKey {

    FARMING_NAME,
    FARMING_DESC,
    FORAGING_NAME,
    FORAGING_DESC,
    MINING_NAME,
    MINING_DESC,
    FISHING_NAME,
    FISHING_DESC,
    EXCAVATION_NAME,
    EXCAVATION_DESC,
    ARCHERY_NAME,
    ARCHERY_DESC,
    DEFENSE_NAME,
    DEFENSE_DESC,
    FIGHTING_NAME,
    FIGHTING_DESC,
    ENDURANCE_NAME,
    ENDURANCE_DESC,
    AGILITY_NAME,
    AGILITY_DESC,
    ALCHEMY_NAME,
    ALCHEMY_DESC,
    ENCHANTING_NAME,
    ENCHANTING_DESC,
    SORCERY_NAME,
    SORCERY_DESC,
    HEALING_NAME,
    HEALING_DESC,
    FORGING_NAME,
    FORGING_DESC;

    private final Origin origin = Origins.valueOf(this.name().substring(0, this.name().lastIndexOf("_")));
    private final String path = "skills." + origin.name().toLowerCase(Locale.ENGLISH) + "." + this.name().substring(this.name().lastIndexOf("_") + 1).toLowerCase(Locale.ENGLISH);

    public String getPath() {
        return path;
    }

}
