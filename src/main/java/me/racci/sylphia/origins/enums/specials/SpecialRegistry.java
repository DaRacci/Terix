package me.racci.sylphia.origins.enums.specials;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class SpecialRegistry {

	public final Map<String, Special> originSettings;

	public SpecialRegistry() {
		this.originSettings = new HashMap<>();
	}

	public void register(String key, Special originSetting) {
		this.originSettings.put(key.toLowerCase(), originSetting);
	}

	public Collection<Special> getOriginSettings() {
		return originSettings.values();
	}

	@Nullable
	public Special getOriginSetting(String key) {
		return this.originSettings.get(key.toLowerCase(Locale.ROOT));
	}
}
