package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage

@MappedConfig(Terix::class, "Lang.conf")
@ConfigSerializable
class Lang {


    var prefixes: PersistentMap<String, Component> = persistentHashMapOf(
        "terix" to "<light_purple>Terix</light_purple> » <aqua>".parse(),
        "server" to "<light_purple>Elixir</light_purple> » <aqua>".parse(),
        "origins" to "<gold>Origins</gold> » <aqua>".parse(),
    )

    var generic: Generic = Generic()

    var origins: Origins by Origins()



    class Generic {

        var error: Component = "<dark_red>Error <white>» <red><message>".parse()

        var reloadLang: Component = "<prefix_terix>Reloaded Language file.".parse()
    }

    class Origins {

        var broadcast: Component = "".parse()

        var setSelf: Component = "".parse()

        var setOther: Component = "".parse()

        var setSameSelf: Component = "".parse()

        var setSameOther: Component = "".parse()

        var getSelf: Component = "".parse()

        var getOther: Component = "".parse()

        var nightVision: Component = "".parse()
    }

}