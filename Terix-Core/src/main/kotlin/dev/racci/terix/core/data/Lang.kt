package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.LangConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.terix.api.Terix
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
@MappedConfig(Terix::class, "Lang.conf")
class Lang : LangConfig<Terix>() {

    override val prefixes: Map<String, String> = mapOf(
        "server" to "<gradient:#ED13D9:#12d3ff>Elixir</gradient> <white>»</white> <aqua>",
        "terix" to "<gradient:#ED13D9:#12d3ff>Terix</gradient> <white>»</white> <aqua>",
        "origins" to "<gold>Origins</gold> » <aqua>"
    )

    var generic: Generic = Generic()

    var origin: Origin = Origin()

    var choices: Choices = Choices()

    @ConfigSerializable
    class Generic : InnerLang() {

        var error: PartialComponent = PartialComponent.of("<dark_red>Error <white>» <red><message>")

        var reloadLang: PartialComponent = PartialComponent.of("<prefix:terix>Reloaded plugin in <time>ms. (Does nothing currently)")
    }

    @ConfigSerializable
    class Origin : InnerLang() {

        var broadcast: PartialComponent = PartialComponent.of("<prefix:server><player> has become the <new_origin> origin!")

        var setSelf: PartialComponent = PartialComponent.of("<prefix:origins>You set your origin to <new_origin>.")

        var setOther: PartialComponent = PartialComponent.of("<prefix:origins>Set <player>'s origin to <new_origin>.")

        var setSameSelf: PartialComponent = PartialComponent.of("<prefix:origins>You are already the <origin> origin!")

        var setSameOther: PartialComponent = PartialComponent.of("<prefix:origins><player> is already the <origin> origin!")

        var getSelf: PartialComponent = PartialComponent.of("<prefix:origins>Your origin is <origin>.")

        var getOther: PartialComponent = PartialComponent.of("<prefix:origins><player>'s origin is <origin>.")

        var nightVision: PartialComponent = PartialComponent.of("<prefix:origins>Your night vision now triggers on: <new_nightvision>.")

        var onChangeCooldown: PartialComponent = PartialComponent.of("<prefix:origins>You can't change your origin for another <cooldown>.")

        var missingRequirement: PartialComponent = PartialComponent.of("<prefix:origins>You're missing a requirement to select this origin.")

        var bee: Bee = Bee()

        var descriptor: Descriptor = Descriptor()

        @ConfigSerializable
        class Bee : InnerLang() {
            var potion: PartialComponent = PartialComponent.of("<prefix:origins>The potion is too strong for you, try a flower instead.")
        }

        @ConfigSerializable
        class Descriptor : InnerLang() {
            var head: PartialComponent = PartialComponent.of("<prefix:origins> Information about <origin>:<category>")

            var bodyLine: PartialComponent = PartialComponent.of("    <aqua><key> <white>»</white> <value></aqua>")

            var footer: PartialComponent = PartialComponent.of("<prefix:origins>End of information.")
        }
    }

    @ConfigSerializable
    class Choices : InnerLang() {
        var getSelf = PartialComponent.of("<prefix:origins>You have <choices> choices.")
        var getOther = PartialComponent.of("<prefix:origins><player> has <choices> choices.")

        var mutateSelf = PartialComponent.of("<prefix:origins>You now have <choices> choices.")
        var mutateOther = PartialComponent.of("<prefix:origins><player> now has <choices> choices.")
    }
}
