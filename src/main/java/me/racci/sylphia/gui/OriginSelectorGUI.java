package me.racci.sylphia.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.racci.sylphia.lang.GUI;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class OriginSelectorGUI {

	private static final GuiItem orangeFiller = ItemBuilder.from(Material.ORANGE_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem();
	private static final GuiItem yellowFiller = ItemBuilder.from(Material.YELLOW_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem();
	private static final Gui selectionGUI = Gui.gui()
			.title(TextUtil.parseLegacy(Lang.getMessage(GUI.SELECTION_TITLE)))
			.rows(6)
			.disableAllInteractions()
			.create();
	private static final Gui confirmGUI = Gui.gui()
			.title(TextUtil.parseLegacy(Lang.getMessage(GUI.SELECTION_CONFIRM_TITLE)))
			.rows(1)
			.disableAllInteractions()
			.create();
	private static final Gui singleCustomSelectionGUI = Gui.gui()
			.title(TextUtil.parseLegacy(Lang.getMessage(GUI.SELECTION_CUSTOM_TITLE)))
			.type(GuiType.BREWING)
			.disableAllInteractions()
			.create();
	private static final Gui multiCustomSelectionGUI = Gui.gui()
			.title(TextUtil.parseLegacy(Lang.getMessage(GUI.SELECTION_CUSTOM_TITLE)))
			.rows(2)
			.disableAllInteractions()
			.create();

	public OriginSelectorGUI() {
		init();
	}

	private void init() {
		selectionGUI.getFiller().fillBorder(Arrays.asList(orangeFiller, yellowFiller));
		selectionGUI.addSlotAction(49, event -> event.getWhoClicked().closeInventory());
		selectionGUI.addSlotAction(4, event -> {
			Player player = (Player) event.getWhoClicked();
			for(String permission : )
			multiCustomSelectionGUI.open(player)
		});

	}




}
