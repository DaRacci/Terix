package me.racci.sylphia.gui;

public enum MenuType {

	SELECTOR("GUI.Selector."),
	CUSTOM("GUI.Custom."),
	CONFIRM("GUI.Confirm."),
	INFO("GUI.Info.");

	private final String path;

	MenuType(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

}
