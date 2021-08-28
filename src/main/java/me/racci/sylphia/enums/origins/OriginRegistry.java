package me.racci.sylphia.enums.origins;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class OriginRegistry {

	public final Map<String, Origin> origins;

	public OriginRegistry() {
		this.origins = new HashMap<>();
	}

	public void register(String key, Origin origin) {
		this.origins.put(key.toLowerCase(), origin);
	}

	public Collection<Origin> getOrigins() {
		return origins.values();
	}

	@Nullable
	public Origin getOrigin(String key) {
		return this.origins.get(key.toLowerCase());
	}

}
