/*
 * This file is part of Dawn Config (formerly 'Cloth Config').
 * Copyright (C) 2020 - 2021 shedaniel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.raxdiam.dawn.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class LongSliderEntry extends TooltipListEntry<Long> {
    
    protected Slider sliderWidget;
    protected Button resetButton;
    protected AtomicLong value;
    protected final long orginial;
    private long minimum, maximum;
    private final Supplier<Long> defaultValue;
    private Function<Long, Component> textGetter = value -> Component.literal(String.format("Value: %d", value));
    private final List<AbstractWidget> widgets;
    
    @ApiStatus.Internal
    @Deprecated
    public LongSliderEntry(Component fieldName, long minimum, long maximum, long value, Consumer<Long> saveConsumer, Component resetButtonKey, Supplier<Long> defaultValue) {
        this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, null);
    }
    
    @ApiStatus.Internal
    @Deprecated
    public LongSliderEntry(Component fieldName, long minimum, long maximum, long value, Consumer<Long> saveConsumer, Component resetButtonKey, Supplier<Long> defaultValue, Supplier<Optional<Component[]>> tooltipSupplier) {
        this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, tooltipSupplier, false);
    }
    
    @ApiStatus.Internal
    @Deprecated
    public LongSliderEntry(Component fieldName, long minimum, long maximum, long value, Consumer<Long> saveConsumer, Component resetButtonKey, Supplier<Long> defaultValue, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.orginial = value;
        this.defaultValue = defaultValue;
        this.value = new AtomicLong(value);
        this.saveCallback = saveConsumer;
        this.maximum = maximum;
        this.minimum = minimum;
        this.sliderWidget = new Slider(0, 0, 152, 20, ((double) LongSliderEntry.this.value.get() - minimum) / Math.abs(maximum - minimum));
        this.resetButton = Button.builder(resetButtonKey, widget -> {
            setValue(defaultValue.get());
        }).bounds(0, 0, Minecraft.getInstance().font.width(resetButtonKey) + 6, 20).build();
        this.sliderWidget.setMessage(textGetter.apply(LongSliderEntry.this.value.get()));
        this.widgets = Lists.newArrayList(sliderWidget, resetButton);
    }
    
    public Function<Long, Component> getTextGetter() {
        return textGetter;
    }
    
    public LongSliderEntry setTextGetter(Function<Long, Component> textGetter) {
        this.textGetter = textGetter;
        this.sliderWidget.setMessage(textGetter.apply(LongSliderEntry.this.value.get()));
        return this;
    }
    
    @Override
    public Long getValue() {
        return value.get();
    }
    
    @Deprecated
    public void setValue(long value) {
        sliderWidget.setValue((Mth.clamp(value, minimum, maximum) - minimum) / (double) Math.abs(maximum - minimum));
        this.value.set(Math.min(Math.max(value, minimum), maximum));
        sliderWidget.updateMessage();
    }
    
    @Override
    public Optional<Long> getDefaultValue() {
        return defaultValue == null ? Optional.empty() : Optional.ofNullable(defaultValue.get());
    }
    
    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }
    
    @Override
    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }
    
    @Override
    public boolean isEdited() {
        return super.isEdited() || getValue() != orginial;
    }
    
    public LongSliderEntry setMaximum(long maximum) {
        this.maximum = maximum;
        return this;
    }
    
    public LongSliderEntry setMinimum(long minimum) {
        this.minimum = minimum;
        return this;
    }
    
    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = Minecraft.getInstance().getWindow();
        this.resetButton.active = isEditable() && getDefaultValue().isPresent() && defaultValue.get() != value.get();
        this.resetButton.setY(y);
        this.sliderWidget.active = isEditable();
        this.sliderWidget.setY(y);
        Component displayedFieldName = getDisplayedFieldName();
        if (Minecraft.getInstance().font.isBidirectional()) {
            graphics.drawString(Minecraft.getInstance().font, displayedFieldName.getVisualOrderText(), window.getGuiScaledWidth() - x - Minecraft.getInstance().font.width(displayedFieldName), y + 6, getPreferredTextColor());
            this.resetButton.setX(x);
            this.sliderWidget.setX(x + resetButton.getWidth() + 1);
        } else {
            graphics.drawString(Minecraft.getInstance().font, displayedFieldName.getVisualOrderText(), x, y + 6, getPreferredTextColor());
            this.resetButton.setX(x + entryWidth - resetButton.getWidth());
            this.sliderWidget.setX(x + entryWidth - 150);
        }
        this.sliderWidget.setWidth(150 - resetButton.getWidth() - 2);
        resetButton.render(graphics, mouseX, mouseY, delta);
        sliderWidget.render(graphics, mouseX, mouseY, delta);
    }
    
    private class Slider extends AbstractSliderButton {
        protected Slider(int int_1, int int_2, int int_3, int int_4, double double_1) {
            super(int_1, int_2, int_3, int_4, Component.empty(), double_1);
        }
        
        @Override
        public void updateMessage() {
            setMessage(textGetter.apply(LongSliderEntry.this.value.get()));
        }
        
        @Override
        protected void applyValue() {
            LongSliderEntry.this.value.set((long) (minimum + Math.abs(maximum - minimum) * value));
        }
        
        @Override
        public boolean keyPressed(int int_1, int int_2, int int_3) {
            if (!isEditable())
                return false;
            return super.keyPressed(int_1, int_2, int_3);
        }
        
        @Override
        public boolean mouseDragged(double double_1, double double_2, int int_1, double double_3, double double_4) {
            if (!isEditable())
                return false;
            return super.mouseDragged(double_1, double_2, int_1, double_3, double_4);
        }
        
        public double getValue() {
            return value;
        }
        
        public void setValue(double integer) {
            this.value = integer;
        }
    }
    
}
