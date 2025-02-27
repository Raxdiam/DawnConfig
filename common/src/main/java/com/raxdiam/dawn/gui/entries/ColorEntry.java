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

import com.raxdiam.dawn.gui.widget.ColorDisplayWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorEntry extends TextFieldListEntry<Integer> {
    
    private final ColorDisplayWidget colorDisplayWidget;
    private boolean alpha;
    
    @ApiStatus.Internal
    @Deprecated
    public ColorEntry(Component fieldName, int value, Component resetButtonKey, Supplier<Integer> defaultValue, Consumer<Integer> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
        super(fieldName, 0, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.alpha = true;
        ColorValue colorValue = getColorValue(String.valueOf(value));
        if (colorValue.hasError())
            throw new IllegalArgumentException("Invalid Color: " + colorValue.getError().name());
        this.alpha = false;
        this.saveCallback = saveConsumer;
        this.original = value;
        this.textFieldWidget.setValue(getHexColorString(value));
        this.colorDisplayWidget = new ColorDisplayWidget(textFieldWidget, 0, 0, 20, getColorValueColor(textFieldWidget.getValue()));
        this.resetButton.onPress = button -> {
            this.textFieldWidget.setValue(getHexColorString(defaultValue.get()));
        };
    }
    
    @Override
    protected boolean isChanged(Integer original, String s) {
        ColorValue colorValue = getColorValue(s);
        return colorValue.hasError() || this.original != colorValue.color;
    }
    
    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        this.colorDisplayWidget.setY(y);
        ColorValue value = getColorValue(textFieldWidget.getValue());
        if (!value.hasError())
            colorDisplayWidget.setColor(alpha ? value.getColor() : 0xff000000 | value.getColor());
        if (Minecraft.getInstance().font.isBidirectional()) {
            this.colorDisplayWidget.setX(x + resetButton.getWidth() + textFieldWidget.getWidth());
        } else {
            this.colorDisplayWidget.setX(textFieldWidget.getX() - 23);
        }
        colorDisplayWidget.render(graphics, mouseX, mouseY, delta);
    }
    
    @Override
    protected boolean isMatchDefault(String text) {
        if (!getDefaultValue().isPresent())
            return false;
        ColorValue colorValue = getColorValue(text);
        return !colorValue.hasError() && colorValue.color == getDefaultValue().get();
    }
    
    @Override
    public boolean isEdited() {
        ColorValue colorValue = getColorValue(textFieldWidget.getValue());
        return colorValue.hasError() || colorValue.color != original;
    }
    
    @Override
    public Integer getValue() {
        return getColorValueColor(textFieldWidget.getValue());
    }
    
    @Deprecated
    public void setValue(int color) {
        textFieldWidget.setValue(getHexColorString(color));
    }
    
    @Override
    public Optional<Component> getError() {
        ColorValue colorValue = getColorValue(this.textFieldWidget.getValue());
        if (colorValue.hasError())
            return Optional.of(Component.translatable("text.dawn-config.error.color." + colorValue.getError().name().toLowerCase(Locale.ROOT)));
        return super.getError();
    }
    
    public void withAlpha() {
        if (!alpha) {
            this.alpha = true;
            textFieldWidget.setValue(getHexColorString(original));
        }
    }
    
    public void withoutAlpha() {
        if (alpha) {
            alpha = false;
            textFieldWidget.setValue(getHexColorString(original));
        }
    }
    
    protected String stripHexStarter(String hex) {
        if (hex.startsWith("#")) {
            return hex.substring(1);
        } else return hex;
    }
    
    protected boolean isValidColorString(String str) {
        return !getColorValue(str).hasError();
    }
    
    protected int getColorValueColor(String str) {
        return getColorValue(str).getColor();
    }
    
    protected ColorValue getColorValue(String str) {
        try {
            int color;
            if (str.startsWith("#")) {
                String stripHexStarter = stripHexStarter(str);
                if (stripHexStarter.length() > 8) return ColorError.INVALID_COLOR.toValue();
                if (!alpha && stripHexStarter.length() > 6) return ColorError.NO_ALPHA_ALLOWED.toValue();
                color = (int) Long.parseLong(stripHexStarter, 16);
            } else {
                color = (int) Long.parseLong(str);
            }
            int a = color >> 24 & 0xFF;
            if (!alpha && a > 0)
                return ColorError.NO_ALPHA_ALLOWED.toValue();
            if (a < 0 || a > 255)
                return ColorError.INVALID_ALPHA.toValue();
            int r = color >> 16 & 0xFF;
            if (r < 0 || r > 255)
                return ColorError.INVALID_RED.toValue();
            int g = color >> 8 & 0xFF;
            if (g < 0 || g > 255)
                return ColorError.INVALID_GREEN.toValue();
            int b = color & 0xFF;
            if (b < 0 || b > 255)
                return ColorError.INVALID_BLUE.toValue();
            return new ColorValue(color);
        } catch (NumberFormatException e) {
            return ColorError.INVALID_COLOR.toValue();
        }
    }
    
    protected String getHexColorString(int color) {
        return "#" + StringUtils.leftPad(Integer.toHexString(color), alpha ? 8 : 6, '0');
    }
    
    protected enum ColorError {
        NO_ALPHA_ALLOWED,
        INVALID_ALPHA,
        INVALID_RED,
        INVALID_GREEN,
        INVALID_BLUE,
        INVALID_COLOR;
        
        private final ColorValue value;
        
        ColorError() {
            this.value = new ColorValue(this);
        }
        
        public ColorValue toValue() {
            return value;
        }
    }
    
    protected static class ColorValue {
        private int color = -1;
        @Nullable
        private ColorError error = null;
        
        public ColorValue(int color) {
            this.color = color;
        }
        
        public ColorValue(ColorError error) {
            this.error = error;
        }
        
        public int getColor() {
            return color;
        }
        
        @Nullable
        public ColorError getError() {
            return error;
        }
        
        public boolean hasError() {
            return getError() != null;
        }
    }
}
