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

package com.raxdiam.dawn.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raxdiam.dawn.DawnConfigInitializer;
import com.raxdiam.dawn.api.*;
import com.raxdiam.dawn.api.scroll.ScrollingContainer;
import com.raxdiam.dawn.gui.entries.EmptyEntry;
import com.raxdiam.dawn.gui.widget.DynamicEntryListWidget;
import com.raxdiam.dawn.gui.widget.SearchFieldEntry;
import com.raxdiam.dawn.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Supplier;

public class GlobalizedDawnConfigScreen extends AbstractConfigScreen implements ReferenceBuildingConfigScreen, Expandable {
    public DawnConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> listWidget;
    private AbstractWidget cancelButton, exitButton;
    private final LinkedHashMap<Component, List<AbstractConfigEntry<?>>> categorizedEntries = Maps.newLinkedHashMap();
    private final ScrollingContainer sideScroller = new ScrollingContainer() {
        @Override
        public Rectangle getBounds() {
            return new Rectangle(4, 4, getSideSliderPosition() - 14 - 4, height - 8);
        }
        
        @Override
        public int getMaxScrollHeight() {
            int i = 0;
            for (Reference reference : references) {
                if (i != 0) i += 3 * reference.getScale();
                i += font.lineHeight * reference.getScale();
            }
            return i;
        }
    };
    private Reference lastHoveredReference = null;
    private SearchFieldEntry searchFieldEntry;
    private final ScrollingContainer sideSlider = new ScrollingContainer() {
        private final Rectangle empty = new Rectangle();
        
        @Override
        public Rectangle getBounds() {
            return empty;
        }
        
        @Override
        public int getMaxScrollHeight() {
            return 1;
        }
    };
    private final List<Reference> references = Lists.newArrayList();
    private final LazyResettable<Integer> sideExpandLimit = new LazyResettable<>(() -> {
        int max = 0;
        for (Reference reference : references) {
            Component referenceText = reference.getText();
            int width = font.width(Component.literal(StringUtils.repeat("  ", reference.getIndent()) + "- ").append(referenceText));
            if (width > max) max = width;
        }
        return Math.min(max + 8, width / 4);
    });
    private boolean requestingReferenceRebuilding = false;
    
    @ApiStatus.Internal
    public GlobalizedDawnConfigScreen(Screen parent, Component title, Map<String, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
        super(parent, title, backgroundLocation);
        categoryMap.forEach((categoryName, category) -> {
            List<AbstractConfigEntry<?>> entries = Lists.newArrayList();
            for (Object object : category.getEntries()) {
                AbstractConfigListEntry<?> entry;
                if (object instanceof Tuple<?, ?>) {
                    entry = (AbstractConfigListEntry<?>) ((Tuple<?, ?>) object).getB();
                } else {
                    entry = (AbstractConfigListEntry<?>) object;
                }
                entry.setScreen(this);
                entries.add(entry);
            }
            categorizedEntries.put(category.getCategoryKey(), entries);
        });
        this.sideSlider.scrollTo(0, false);
    }
    
    @Override
    public void requestReferenceRebuilding() {
        this.requestingReferenceRebuilding = true;
    }
    
    @Override
    public Map<Component, List<AbstractConfigEntry<?>>> getCategorizedEntries() {
        return this.categorizedEntries;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    protected void init() {
        super.init();
        this.sideExpandLimit.reset();
        this.references.clear();
        buildReferences();
        this.addWidget(listWidget = new DawnConfigScreen.ListWidget<>(this, minecraft, width - 14, height, 30, height - 32, getBackgroundLocation()));
        this.listWidget.setLeftPos(14);
        this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
        this.listWidget.children().add((AbstractConfigEntry) (searchFieldEntry = new SearchFieldEntry(this, listWidget)));
        this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
        this.categorizedEntries.forEach((category, entries) -> {
            if (!listWidget.children().isEmpty())
                this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(4));
            this.listWidget.children().add((AbstractConfigEntry) new CategoryTextEntry(category, category.copy().withStyle(ChatFormatting.BOLD)));
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(4));
            this.listWidget.children().addAll((List) entries);
        });
        int buttonWidths = Math.min(200, (width - 50 - 12) / 3);
        addRenderableWidget(cancelButton = Button.builder(isEdited() ? Component.translatable("text.dawn-config.cancel_discard") : Component.translatable("gui.cancel"), widget -> quit()).bounds(0, height - 26, buttonWidths, 20).build());
        addRenderableWidget(exitButton = new Button(0, height - 26, buttonWidths, 20, Component.empty(), button -> saveAll(true), Supplier::get) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                boolean hasErrors = false;
                label:
                for (List<AbstractConfigEntry<?>> entries : categorizedEntries.values()) {
                    for (AbstractConfigEntry<?> entry : entries) {
                        if (entry.getConfigError().isPresent()) {
                            hasErrors = true;
                            break label;
                        }
                    }
                }
                active = isEdited() && !hasErrors;
                setMessage(hasErrors ? Component.translatable("text.dawn-config.error_cannot_save") : Component.translatable("text.dawn-config.save_and_done"));
                super.renderWidget(graphics, mouseX, mouseY, delta);
            }
        });
        Optional.ofNullable(this.afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
    }
    
    @Override
    public boolean matchesSearch(Iterator<String> tags) {
        return searchFieldEntry.matchesSearch(tags);
    }
    
    private void buildReferences() {
        categorizedEntries.forEach((categoryText, entries) -> {
            this.references.add(new CategoryReference(categoryText));
            for (AbstractConfigEntry<?> entry : entries) buildReferenceFor(entry, 1);
        });
    }
    
    private void buildReferenceFor(AbstractConfigEntry<?> entry, int layer) {
        List<ReferenceProvider<?>> referencableEntries = entry.getReferenceProviderEntries();
        if (referencableEntries != null) {
            this.references.add(new ConfigEntryReference(entry, layer));
            for (ReferenceProvider<?> referencableEntry : referencableEntries) {
                buildReferenceFor(referencableEntry.provideReferenceEntry(), layer + 1);
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.lastHoveredReference = null;
        if (requestingReferenceRebuilding) {
            this.references.clear();
            buildReferences();
            requestingReferenceRebuilding = false;
        }
        int sliderPosition = getSideSliderPosition();
        if (!isTransparentBackground()) {
            ScissorsHandler.INSTANCE.scissor(new Rectangle(sliderPosition, 0, width - sliderPosition, height));
            renderMenuBackground(graphics);
            overlayBackground(graphics, new Rectangle(14, 0, width, height), 64, 64, 64, 255, 255);
        } else {
            if (this.minecraft.level == null) {
                this.renderPanorama(graphics, delta);
            }
            renderBlurredBackground(delta);
            renderMenuBackground(graphics);
            ScissorsHandler.INSTANCE.scissor(new Rectangle(sliderPosition, 0, width - sliderPosition, height));
        }
        listWidget.width = width - sliderPosition;
        listWidget.setLeftPos(sliderPosition);
        listWidget.render(graphics, mouseX, mouseY, delta);
        ScissorsHandler.INSTANCE.scissor(new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.bottom - listWidget.top));
        for (AbstractConfigEntry<?> child : listWidget.children())
            child.lateRender(graphics, mouseX, mouseY, delta);
        ScissorsHandler.INSTANCE.removeLastScissor();
        graphics.drawString(font, title.getVisualOrderText(), (int) (sliderPosition + (width - sliderPosition) / 2f - font.width(title) / 2f), 12, -1);
        ScissorsHandler.INSTANCE.removeLastScissor();
        cancelButton.setX(sliderPosition + (width - sliderPosition) / 2 - cancelButton.getWidth() - 3);
        exitButton.setX(sliderPosition + (width - sliderPosition) / 2 + 3);
        super.render(graphics, mouseX, mouseY, delta);
        sideSlider.updatePosition(delta);
        sideScroller.updatePosition(delta);
        if (isTransparentBackground()) {
            RenderSystem.enableBlend();
            graphics.blit(ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png"), 0, 0, sliderPosition, height, sliderPosition, height, 32, 32);
            graphics.blit(ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png"), 0, 0, sliderPosition - 14, height, sliderPosition - 14, height, 32, 32);
            graphics.blit(DynamicEntryListWidget.VERTICAL_HEADER_SEPARATOR, sliderPosition - 1, 0, 0.0F, 0.0F, 1, this.height, 2, 32);
            if (sliderPosition - 14 - 1 > 0) {
                graphics.blit(DynamicEntryListWidget.VERTICAL_HEADER_SEPARATOR, sliderPosition - 14 - 1, 0, 0.0F, 0.0F, 1, this.height, 2, 32);
            }
            RenderSystem.disableBlend();
        } else {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, getBackgroundLocation());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float f = 32.0F;
            buffer.addVertex(sliderPosition - 14, height, 0.0F).setUv(0, height / 32.0F).setColor(68, 68, 68, 255);
            buffer.addVertex(sliderPosition, height, 0.0F).setUv(14 / 32.0F, height / 32.0F).setColor(68, 68, 68, 255);
            buffer.addVertex(sliderPosition, 0, 0.0F).setUv(14 / 32.0F, 0).setColor(68, 68, 68, 255);
            buffer.addVertex(sliderPosition - 14, 0, 0.0F).setUv(0, 0).setColor(68, 68, 68, 255);
            
            buffer.addVertex(0, height, 0.0F).setUv(0, (height + sideScroller.scrollAmountInt()) / 32.0F).setColor(32, 32, 32, 255);
            buffer.addVertex(sliderPosition - 14, height, 0.0F).setUv((sliderPosition - 14) / 32.0F, (height + sideScroller.scrollAmountInt()) / 32.0F).setColor(32, 32, 32, 255);
            buffer.addVertex(sliderPosition - 14, 0, 0.0F).setUv((sliderPosition - 14) / 32.0F, sideScroller.scrollAmountInt() / 32.0F).setColor(32, 32, 32, 255);
            buffer.addVertex(0, 0, 0.0F).setUv(0, sideScroller.scrollAmountInt() / 32.0F).setColor(32, 32, 32, 255);
            
            Matrix4f matrix = graphics.pose().last().pose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            int shadeColor = isTransparentBackground() ? 120 : 160;
            buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(matrix, sliderPosition + 4, 0, 100.0F).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, sliderPosition, 0, 100.0F).setColor(0, 0, 0, shadeColor);
            buffer.addVertex(matrix, sliderPosition, height, 100.0F).setColor(0, 0, 0, shadeColor);
            buffer.addVertex(matrix, sliderPosition + 4, height, 100.0F).setColor(0, 0, 0, 0);
            shadeColor /= 2;
            buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(matrix, sliderPosition - 14, 0, 100.0F).setColor(0, 0, 0, shadeColor);
            buffer.addVertex(matrix, sliderPosition - 14 - 4, 0, 100.0F).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, sliderPosition - 14 - 4, height, 100.0F).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, sliderPosition - 14, height, 100.0F).setColor(0, 0, 0, shadeColor);
            RenderSystem.disableBlend();
        }
        Rectangle slideArrowBounds = new Rectangle(sliderPosition - 14, 0, 14, height);
        {
            MultiBufferSource.BufferSource immediate = graphics.bufferSource();
            font.renderText(">", sliderPosition - 7 - font.width(">") / 2f, height / 2, (slideArrowBounds.contains(mouseX, mouseY) ? 16777120 : 16777215) | Mth.clamp(Mth.ceil((1 - sideSlider.scrollAmount()) * 255.0F), 0, 255) << 24, false, graphics.pose().last().pose(), immediate, Font.DisplayMode.NORMAL, 0, 15728880);
            font.renderText("<", sliderPosition - 7 - font.width("<") / 2f, height / 2, (slideArrowBounds.contains(mouseX, mouseY) ? 16777120 : 16777215) | Mth.clamp(Mth.ceil(sideSlider.scrollAmount() * 255.0F), 0, 255) << 24, false, graphics.pose().last().pose(), immediate, Font.DisplayMode.NORMAL, 0, 15728880);
            graphics.flush();
            
            Rectangle scrollerBounds = sideScroller.getBounds();
            if (!scrollerBounds.isEmpty()) {
                ScissorsHandler.INSTANCE.scissor(new Rectangle(0, 0, sliderPosition - 14, height));
                int scrollOffset = scrollerBounds.y - sideScroller.scrollAmountInt();
                for (Reference reference : references) {
                    graphics.pose().pushPose();
                    graphics.pose().scale(reference.getScale(), reference.getScale(), reference.getScale());
                    MutableComponent text = Component.literal(StringUtils.repeat("  ", reference.getIndent()) + "- ").append(reference.getText());
                    if (lastHoveredReference == null && new Rectangle(scrollerBounds.x, (int) (scrollOffset - 4 * reference.getScale()), (int) (font.width(text) * reference.getScale()), (int) ((font.lineHeight + 4) * reference.getScale())).contains(mouseX, mouseY))
                        lastHoveredReference = reference;
                    graphics.drawString(font, text.getVisualOrderText(), scrollerBounds.x, scrollOffset, lastHoveredReference == reference ? 16769544 : 16777215, false);
                    graphics.pose().popPose();
                    scrollOffset += (font.lineHeight + 3) * reference.getScale();
                }
                ScissorsHandler.INSTANCE.removeLastScissor();
                sideScroller.renderScrollBar(graphics);
            }
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Rectangle slideBounds = new Rectangle(0, 0, getSideSliderPosition() - 14, height);
        if (button == 0 && slideBounds.contains(mouseX, mouseY) && lastHoveredReference != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            lastHoveredReference.go();
            return true;
        }
        Rectangle slideArrowBounds = new Rectangle(getSideSliderPosition() - 14, 0, 14, height);
        if (button == 0 && slideArrowBounds.contains(mouseX, mouseY)) {
            setExpanded(!isExpanded());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isExpanded() {
        return sideSlider.scrollTarget() == 1;
    }
    
    @Override
    public void setExpanded(boolean expanded) {
        this.sideSlider.scrollTo(expanded ? 1 : 0, true, 2000);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        Rectangle slideBounds = new Rectangle(0, 0, getSideSliderPosition() - 14, height);
        if (amountY != 0 && slideBounds.contains(mouseX, mouseY)) {
            sideScroller.offset(DawnConfigInitializer.getScrollStep() * -amountY, true);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }
    
    private int getSideSliderPosition() {
        return (int) (sideSlider.scrollAmount() * sideExpandLimit.get() + 14);
    }
    
    private static class CategoryTextEntry extends AbstractConfigListEntry<Object> {
        private final Component category;
        private final Component text;
        
        public CategoryTextEntry(Component category, Component text) {
            super(Component.literal(UUID.randomUUID().toString()), false);
            this.category = category;
            this.text = text;
        }
        
        @Override
        public int getItemHeight() {
            List<FormattedCharSequence> strings = Minecraft.getInstance().font.split(text, getParent().getItemWidth());
            if (strings.isEmpty())
                return 0;
            return 4 + strings.size() * 10;
        }
        
        @Nullable
        public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
            return null;
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
        public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
            return false;
        }
        
        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
            int yy = y + 2;
            List<FormattedCharSequence> texts = Minecraft.getInstance().font.split(this.text, getParent().getItemWidth());
            for (FormattedCharSequence text : texts) {
                graphics.drawString(Minecraft.getInstance().font, text, x - 4 + entryWidth / 2 - Minecraft.getInstance().font.width(text) / 2, yy, -1);
                yy += 10;
            }
        }
        
        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
        
        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }
    
    private interface Reference {
        default int getIndent() {
            return 0;
        }
        
        Component getText();
        
        float getScale();
        
        void go();
    }
    
    private class CategoryReference implements Reference {
        private final Component category;
        
        public CategoryReference(Component category) {
            this.category = category;
        }
        
        @Override
        public Component getText() {
            return category;
        }
        
        @Override
        public float getScale() {
            return 1.0F;
        }
        
        @Override
        public void go() {
            int i = 0;
            for (AbstractConfigEntry<?> child : listWidget.children()) {
                if (child instanceof CategoryTextEntry && ((CategoryTextEntry) child).category == category) {
                    listWidget.scrollTo(i, true);
                    return;
                }
                i += child.getItemHeight();
            }
        }
    }
    
    private class ConfigEntryReference implements Reference {
        private final AbstractConfigEntry<?> entry;
        private final int layer;
        
        public ConfigEntryReference(AbstractConfigEntry<?> entry, int layer) {
            this.entry = entry;
            this.layer = layer;
        }
        
        @Override
        public int getIndent() {
            return layer;
        }
        
        @Override
        public Component getText() {
            return entry.getFieldName();
        }
        
        @Override
        public float getScale() {
            return 1.0F;
        }
        
        @Override
        public void go() {
            int[] i = {0};
            for (AbstractConfigEntry<?> child : listWidget.children()) {
                int i1 = i[0];
                if (goChild(i, null, child)) return;
                i[0] = i1 + child.getItemHeight();
            }
        }
        
        private boolean goChild(int[] i, Integer expandedParent, AbstractConfigEntry<?> root) {
            if (root == entry) {
                listWidget.scrollTo(expandedParent == null ? i[0] : expandedParent, true);
                return true;
            }
            int j = i[0];
            i[0] += root.getInitialReferenceOffset();
            boolean expanded = root instanceof Expandable && ((Expandable) root).isExpanded();
            if (root instanceof Expandable) ((Expandable) root).setExpanded(true);
            List<? extends GuiEventListener> children = root.children();
            if (root instanceof Expandable) ((Expandable) root).setExpanded(expanded);
            for (GuiEventListener child : children) {
                if (child instanceof ReferenceProvider<?>) {
                    int i1 = i[0];
                    if (goChild(i, expandedParent != null ? expandedParent : root instanceof Expandable && !expanded ? j : null, ((ReferenceProvider<?>) child).provideReferenceEntry())) {
                        return true;
                    }
                    i[0] = i1 + ((ReferenceProvider<?>) child).provideReferenceEntry().getItemHeight();
                }
            }
            return false;
        }
    }
}
