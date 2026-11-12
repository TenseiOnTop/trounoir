package net.trounoir.util;

/** Petites aides de texte. */
public final class Text {
    private Text() {}

    private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static String roman(int n) {
        return n >= 0 && n < ROMAN.length ? ROMAN[n] : Integer.toString(n);
    }

    /** Durée lisible : "2 j 5 h", "37 min"… (20 ticks = 1 s ; 24000 ticks = 1 jour de jeu). */
    public static String duration(long ticks, String dayUnit, String hourUnit, String minuteUnit) {
        long days = ticks / 24000L;
        long hours = (ticks % 24000L) / 1000L;
        long minutes = ticks / 1200L;
        if (days > 0) return days + " " + dayUnit + " " + hours + " " + hourUnit;
        return Math.max(1, minutes) + " " + minuteUnit;
    }
}
