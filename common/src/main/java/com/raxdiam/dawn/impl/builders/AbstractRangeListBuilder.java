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

package com.raxdiam.dawn.impl.builders;

import com.raxdiam.dawn.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

public abstract class AbstractRangeListBuilder<T, A extends AbstractConfigListEntry, SELF extends AbstractRangeListBuilder<T, A, SELF>> extends AbstractListBuilder<T, A, SELF> {
    protected T min = null, max = null;
    
    protected AbstractRangeListBuilder(Component resetButtonKey, Component fieldNameKey) {
        super(resetButtonKey, fieldNameKey);
    }
    
    public SELF setMin(T min) {
        this.min = min;
        return (SELF) this;
    }
    
    public SELF setMax(T max) {
        this.max = max;
        return (SELF) this;
    }
    
    public SELF removeMin() {
        this.min = null;
        return (SELF) this;
    }
    
    public SELF removeMax() {
        this.max = null;
        return (SELF) this;
    }
}
