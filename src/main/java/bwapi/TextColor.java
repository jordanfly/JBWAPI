package bwapi;

public enum TextColor {
    Previous(1),
    Default(2),
    Yellow(3),
    White(4),
    Grey(5),
    Red(6),
    Green(7),
    BrightRed(8),
    Invisible(11),
    Blue(14),
    Teal(15),
    Purple(16),
    Orange(17),
    Align_Right(18),
    Align_Center(19),
    Invisible2(20),
    Brown(21),
    PlayerWhite(22),
    PlayerYellow(23),
    DarkGreen(24),
    LightYellow(25),
    Cyan(26),
    Tan(27),
    GreyBlue(28),
    GreyGreen(29),
    GreyCyan(30),
    Turquoise(31);

    final byte id;

    TextColor(final int id) {
        this.id = (byte) id;
    }

    /**
     * Format text with a textcolor to display on broodwar
     */
    public static String formatText(final String text, final TextColor format) {
        final byte[] data = text.getBytes();
        final int len = text.length();
        final byte[] formatted = new byte[len + 1];
        formatted[0] = format.id;
        System.arraycopy(data, 0, formatted, 1, len);
        return new String(formatted);
    }

    //SINCE 4.2
    //C hecks if the given character is a color-changing control code.
    boolean isColor() {
        final int c = this.id;
        return (2 <= c && c <= 8) || (14 <= c && c <= 17) || (21 <= c && c <= 31);
    }
}
