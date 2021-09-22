package me.racci.sylphia.events


class DataLoadEvent(val playerData: me.racci.sylphia.data.PlayerData) : me.racci.raccilib.events.KotlinEvent(true) { }