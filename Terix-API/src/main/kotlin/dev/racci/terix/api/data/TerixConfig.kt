package dev.racci.terix.api.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.MinixConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.terix.api.Terix
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Required
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@ConfigSerializable
@MappedConfig(Terix::class, "Config.conf")
public class TerixConfig : MinixConfig<Terix>(true) {

    @Comment("The players default origin.")
    public val defaultOrigin: String = "Human"

    @Comment("Should the player see a title when changing origins?")
    public val showTitleOnChange: Boolean = true

    @Comment("How long does the player have to wait between changing their origin again?")
    public val intervalBeforeChange: Duration = 360.minutes

    @Comment("How many times should the player be able to change their origin for free?")
    public val freeChanges: Int = 3

    public val gui: GUI = GUI()

    @ConfigSerializable
    public data class GUI(
        val remainingChanges: GUIItemSlot = GUIItemSlot(
            "itemsadder:elixirmc__opal",
            "-1;1"
        ),
        val info: GUIItemSlot = GUIItemSlot(
            "itemsadder:mcicons__icon_plus name:\"<white>Info\"",
            "-1;5",
            listOf(
                PartialComponent.of("<i:false><aqua>Click on an origin to select it."),
                PartialComponent.of("<i:false><aqua>Click on the same origin to deselect it."),
                PartialComponent.of("<i:false><aqua>Click on the <green>Confirm</green> button to confirm your selection."),
                PartialComponent.of("<i:false><aqua>Click on the <red>Cancel</red> button to cancel your selection.")
            )
        ),
        val previousPage: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_left_blue", "-1;8"),
        val nextPage: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_right_blue", "-1;9"),
        val cancelSelection: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_cancel name:\"<red>Cancel selection\"", "-1;4"),
        val confirmSelection: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_confirm name:\"<green>Confirm Selection\"", "-1;6")
    ) : InnerConfig by InnerConfig.Default() {

        @ConfigSerializable
        public data class GUIItemSlot(
            @Required val display: String,
            @Required val position: String,
            val lore: List<PartialComponent> = emptyList(),
            val commands: List<String> = emptyList()
        )
    }
}
