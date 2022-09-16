package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.LangConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.terix.api.Terix
import org.spongepowered.configurate.objectmapping.ConfigSerializable

// TODO -> Fix elixir gradient and make bold
@ConfigSerializable
@MappedConfig(Terix::class, "Lang.conf")
class Lang : LangConfig<Terix>() {

    override val prefixes: Map<String, String> = mapOf(
        "<prefix_terix>" to "<light_purple>Terix</light_purple> » <aqua>",
        "<prefix_server>" to "<light_purple>Elixir</light_purple> » <aqua>",
        "<prefix_origins>" to "<gold>Origins</gold> » <aqua>"
    )

    var generic: Generic = Generic()

    var origin: Origin = Origin()

    var choices: Choices = Choices()

    @ConfigSerializable
    class Generic : InnerLang() {

        var error: PartialComponent = PartialComponent.of("<dark_red>Error <white>» <red><message>")

        var reloadLang: PartialComponent = PartialComponent.of("<prefix_terix>Reloaded plugin in <time>ms. (Does nothing currently)")
    }

    @ConfigSerializable
    class Origin : InnerLang() {

        var broadcast: PartialComponent = PartialComponent.of("<prefix_server><player> has become the <new_origin> origin!")

        var setSelf: PartialComponent = PartialComponent.of("<prefix_origins>You set your origin to <new_origin>.")

        var setOther: PartialComponent = PartialComponent.of("<prefix_origins>Set <player>'s origin to <new_origin>.")

        var setSameSelf: PartialComponent = PartialComponent.of("<prefix_origins>You are already the <origin> origin!")

        var setSameOther: PartialComponent = PartialComponent.of("<prefix_origins><player> is already the <origin> origin!")

        var getSelf: PartialComponent = PartialComponent.of("<prefix_origins>Your origin is <origin>.")

        var getOther: PartialComponent = PartialComponent.of("<prefix_origins><player>'s origin is <origin>.")

        var nightVision: PartialComponent = PartialComponent.of("<prefix_origins>Your night vision now triggers on: <new_nightvision>.")

        var onChangeCooldown: PartialComponent = PartialComponent.of("<prefix_origins>You can't change your origin for another <cooldown>.")

        var missingRequirement: PartialComponent = PartialComponent.of("<prefix_origins>You're missing a requirement to select this origin.")

        var bee: Bee = Bee()

        @ConfigSerializable
        class Bee : InnerLang() {
            var potion: PartialComponent = PartialComponent.of("<prefix_origins>The potion is too strong for you, try a flower instead.")
        }
    }

    @ConfigSerializable
    class Choices : InnerLang() {
        var getSelf = PartialComponent.of("<prefix_origins>You have <choices> choices.")
        var getOther = PartialComponent.of("<prefix_origins><player> has <choices> choices.")

        var mutateSelf = PartialComponent.of("<prefix_origins>You now have <choices> choices.")
        var mutateOther = PartialComponent.of("<prefix_origins><player> now has <choices> choices.")
    }
}
