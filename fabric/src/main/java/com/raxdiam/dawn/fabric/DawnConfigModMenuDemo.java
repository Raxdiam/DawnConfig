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

package com.raxdiam.dawn.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.raxdiam.dawn.AutoConfig;
import com.raxdiam.dawn.example.ExampleConfig;
import com.raxdiam.dawn.DawnConfigDemo;
import net.minecraft.client.gui.screens.Screen;

public class DawnConfigModMenuDemo implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> {
            if (RenderSystem.isOnRenderThread() && Screen.hasShiftDown()) return AutoConfig.getConfigScreen(ExampleConfig.class, screen).get();
            return DawnConfigDemo.getConfigBuilderWithDemo().setParentScreen(screen).build();
        };
    }
}
