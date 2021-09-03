//package me.racci.sylphia.utils.items;
//
//
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.Skull;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.SkullMeta;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.Base64;
//import java.util.UUID;
//
//
//@SuppressWarnings("unused")
//public class SkullCreator {
//
//	private SkullCreator() {}
//
//	private static final boolean warningPosted = false;
//
//	private static Field blockProfileField;
//	private static Method metaSetProfileMethod;
//	private static Field metaProfileField;
//
//	public static ItemStack createSkull() {
//		return new ItemStack(Material.valueOf("PLAYER_HEAD"));
//	}
//
//	public static ItemStack itemFromUuid(UUID id) {
//		return itemWithUuid(createSkull(), id);
//	}
//
//	public static ItemStack itemFromBase64(String base64) {
//		return itemWithBase64(createSkull(), base64);
//	}
//
//	public static ItemStack itemWithUuid(ItemStack item, UUID id) {
//		notNull(item, "item");
//		notNull(id, "id");
//
//		SkullMeta meta = (SkullMeta) item.getItemMeta();
//		meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
//		item.setItemMeta(meta);
//
//		return item;
//	}
//
//	public static ItemStack itemWithBase64(ItemStack item, String base64) {
//		notNull(item, "item");
//		notNull(base64, "base64");
//
//		if (!(item.getItemMeta() instanceof SkullMeta meta)) {
//			return null;
//		}
//		mutateItemMeta(meta, base64);
//		item.setItemMeta(meta);
//
//		return item;
//	}
//
//	public static void blockWithUuid(Block block, UUID id) {
//		notNull(block, "block");
//		notNull(id, "id");
//
//		setToSkull(block);
//		Skull state = (Skull) block.getState();
//		state.setOwningPlayer(Bukkit.getOfflinePlayer(id));
//		state.update(false, false);
//	}
//
//	public static void blockWithBase64(Block block, String base64) {
//		notNull(block, "block");
//		notNull(base64, "base64");
//
//		setToSkull(block);
//		Skull state = (Skull) block.getState();
//		mutateBlockState(state, base64);
//		state.update(false, false);
//	}
//
//	private static void setToSkull(Block block) {
//		block.setType(Material.valueOf("PLAYER_HEAD"), false);
//	}
//
//	private static void notNull(Object o, String name) {
//		if (o == null) {
//			throw new NullPointerException(name + " should not be null!");
//		}
//	}
//
//	private static String urlToBase64(String url) {
//
//		URI actualUrl;
//		try {
//			actualUrl = new URI(url);
//		} catch (URISyntaxException e) {
//			throw new RuntimeException(e);
//		}
//		String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
//		return Base64.getEncoder().encodeToString(toEncode.getBytes());
//	}
//
//}
