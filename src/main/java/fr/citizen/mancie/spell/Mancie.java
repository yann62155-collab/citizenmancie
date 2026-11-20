package fr.citizen.mancie.spell;

import java.util.Locale;

/** Les trois écoles de mancie du serveur Citizen. */
public enum Mancie {
    PYROMANCIE("pyromancie", "Pyromancie", "✹", 0xFFE8641E),
    LUMINOMANCIE("luminomancie", "Luminomancie", "☀", 0xFFF2D46A),
    HEMATOMANCIE("hematomancie", "Hématomancie", "❤", 0xFFB0141E);

    private final String id;
    private final String displayName;
    private final String symbol;
    private final int color; // ARGB

    Mancie(String id, String displayName, String symbol, int color) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String symbol() { return symbol; }
    public int color() { return color; }
    public int rgb() { return color & 0xFFFFFF; }

    public static Mancie byId(String id) {
        if (id == null) return null;
        String s = id.toLowerCase(Locale.ROOT).replace("é", "e");
        for (Mancie m : values()) if (m.id.equals(s)) return m;
        return null;
    }
}
