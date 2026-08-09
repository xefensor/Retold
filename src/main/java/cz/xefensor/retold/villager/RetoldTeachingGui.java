package cz.xefensor.retold.villager;

public final class RetoldTeachingGui {
    public static final int PANEL_GAP = 4;
    public static final int PANEL_PADDING = 4;
    public static final int CONTROL_GAP = 4;
    public static final int TEXT_HEIGHT = 9;
    public static final int BUTTON_HEIGHT = 20;
    public static final int SLOT_SIZE = 16;
    public static final int SLOT_FRAME_SIZE = 20;

    public static final int PANEL_X = 276 + PANEL_GAP;
    public static final int PANEL_Y = PANEL_GAP;
    public static final int PANEL_WIDTH = 136;
    public static final int PANEL_HEIGHT = 158;
    public static final int CONTENT_X = PANEL_X + PANEL_PADDING;
    public static final int CONTENT_WIDTH = PANEL_WIDTH - 2 * PANEL_PADDING;

    public static final int TITLE_Y = PANEL_Y + PANEL_PADDING;
    public static final int SLOT_X = PANEL_X + (PANEL_WIDTH - SLOT_SIZE) / 2;
    public static final int SLOT_Y = TITLE_Y + TEXT_HEIGHT + 2 * CONTROL_GAP;
    public static final int STATUS_Y = SLOT_Y + SLOT_SIZE + 2 * CONTROL_GAP;
    public static final int COST_Y = STATUS_Y + 2 * TEXT_HEIGHT + CONTROL_GAP;
    public static final int DETAILS_Y = COST_Y + TEXT_HEIGHT + 3 * CONTROL_GAP;
    public static final int BUTTON_X = CONTENT_X;
    public static final int BUTTON_Y = PANEL_Y + PANEL_HEIGHT
            - PANEL_PADDING - BUTTON_HEIGHT;
    public static final int BUTTON_WIDTH = CONTENT_WIDTH;

    public static final int PANEL_BACKGROUND = 0xFF17231B;
    public static final int SECTION_BACKGROUND = 0xFF203229;
    public static final int PANEL_BORDER = 0xFF63806B;
    public static final int READY_BORDER = 0xFF61D889;
    public static final int SUCCESS_BORDER = 0xFFA6F3B8;
    public static final int FAILURE_BORDER = 0xFFE36F7F;
    public static final int TEXT_COLOR = 0xFFE3EBDD;

    private RetoldTeachingGui() {
    }
}
