package com.chatai.core;

/**
 * Result of parsing a player's request. Pure data, no Minecraft types, so the same
 * parser is shared by every Minecraft version build.
 */
public final class Intent {
    public enum Type { GIVE, TIME, WEATHER, EFFECT, SPAWN, BUILD, TELEPORT, FIREWORK, HEAL, CLEAR_WEATHER_HELP, UNKNOWN }

    public enum Weather { CLEAR, RAIN, THUNDER }

    public final Type type;
    /** Name of the target player, or null meaning "the requester". */
    public final String targetPlayer;
    /** Minecraft resource path without namespace, e.g. "diamond". */
    public final String id;
    public final int count;
    public final int durationSeconds;
    public final int amplifier;
    public final Weather weather;
    /** Time of day in ticks (0 = sunrise, 6000 noon, 13000 dusk, 18000 midnight). */
    public final long timeOfDay;
    /** Short human label of what was understood (Russian/English per language). */
    public final String label;

    private Intent(Builder b) {
        this.type = b.type; this.targetPlayer = b.targetPlayer; this.id = b.id; this.count = b.count;
        this.durationSeconds = b.durationSeconds; this.amplifier = b.amplifier; this.weather = b.weather;
        this.timeOfDay = b.timeOfDay; this.label = b.label;
    }

    /** Rough number of "actions" (blocks/entities/items) this intent costs against the per-request limit. */
    public int cost() {
        switch (type) {
            case GIVE: return Math.max(1, count);
            case SPAWN: return Math.max(1, count);
            case BUILD: return Structures.blockCount(id);
            case FIREWORK: return Math.max(1, count);
            default: return 1;
        }
    }

    /** Whether the intent changes blocks in the world in bulk (needs confirmation when enabled). */
    public boolean isLargeWorldChange() {
        return type == Type.BUILD || (type == Type.SPAWN && count >= 20);
    }

    public boolean affectsOtherPlayer(String requester) {
        return targetPlayer != null && !targetPlayer.equalsIgnoreCase(requester);
    }

    public static Builder of(Type t) { return new Builder(t); }

    public static final class Builder {
        Type type; String targetPlayer; String id; int count = 1; int durationSeconds = 60; int amplifier = 0;
        Weather weather = Weather.CLEAR; long timeOfDay = 1000; String label = "";
        Builder(Type t) { this.type = t; }
        public Builder target(String p) { this.targetPlayer = p; return this; }
        public Builder id(String v) { this.id = v; return this; }
        public Builder count(int v) { this.count = v; return this; }
        public Builder duration(int v) { this.durationSeconds = v; return this; }
        public Builder amplifier(int v) { this.amplifier = v; return this; }
        public Builder weather(Weather w) { this.weather = w; return this; }
        public Builder time(long t) { this.timeOfDay = t; return this; }
        public Builder label(String l) { this.label = l; return this; }
        public Intent build() { return new Intent(this); }
    }

    @Override public String toString() {
        return "Intent[" + type + ", target=" + targetPlayer + ", id=" + id + ", count=" + count
                + ", dur=" + durationSeconds + ", amp=" + amplifier + ", weather=" + weather + ", time=" + timeOfDay + "]";
    }
}
