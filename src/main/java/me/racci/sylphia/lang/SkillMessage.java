package me.racci.sylphia.lang;

import me.racci.sylphia.enums.origins.Origin;
import me.racci.sylphia.enums.origins.Origins;

import java.util.Locale;

public enum SkillMessage implements MessageKey {

    ;

    private final Origin origin = Origins.valueOf(this.name().substring(0, this.name().lastIndexOf("_")));
    private final String path = "skills." + origin.name().toLowerCase(Locale.ENGLISH) + "." + this.name().substring(this.name().lastIndexOf("_") + 1).toLowerCase(Locale.ENGLISH);

    public String getPath() {
        return path;
    }

}
