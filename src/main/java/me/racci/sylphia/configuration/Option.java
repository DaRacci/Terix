package me.racci.sylphia.configuration;


public enum Option {

    SOUND_VOLUME("Sound-Volume", OptionType.DOUBLE),
    BLACKLISTED_WORLDS("Blacklisted-Worlds", OptionType.LIST),
    PREVIEW_WORLDS("Preview-Worlds", OptionType.LIST);

    private final String path;
    private final OptionType type;

    Option(String path, OptionType type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public OptionType getType() {
        return type;
    }

}
