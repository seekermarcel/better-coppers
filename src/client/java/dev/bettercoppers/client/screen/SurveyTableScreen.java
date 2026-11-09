package dev.bettercoppers.client.screen;

import dev.bettercoppers.menu.SurveyTableMenu;
import dev.bettercoppers.mining.MinePattern;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Uses the stonecutter background (input slot left, result slot right). The middle panel shows the Mine Pattern button
 * and the length and setting rows instead of the recipe list.
 */
public class SurveyTableScreen extends AbstractContainerScreen<SurveyTableMenu> {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
	private static final int PANEL_X = 52;
	private static final int PANEL_Y = 14;
	private static final int PANEL_WIDTH = 80;
	private static final int PANEL_HEIGHT = 54;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int LABEL_COLOR = 0xFFB0A090;
	private static final int LENGTH_ROW = 22;
	private static final int SETTING_ROW = 42;

	private Button patternButton;
	private Button lessButton;
	private Button moreButton;

	public SurveyTableScreen(SurveyTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.titleLabelY--;
	}

	@Override
	protected void init() {
		super.init();
		int x = this.leftPos + PANEL_X;
		int y = this.topPos + PANEL_Y;
		this.patternButton = this.addRenderableWidget(this.button(this.menu.getPattern().displayName(), x + 1, y + 1, PANEL_WIDTH - 2, SurveyTableMenu.BUTTON_NEXT_PATTERN));
		this.addRenderableWidget(this.button(Component.literal("-"), x + 2, y + LENGTH_ROW, 12, SurveyTableMenu.BUTTON_SHORTER));
		this.addRenderableWidget(this.button(Component.literal("+"), x + PANEL_WIDTH - 14, y + LENGTH_ROW, 12, SurveyTableMenu.BUTTON_LONGER));
		this.lessButton = this.addRenderableWidget(this.button(Component.literal("-"), x + 2, y + SETTING_ROW, 12, SurveyTableMenu.BUTTON_LESS));
		this.moreButton = this.addRenderableWidget(this.button(Component.literal("+"), x + PANEL_WIDTH - 14, y + SETTING_ROW, 12, SurveyTableMenu.BUTTON_MORE));
		this.followMenu();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.followMenu();
	}

	/** The menu's values arrive from the server after the click, so the widgets catch up here. */
	private void followMenu() {
		this.patternButton.setMessage(this.menu.getPattern().displayName());
		this.lessButton.visible = this.menu.getPattern().hasSetting();
		this.moreButton.visible = this.menu.getPattern().hasSetting();
	}

	private Button button(Component label, int x, int y, int width, int buttonId) {
		return Button.builder(label, b -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId))
			.bounds(x, y, width, 11)
			.build();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		// Cover the recipe list and scroll bar with a plain dark panel.
		int x = this.leftPos + PANEL_X;
		int y = this.topPos + PANEL_Y;
		graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF3A3A3A);
		graphics.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, 0xFF5B4636);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		int center = PANEL_X + PANEL_WIDTH / 2;
		MinePattern pattern = this.menu.getPattern();
		String length = this.menu.getLength() + (this.menu.getLength() == pattern.maxLength() ? " max" : "");
		this.row(graphics, Component.translatable("screen.bettercoppers.length"), length, center, LENGTH_ROW);
		if (pattern.hasSetting()) {
			this.row(graphics, pattern.setting().displayName(), String.valueOf(this.menu.getSetting()), center, SETTING_ROW);
		}
	}

	private void row(GuiGraphicsExtractor graphics, Component label, String value, int center, int rowY) {
		graphics.centeredText(this.font, label, center, PANEL_Y + rowY - 8, LABEL_COLOR);
		graphics.centeredText(this.font, Component.literal(value), center, PANEL_Y + rowY + 2, TEXT_COLOR);
	}
}
