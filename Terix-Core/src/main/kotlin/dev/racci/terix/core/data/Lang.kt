package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.LangConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.terix.api.Terix
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
@MappedConfig(Terix::class, "Lang.conf")
public class Lang : LangConfig<Terix>() {

    override val prefixes: Map<String, String> = mapOf(
        "server" to "<gradient:#ED13D9:#12d3ff>Elixir</gradient> <white>»</white> <aqua>",
        "terix" to "<gradient:#ED13D9:#12d3ff>Terix</gradient> <white>»</white> <aqua>",
        "origins" to "<gold>Origins</gold> » <aqua>"
    )

    public var generic: Generic = Generic()

    public var origin: Origin = Origin()

    public var choices: Choices = Choices()

    public var gui: GUI = GUI()

    @ConfigSerializable
    public class Generic : InnerLang() {

        public var error: PartialComponent = PartialComponent.of("<dark_red>Error <white>» <red><message>")

        public var reloadLang: PartialComponent = PartialComponent.of("<prefix:terix>Reloaded plugin in <time>ms. (Does nothing currently)")
    }

    @ConfigSerializable
    public class Origin : InnerLang() {

        public var broadcast: PartialComponent = PartialComponent.of("<prefix:server><player> has become the <new_origin> origin!")

        public var setSelf: PartialComponent = PartialComponent.of("<prefix:origins>You set your origin to <new_origin>.")

        public var setOther: PartialComponent = PartialComponent.of("<prefix:origins>Set <player>'s origin to <new_origin>.")

        public var setSameSelf: PartialComponent = PartialComponent.of("<prefix:origins>You are already the <origin> origin!")

        public var setSameOther: PartialComponent = PartialComponent.of("<prefix:origins><player> is already the <origin> origin!")

        public var getSelf: PartialComponent = PartialComponent.of("<prefix:origins>Your origin is <origin>.")

        public var getOther: PartialComponent = PartialComponent.of("<prefix:origins><player>'s origin is <origin>.")

        public var nightVision: PartialComponent = PartialComponent.of("<prefix:origins>Your night vision now triggers on: <new_nightvision>.")

        public var onChangeCooldown: PartialComponent = PartialComponent.of("<prefix:origins>You can't change your origin for another <cooldown>.")

        public var missingRequirement: PartialComponent = PartialComponent.of("<prefix:origins>You're missing a requirement to select this origin.")

        public var cancelledCommand: PartialComponent = PartialComponent.of("<prefix:origins>Couldn't change origin, reason: <reason>.")

        public val remainingChanges: PartialComponent = PartialComponent.of("<prefix:origins>You have <amount> changes remaining.")

        public var bee: Bee = Bee()

        public var descriptor: Descriptor = Descriptor()

        @ConfigSerializable
        public class Bee : InnerLang() {
            public var potion: PartialComponent = PartialComponent.of("<prefix:origins>The potion is too strong for you, try a flower instead.")
        }

        @ConfigSerializable
        public class Descriptor : InnerLang() {
            public var head: PartialComponent = PartialComponent.of("<prefix:origins> Information about <origin>:<category>")

            public var bodyLine: PartialComponent = PartialComponent.of("    <aqua><key> <white>»</white> <value></aqua>")

            public var footer: PartialComponent = PartialComponent.of("<prefix:origins>End of information.")
        }
    }

    @ConfigSerializable
    public class Choices : InnerLang() {
        public var getSelf: PartialComponent = PartialComponent.of("<prefix:origins>You have <choices> choices.")
        public var getOther: PartialComponent = PartialComponent.of("<prefix:origins><player> has <choices> choices.")

        public var mutateSelf: PartialComponent = PartialComponent.of("<prefix:origins>You now have <choices> choices.")
        public var mutateOther: PartialComponent = PartialComponent.of("<prefix:origins><player> now has <choices> choices.")
    }

    @ConfigSerializable
    public data class GUI(
        val title: PartialComponent = PartialComponent.of("<prefix:origins>Origins"),
        val requirementLine: PartialComponent = PartialComponent.of("|-    <requirement>"),
        val requirementLore: List<PartialComponent> = listOf(
            PartialComponent.of(""),
            PartialComponent.of("Requirements:")
        )
    ) : InnerLang()
}
