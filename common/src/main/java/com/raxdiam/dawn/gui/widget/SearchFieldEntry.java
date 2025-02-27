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

package com.raxdiam.dawn.gui.widget;

import com.google.common.collect.Iterators;
import com.raxdiam.dawn.api.AbstractConfigEntry;
import com.raxdiam.dawn.api.AbstractConfigListEntry;
import com.raxdiam.dawn.api.ConfigScreen;
import com.raxdiam.dawn.gui.DawnConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;

public class SearchFieldEntry extends AbstractConfigListEntry<Object> {
    private final EditBox editBox;
    private String[] lowerCases;
    
    public SearchFieldEntry(ConfigScreen screen, DawnConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> listWidget) {
        super(Component.empty(), false);
        this.editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 18, Component.empty());
        this.lowerCases = editBox.getValue().isEmpty() ? new String[0] : editBox.getValue().toLowerCase(Locale.ROOT).split(" ");
        this.editBox.setResponder(s -> {
            lowerCases = s.isEmpty() ? new String[0] : s.toLowerCase(Locale.ROOT).split(" ");
        });
        listWidget.entriesTransformer = entries -> {
            return new AbstractList<AbstractConfigEntry<AbstractConfigEntry<?>>>() {
                @Override
                public Iterator<AbstractConfigEntry<AbstractConfigEntry<?>>> iterator() {
                    if (editBox.getValue().isEmpty())
                        return entries.iterator();
                    return Iterators.filter(entries.iterator(), entry -> {
                        return entry.isDisplayed() && screen.matchesSearch(entry.getSearchTags());
                    });
                }
                
                @Override
                public AbstractConfigEntry<AbstractConfigEntry<?>> get(int index) {
                    return Iterators.get(iterator(), index);
                }
                
                @Override
                public void add(int index, AbstractConfigEntry<AbstractConfigEntry<?>> element) {
                    entries.add(index, element);
                }
                
                @Override
                public AbstractConfigEntry<AbstractConfigEntry<?>> remove(int index) {
                    AbstractConfigEntry<AbstractConfigEntry<?>> entry = get(index);
                    return entries.remove(entry) ? entry : null;
                }
                
                @Override
                public boolean remove(Object o) {
                    return entries.remove(o);
                }
                
                @Override
                public void clear() {
                    entries.clear();
                }
                
                @Override
                public int size() {
                    return Iterators.size(iterator());
                }
            };
        };
    }
    
    public boolean matchesSearch(Iterator<String> tags) {
        if (lowerCases.length == 0) return true;
        if (!tags.hasNext()) return true;
        for (String lowerCase : lowerCases) {
            boolean found = false;
            for (String tag : (Iterable<? extends String>) (() -> tags)) {
                if (tag.toLowerCase(Locale.ROOT).contains(lowerCase)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
    
    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        this.editBox.setWidth(Mth.clamp(entryWidth - 10, 0, 500));
        this.editBox.setX(x + entryWidth / 2 - this.editBox.getWidth() / 2);
        this.editBox.setY(y + entryHeight / 2 - 9);
        this.editBox.render(graphics, mouseX, mouseY, delta);
        if (this.editBox.getValue().isEmpty()) {
            this.editBox.setSuggestion("Search...");
        } else {
            this.editBox.setSuggestion(null);
        }
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }
    
    @Override
    public Object getValue() {
        return null;
    }
    
    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }
    
    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.singletonList(editBox);
    }
    
    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.singletonList(editBox);
    }
}
