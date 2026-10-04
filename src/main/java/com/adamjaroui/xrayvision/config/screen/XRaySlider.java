package com.adamjaroui.xrayvision.config.screen;

import java.util.function.DoubleConsumer;
import java.util.function.Function;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * A small general purpose slider built on the vanilla {@link AbstractSliderButton}.
 *
 * <p>The slider stores a normalised {@code 0..1} value and maps it onto a {@code [min, max]} range.
 * The mapped value is pushed to {@code setter} while dragging, and the label is produced by
 * {@code label}.
 */
public final class XRaySlider extends AbstractSliderButton {
	private final double min;
	private final double max;
	private final DoubleConsumer setter;
	private final Function<Double, Component> label;

	public XRaySlider(int x, int y, int width, int height, double min, double max, double current,
			DoubleConsumer setter, Function<Double, Component> label) {
		super(x, y, width, height, Component.empty(), normalise(current, min, max));
		this.min = min;
		this.max = max;
		this.setter = setter;
		this.label = label;
		this.updateMessage();
	}

	private static double normalise(double current, double min, double max) {
		if (max <= min) {
			return 0.0D;
		}

		double v = (current - min) / (max - min);
		return v < 0.0D ? 0.0D : (v > 1.0D ? 1.0D : v);
	}

	/**
	 * @return the current value mapped back onto {@code [min, max]}
	 */
	public double currentValue() {
		return this.min + (this.max - this.min) * this.value;
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.label.apply(this.currentValue()));
	}

	@Override
	protected void applyValue() {
		this.setter.accept(this.currentValue());
	}
}
