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

        var broadcast: Component = "<prefix_server><player> has become the <new_origin> origin!".parse()

        var setSelf: Component = "<prefix_origins>You set your origin to <new_origin>.".parse()

        var setOther: Component = "<prefix_origins>Set <target>'s origin to <new_origin>.".parse()

        var setSameSelf: Component = "<prefix_origins>You are already the <origin> origin!".parse()

        var setSameOther: Component = "<prefix_origins><player> is already the <origin> origin!".parse()

        var getSelf: Component = "<prefix_origins>Your origin is <origin>.".parse()

        var getOther: Component = "<prefix_origins><target>'s origin is <origin>.".parse()

        var nightVision: Component = "<prefix_origins>Your night vision now triggers on: <new_nightvision>.".parse()
    }

}