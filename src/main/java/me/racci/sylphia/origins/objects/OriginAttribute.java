package me.racci.sylphia.origins.objects;

import org.bukkit.attribute.Attribute;

public class OriginAttribute {
	private Attribute attribute;
	private double value;

	public OriginAttribute(Attribute attribute, double value) {
		this.attribute = attribute;
		this.value = value;
	}

	public Attribute getAttribute() {
		return this.attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public double getValue() {
		return this.value;
	}

	public void setValue(double value) {
		this.value = value;
	}

}
