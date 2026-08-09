package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentCatalog;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentKnowledge;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantingOptions;
import cz.xefensor.retold.client.enchanting.RetoldEnchantingScreenFeedback;
import cz.xefensor.retold.client.enchanting.RetoldEnchantingScreenState;
import cz.xefensor.retold.enchanting.RetoldEnchantingCosts;
import cz.xefensor.retold.enchanting.RetoldEnchantmentSpellDefinition;
import cz.xefensor.retold.network.RetoldEnchantingCastPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

/** Adds Retold's glyph editor and known-spell panel to the vanilla table screen. */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu>
        implements RetoldEnchantingScreenFeedback {
    @Unique
    private static final int RETOLD$PANEL_GAP = 4;
    @Unique
    private static final int RETOLD$PANEL_PADDING = 4;
    @Unique
    private static final int RETOLD$CONTROL_GAP = 4;
    @Unique
    private static final int RETOLD$DENSE_GAP = 1;
    @Unique
    private static final int RETOLD$TEXT_HEIGHT = 9;
    @Unique
    private static final int RETOLD$CONTROL_HEIGHT = 16;
    @Unique
    private static final int RETOLD$COMPACT_CONTROL_HEIGHT = 14;
    @Unique
    private static final int RETOLD$KNOWN_ROWS = 4;
    @Unique
    private static final int RETOLD$GLYPH_COLUMNS = 9;
    @Unique
    private static final int RETOLD$MAIN_PANEL_OFFSET_X = 58;
    @Unique
    private static final int RETOLD$MAIN_PANEL_WIDTH = 114;
    @Unique
    private static final int RETOLD$MAIN_PANEL_HEIGHT = 77;
    @Unique
    private static final int RETOLD$SIDEBAR_WIDTH = 116;
    @Unique
    private static final int RETOLD$SIDEBAR_HEIGHT = 159;
    @Unique
    private static final int RETOLD$COST_Y = RETOLD$PANEL_PADDING;
    @Unique
    private static final int RETOLD$WORD_Y = RETOLD$COST_Y
            + RETOLD$TEXT_HEIGHT + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$LEVEL_Y = RETOLD$WORD_Y
            + RETOLD$CONTROL_HEIGHT + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$ACTION_Y = RETOLD$LEVEL_Y
            + RETOLD$CONTROL_HEIGHT + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$GLYPH_GRID_Y = RETOLD$PANEL_PADDING
            + RETOLD$TEXT_HEIGHT + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$KNOWN_TITLE_Y = RETOLD$GLYPH_GRID_Y
            + 3 * RETOLD$COMPACT_CONTROL_HEIGHT
            + 2 * RETOLD$DENSE_GAP
            + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$KNOWN_LIST_Y = RETOLD$KNOWN_TITLE_Y
            + RETOLD$TEXT_HEIGHT + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$PAGE_Y = RETOLD$KNOWN_LIST_Y
            + RETOLD$KNOWN_ROWS * RETOLD$COMPACT_CONTROL_HEIGHT
            + (RETOLD$KNOWN_ROWS - 1) * RETOLD$DENSE_GAP
            + RETOLD$CONTROL_GAP;
    @Unique
    private static final int RETOLD$GLYPH_BUTTON_WIDTH = 11;
    @Unique
    private static final int RETOLD$PAGE_BUTTON_WIDTH = 20;
    @Unique
    private static final int RETOLD$CLEAR_BUTTON_WIDTH = 32;
    @Unique
    private static final int RETOLD$PANEL_BACKGROUND = 0xFF201827;
    @Unique
    private static final int RETOLD$WORKSPACE_BACKGROUND = 0xFF2B2033;
    @Unique
    private static final int RETOLD$PANEL_BORDER = 0xFF8B6A9B;
    @Unique
    private static final int RETOLD$SELECTION_BORDER = 0xFFE7C76A;
    @Unique
    private static final int RETOLD$SUCCESS_BORDER = 0xFF7FE39A;
    @Unique
    private static final int RETOLD$FAILURE_BORDER = 0xFFE36F7F;
    @Unique
    private static final int RETOLD$FEEDBACK_HIGHLIGHT_TICKS = 20;
    @Unique
    private static final Style RETOLD$SGA_STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("alt"))
    );

    @Unique
    private final RetoldEnchantingScreenState retold$state =
            new RetoldEnchantingScreenState();
    @Unique
    private final List<Button> retold$wordButtons = new ArrayList<>();
    @Unique
    private final List<Button> retold$levelButtons = new ArrayList<>();
    @Unique
    private final List<Button> retold$knownButtons = new ArrayList<>();
    @Unique
    private List<RetoldEnchantmentSpellDefinition> retold$knownSpells = List.of();
    @Unique
    private Set<Identifier> retold$knowledgeSnapshot = Set.of();
    @Unique
    private ItemStack retold$lastTarget = ItemStack.EMPTY;
    @Unique
    private Button retold$castButton;
    @Unique
    private Button retold$previousPageButton;
    @Unique
    private Button retold$nextPageButton;
    @Unique
    private StringWidget retold$pageLabel;
    @Unique
    private StringWidget retold$costLabel;
    @Unique
    private int retold$page;
    @Unique
    private int retold$panelX;
    @Unique
    private int retold$mainPanelX;
    @Unique
    private int retold$panelTop;
    @Unique
    private int retold$feedbackHighlightTicks;
    @Unique
    private int retold$feedbackBorder = RETOLD$SUCCESS_BORDER;
    @Unique
    private boolean retold$castPending;

    private EnchantmentScreenMixin(
            EnchantmentMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void retold$addEnchantingWidgets(CallbackInfo ci) {
        this.retold$panelTop = this.topPos + RETOLD$PANEL_GAP;
        this.retold$mainPanelX = this.leftPos + RETOLD$MAIN_PANEL_OFFSET_X;
        this.retold$panelX = this.leftPos + this.imageWidth + RETOLD$PANEL_GAP;
        this.retold$addWordEditor();
        this.retold$addGlyphKeyboard();
        this.retold$addKnownSpellPanel();
        this.retold$refreshAllWidgets();
    }

    @Unique
    private void retold$addWordEditor() {
        int contentX = this.retold$mainPanelX + RETOLD$PANEL_PADDING;
        int contentWidth = RETOLD$MAIN_PANEL_WIDTH - 2 * RETOLD$PANEL_PADDING;
        int availableWordWidth = contentWidth - 2 * RETOLD$CONTROL_GAP;
        int baseWordWidth = availableWordWidth / 3;
        int extraWordPixels = availableWordWidth % 3;
        int wordX = contentX;

        for (int index = 0; index < 3; index++) {
            int selectedPosition = index;
            int wordWidth = baseWordWidth + (index < extraWordPixels ? 1 : 0);
            Button wordButton = Button.builder(
                    Component.literal("_"),
                    button -> {
                        this.retold$state.clearFrom(selectedPosition);
                        this.retold$refreshAllWidgets();
                    }
            ).bounds(
                    wordX,
                    this.retold$panelTop + RETOLD$WORD_Y,
                    wordWidth,
                    RETOLD$CONTROL_HEIGHT
            )
                    .tooltip(Tooltip.create(Component.translatable(
                            "container.retold.enchant.edit_word"
                    )))
                    .build();
            this.retold$wordButtons.add(wordButton);
            this.addRenderableWidget(wordButton);
            wordX += wordWidth + RETOLD$CONTROL_GAP;
        }

        int levelButtonWidth = (contentWidth - 4 * RETOLD$CONTROL_GAP) / 5;
        for (int level = 1; level <= 5; level++) {
            int selectedLevel = level;
            Button levelButton = Button.builder(
                    Component.translatable("enchantment.level." + level),
                    button -> {
                        this.retold$state.setLevel(selectedLevel);
                        this.retold$refreshAllWidgets();
                    }
            ).bounds(
                    contentX + (level - 1) * (levelButtonWidth + RETOLD$CONTROL_GAP),
                    this.retold$panelTop + RETOLD$LEVEL_Y,
                    levelButtonWidth,
                    RETOLD$CONTROL_HEIGHT
            ).build();
            this.retold$levelButtons.add(levelButton);
            this.addRenderableWidget(levelButton);
        }

        int writeButtonWidth = contentWidth
                - RETOLD$CONTROL_GAP - RETOLD$CLEAR_BUTTON_WIDTH;
        this.retold$castButton = Button.builder(
                Component.translatable("container.retold.enchant.write"),
                button -> this.retold$sendCast()
        ).bounds(
                contentX,
                this.retold$panelTop + RETOLD$ACTION_Y,
                writeButtonWidth,
                RETOLD$CONTROL_HEIGHT
        ).build();
        this.addRenderableWidget(this.retold$castButton);

        Button clearButton = Button.builder(
                Component.translatable("container.retold.enchant.clear"),
                button -> {
                    this.retold$state.clear();
                    this.retold$refreshAllWidgets();
                }
        ).bounds(
                contentX + writeButtonWidth + RETOLD$CONTROL_GAP,
                this.retold$panelTop + RETOLD$ACTION_Y,
                RETOLD$CLEAR_BUTTON_WIDTH,
                RETOLD$CONTROL_HEIGHT
        ).build();
        this.addRenderableWidget(clearButton);

        this.retold$costLabel = new StringWidget(
                contentX,
                this.retold$panelTop + RETOLD$COST_Y,
                contentWidth,
                RETOLD$TEXT_HEIGHT,
                Component.empty(),
                this.font
        );
        this.addRenderableWidget(this.retold$costLabel);
    }

    @Unique
    private void retold$addGlyphKeyboard() {
        int contentX = this.retold$panelX + RETOLD$PANEL_PADDING;
        for (int glyphIndex = 0; glyphIndex < 26; glyphIndex++) {
            int selectedGlyph = glyphIndex;
            int column = glyphIndex % RETOLD$GLYPH_COLUMNS;
            int row = glyphIndex / RETOLD$GLYPH_COLUMNS;
            Component glyph = Component.literal(Character.toString((char) ('A' + glyphIndex)))
                    .setStyle(RETOLD$SGA_STYLE);
            Button button = Button.builder(
                    glyph,
                    ignored -> {
                        this.retold$state.appendGlyph(selectedGlyph);
                        this.retold$refreshAllWidgets();
                    }
            ).bounds(
                    contentX + column * (RETOLD$GLYPH_BUTTON_WIDTH + RETOLD$DENSE_GAP),
                    this.retold$panelTop + RETOLD$GLYPH_GRID_Y
                            + row * (RETOLD$COMPACT_CONTROL_HEIGHT + RETOLD$DENSE_GAP),
                    RETOLD$GLYPH_BUTTON_WIDTH,
                    RETOLD$COMPACT_CONTROL_HEIGHT
            ).build();
            this.addRenderableWidget(button);
        }
    }

    @Unique
    private void retold$addKnownSpellPanel() {
        int contentX = this.retold$panelX + RETOLD$PANEL_PADDING;
        int contentWidth = RETOLD$SIDEBAR_WIDTH - 2 * RETOLD$PANEL_PADDING;
        for (int row = 0; row < RETOLD$KNOWN_ROWS; row++) {
            int selectedRow = row;
            Button button = Button.builder(
                    Component.empty(),
                    ignored -> this.retold$selectKnownSpell(selectedRow)
            ).bounds(
                    contentX,
                    this.retold$panelTop + RETOLD$KNOWN_LIST_Y
                            + row * (RETOLD$COMPACT_CONTROL_HEIGHT + RETOLD$DENSE_GAP),
                    contentWidth,
                    RETOLD$COMPACT_CONTROL_HEIGHT
            ).build();
            this.retold$knownButtons.add(button);
            this.addRenderableWidget(button);
        }

        this.retold$previousPageButton = Button.builder(
                Component.literal("<"),
                ignored -> {
                    this.retold$page--;
                    this.retold$refreshKnownSpellButtons();
                }
        ).bounds(
                contentX,
                this.retold$panelTop + RETOLD$PAGE_Y,
                RETOLD$PAGE_BUTTON_WIDTH,
                RETOLD$COMPACT_CONTROL_HEIGHT
        ).build();
        this.addRenderableWidget(this.retold$previousPageButton);

        this.retold$pageLabel = new StringWidget(
                contentX + RETOLD$PAGE_BUTTON_WIDTH + RETOLD$CONTROL_GAP,
                this.retold$panelTop + RETOLD$PAGE_Y,
                contentWidth - 2 * (RETOLD$PAGE_BUTTON_WIDTH + RETOLD$CONTROL_GAP),
                RETOLD$COMPACT_CONTROL_HEIGHT,
                Component.empty(),
                this.font
        );
        this.addRenderableWidget(this.retold$pageLabel);

        this.retold$nextPageButton = Button.builder(
                Component.literal(">"),
                ignored -> {
                    this.retold$page++;
                    this.retold$refreshKnownSpellButtons();
                }
        ).bounds(
                contentX + contentWidth - RETOLD$PAGE_BUTTON_WIDTH,
                this.retold$panelTop + RETOLD$PAGE_Y,
                RETOLD$PAGE_BUTTON_WIDTH,
                RETOLD$COMPACT_CONTROL_HEIGHT
        ).build();
        this.addRenderableWidget(this.retold$nextPageButton);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void retold$refreshEnchantingWidgets(CallbackInfo ci) {
        if (this.retold$feedbackHighlightTicks > 0) {
            this.retold$feedbackHighlightTicks--;
        }
        Set<Identifier> knowledge = RetoldClientEnchantmentKnowledge.snapshot();
        if (knowledge != this.retold$knowledgeSnapshot) {
            this.retold$knowledgeSnapshot = knowledge;
            this.retold$reloadKnownSpells();
        }
        ItemStack target = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(target, this.retold$lastTarget)) {
            this.retold$lastTarget = target.copy();
            this.retold$page = 0;
            this.retold$reloadKnownSpells();
        }
        this.retold$refreshCastState();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void retold$extractEnchantingPanels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        graphics.fill(
                this.retold$mainPanelX,
                this.retold$panelTop,
                this.retold$mainPanelX + RETOLD$MAIN_PANEL_WIDTH,
                this.retold$panelTop + RETOLD$MAIN_PANEL_HEIGHT,
                RETOLD$WORKSPACE_BACKGROUND
        );
        graphics.outline(
                this.retold$mainPanelX,
                this.retold$panelTop,
                RETOLD$MAIN_PANEL_WIDTH,
                RETOLD$MAIN_PANEL_HEIGHT,
                RETOLD$PANEL_BORDER
        );
        int contentWidth = RETOLD$MAIN_PANEL_WIDTH - 2 * RETOLD$PANEL_PADDING;
        int levelButtonWidth = (contentWidth - 4 * RETOLD$CONTROL_GAP) / 5;
        graphics.outline(
                this.retold$mainPanelX + RETOLD$PANEL_PADDING - 1
                        + (this.retold$state.level() - 1)
                        * (levelButtonWidth + RETOLD$CONTROL_GAP),
                this.retold$panelTop + RETOLD$LEVEL_Y - 1,
                levelButtonWidth + 2,
                RETOLD$CONTROL_HEIGHT + 2,
                RETOLD$SELECTION_BORDER
        );
        graphics.fill(
                this.retold$panelX,
                this.retold$panelTop,
                this.retold$panelX + RETOLD$SIDEBAR_WIDTH,
                this.retold$panelTop + RETOLD$SIDEBAR_HEIGHT,
                RETOLD$PANEL_BACKGROUND
        );
        graphics.outline(
                this.retold$panelX,
                this.retold$panelTop,
                RETOLD$SIDEBAR_WIDTH,
                RETOLD$SIDEBAR_HEIGHT,
                RETOLD$PANEL_BORDER
        );
        if (this.retold$feedbackHighlightTicks > 0) {
            graphics.outline(
                    this.leftPos + 14,
                    this.topPos + 46,
                    18,
                    18,
                    this.retold$feedbackBorder
            );
        }
        graphics.text(
                this.font,
                Component.translatable("container.retold.enchant.glyphs"),
                this.retold$panelX + RETOLD$PANEL_PADDING,
                this.retold$panelTop + RETOLD$PANEL_PADDING,
                0xFFE6D7EE,
                false
        );
        graphics.text(
                this.font,
                Component.translatable("container.retold.enchant.known"),
                this.retold$panelX + RETOLD$PANEL_PADDING,
                this.retold$panelTop + RETOLD$KNOWN_TITLE_Y,
                0xFFE6D7EE,
                false
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDown() || event.hasAltDown()) {
            return super.keyPressed(event);
        }

        int key = event.key();
        if (key >= GLFW_KEY_A && key <= GLFW_KEY_Z) {
            this.retold$state.appendGlyph(key - GLFW_KEY_A);
            this.retold$refreshAllWidgets();
            return true;
        }
        if (key == GLFW_KEY_BACKSPACE) {
            this.retold$state.backspace();
            this.retold$refreshAllWidgets();
            return true;
        }

        int selectedLevel = retold$levelForKey(key);
        if (selectedLevel > 0) {
            if (selectedLevel <= this.retold$selectedKnownMaximum().orElse(5)) {
                this.retold$state.setLevel(selectedLevel);
                this.retold$refreshAllWidgets();
            }
            return true;
        }
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
            if (this.retold$castButton.active) {
                this.retold$sendCast();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique
    private static int retold$levelForKey(int key) {
        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_5) {
            return key - GLFW_KEY_1 + 1;
        }
        if (key >= GLFW_KEY_KP_1 && key <= GLFW_KEY_KP_5) {
            return key - GLFW_KEY_KP_1 + 1;
        }
        return 0;
    }

    @Unique
    private void retold$sendCast() {
        if (this.retold$castPending) {
            return;
        }
        this.retold$state.word().ifPresent(word -> {
            this.retold$castPending = true;
            this.retold$refreshCastState();
            ClientPacketDistributor.sendToServer(new RetoldEnchantingCastPayload(
                    this.menu.containerId,
                    word,
                    this.retold$state.level()
            ));
        });
    }

    @Override
    public void retold$castFinished(int containerId, boolean success) {
        if (this.menu.containerId != containerId) {
            return;
        }
        this.retold$castPending = false;
        if (success) {
            this.retold$state.clear();
            this.retold$feedbackBorder = RETOLD$SUCCESS_BORDER;
        } else {
            this.retold$feedbackBorder = RETOLD$FAILURE_BORDER;
            this.minecraft.player.playSound(
                    SoundEvents.NOTE_BLOCK_BASS.value(),
                    0.35F,
                    0.65F
            );
        }
        this.retold$feedbackHighlightTicks = RETOLD$FEEDBACK_HIGHLIGHT_TICKS;
        this.retold$refreshAllWidgets();
    }

    @Unique
    private void retold$refreshAllWidgets() {
        this.retold$refreshWordButtons();
        this.retold$refreshLevelButtons();
        this.retold$refreshCastState();
        this.retold$reloadKnownSpells();
    }

    @Unique
    private void retold$refreshWordButtons() {
        for (int position = 0; position < this.retold$wordButtons.size(); position++) {
            Component label = this.retold$state.glyph(position).isPresent()
                    ? Component.literal(Character.toString(
                            (char) ('A' + this.retold$state.glyph(position).getAsInt())
                    )).setStyle(RETOLD$SGA_STYLE)
                    : Component.literal("_");
            this.retold$wordButtons.get(position).setMessage(label);
        }
    }

    @Unique
    private void retold$refreshLevelButtons() {
        int selectedMaximum = this.retold$selectedKnownMaximum().orElse(5);
        if (this.retold$state.level() > selectedMaximum) {
            this.retold$state.setLevel(selectedMaximum);
        }
        for (int index = 0; index < this.retold$levelButtons.size(); index++) {
            int level = index + 1;
            Button button = this.retold$levelButtons.get(index);
            button.setMessage(
                    Component.translatable("enchantment.level." + level)
            );
            button.active = level <= selectedMaximum;
        }
    }

    @Unique
    private void retold$refreshCastState() {
        if (this.retold$castButton == null) {
            return;
        }
        int experienceCost = RetoldEnchantingCosts.experienceLevelCost(
                this.retold$state.level()
        );
        boolean infiniteMaterials = this.minecraft.player.hasInfiniteMaterials();
        boolean enoughLapis = infiniteMaterials
                || this.menu.getSlot(1).getItem().is(Items.LAPIS_LAZULI)
                && this.menu.getSlot(1).getItem().getCount()
                >= RetoldEnchantingCosts.LAPIS_PER_CAST;
        boolean enoughExperience = infiniteMaterials
                || this.minecraft.player.experienceLevel >= experienceCost;
        this.retold$castButton.active = this.retold$state.word().isPresent()
                && !this.menu.getSlot(0).getItem().isEmpty()
                && enoughLapis
                && enoughExperience
                && !this.retold$castPending;

        Component costs = Component.translatable(
                "container.retold.enchant.cost",
                RetoldEnchantingCosts.LAPIS_PER_CAST,
                experienceCost
        );
        this.retold$costLabel.setMessage(costs);
        this.retold$castButton.setTooltip(Tooltip.create(costs));
    }

    @Unique
    private void retold$reloadKnownSpells() {
        this.retold$knowledgeSnapshot = RetoldClientEnchantmentKnowledge.snapshot();
        ItemStack target = this.menu.getSlot(0).getItem();
        this.retold$lastTarget = target.copy();
        this.retold$knownSpells = RetoldClientEnchantingOptions.availableKnownSpells(
                this.minecraft.level.registryAccess(),
                RetoldClientEnchantmentCatalog.definitions(),
                this.retold$knowledgeSnapshot,
                target
        );
        this.retold$refreshKnownSpellButtons();
    }

    @Unique
    private void retold$refreshKnownSpellButtons() {
        int pageCount = Math.max(
                1,
                (this.retold$knownSpells.size() + RETOLD$KNOWN_ROWS - 1)
                        / RETOLD$KNOWN_ROWS
        );
        this.retold$page = Math.clamp(this.retold$page, 0, pageCount - 1);
        int pageStart = this.retold$page * RETOLD$KNOWN_ROWS;

        for (int row = 0; row < this.retold$knownButtons.size(); row++) {
            Button button = this.retold$knownButtons.get(row);
            int spellIndex = pageStart + row;
            boolean present = spellIndex < this.retold$knownSpells.size();
            button.visible = present;
            button.active = present;
            if (present) {
                RetoldEnchantmentSpellDefinition definition =
                        this.retold$knownSpells.get(spellIndex);
                Identifier enchantment = Identifier.parse(definition.enchantment());
                int maximumLevel = RetoldClientEnchantingOptions.resolve(
                        this.minecraft.level.registryAccess(),
                        definition
                ).map(holder -> holder.value().getMaxLevel()).orElse(1);
                Component romanLevel = Component.translatable(
                        "enchantment.level." + maximumLevel
                );
                button.setMessage(
                        Component.translatable(enchantment.toLanguageKey("enchantment"))
                                .append(" ")
                                .append(romanLevel)
                );
                button.setTooltip(Tooltip.create(Component.translatable(
                        "container.retold.enchant.maximum_level",
                        romanLevel
                )));
            }
        }

        this.retold$previousPageButton.active = this.retold$page > 0;
        this.retold$nextPageButton.active = this.retold$page + 1 < pageCount;
        Component pageMessage = Component.translatable(
                "container.retold.enchant.page",
                this.retold$page + 1,
                pageCount
        );
        this.retold$pageLabel.setMessage(pageMessage);
        this.retold$pageLabel.setX(
                this.retold$panelX
                        + (RETOLD$SIDEBAR_WIDTH - this.font.width(pageMessage)) / 2
        );
    }

    @Unique
    private void retold$selectKnownSpell(int row) {
        int index = this.retold$page * RETOLD$KNOWN_ROWS + row;
        if (index < this.retold$knownSpells.size()) {
            RetoldEnchantmentSpellDefinition definition = this.retold$knownSpells.get(index);
            this.retold$state.selectWord(definition.word());
            RetoldClientEnchantingOptions.resolve(
                    this.minecraft.level.registryAccess(),
                    definition
            ).ifPresent(enchantment -> this.retold$state.setLevel(Math.min(
                    this.retold$state.level(),
                    enchantment.value().getMaxLevel()
            )));
            this.retold$refreshAllWidgets();
        }
    }

    @Unique
    private OptionalInt retold$selectedKnownMaximum() {
        RetoldEnchantmentSpellDefinition definition = this.retold$state.word()
                .flatMap(RetoldClientEnchantmentCatalog::byWord)
                .filter(candidate -> RetoldClientEnchantmentKnowledge.isKnown(
                        Identifier.parse(candidate.enchantment())
                ))
                .orElse(null);
        if (definition == null) {
            return OptionalInt.empty();
        }
        return RetoldClientEnchantingOptions.resolve(
                this.minecraft.level.registryAccess(),
                definition
        ).map(enchantment -> OptionalInt.of(enchantment.value().getMaxLevel()))
                .orElseGet(OptionalInt::empty);
    }
}
