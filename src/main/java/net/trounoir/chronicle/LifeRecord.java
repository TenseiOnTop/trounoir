package net.trounoir.chronicle;

import java.util.ArrayList;
import java.util.List;

/** Une vie achevée, telle qu'elle est conservée dans la Chronique des Vies. */
public final class LifeRecord {
    /** Ligne traduisible côté client : clé + arguments (un argument commençant par '@' est lui-même une clé). */
    public static final class Line {
        public String key = "";
        public List<String> args = new ArrayList<>();

        public Line() {}

        public Line(String key, String... args) {
            this.key = key;
            this.args.addAll(List.of(args));
        }
    }

    public int number;
    public String name = "";
    public long durationTicks;
    /** Voie dominante : étiquette la plus représentée. */
    public String path = "";
    public List<String> notable = new ArrayList<>();
    public List<String> allies = new ArrayList<>();
    public List<Line> feats = new ArrayList<>();
    public String cause = "";
    public String killer = "";
    public String killerId = "";
    public String place = "";
    public String epitaph = "";
    public int power;
    public int rank;
    public int kills;
    public boolean victory;
}
