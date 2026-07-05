package net.blueva.foundation.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BlueFoundation's own packet-based sidebar scoreboard.
 *
 * <p>Multiversion support from Minecraft 1.8 to 1.26.2, with native Adventure
 * parsing on Paper and BlueFoundation's own MiniMessage parser on Spigot.</p>
 */
public final class BfScoreboard {

    private final Player player;
    private final String id;

    private final List<String> lines = new ArrayList<>();
    private final List<String> scores = new ArrayList<>();
    private String title = "";

    private volatile boolean deleted = false;

    /**
     * Creates a new scoreboard for the given player.
     *
     * @param player the owner of the scoreboard
     */
    public BfScoreboard(Player player) {
        this.player = Objects.requireNonNull(player, "player");
        this.id = "bf-sb-" + Integer.toHexString(ThreadLocalRandom.current().nextInt());

        try {
            Object objective = ScoreboardReflection.newObjective(this.id, ScoreboardComponentConverter.toComponent(this.title));
            sendPacket(ScoreboardReflection.objectivePacket(objective, ScoreboardReflection.ObjectiveMode.CREATE));
            sendPacket(ScoreboardReflection.displayObjectivePacket(objective));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create scoreboard", t);
        }
    }

    /**
     * Returns the current title.
     *
     * @return the title
     */
    public synchronized String getTitle() {
        return this.title;
    }

    /**
     * Updates the scoreboard title.
     *
     * @param title the new title
     */
    public synchronized void updateTitle(String title) {
        Objects.requireNonNull(title, "title");
        if (this.title.equals(title)) {
            return;
        }
        this.title = title;
        try {
            Object objective = ScoreboardReflection.newObjective(this.id, ScoreboardComponentConverter.toComponent(title));
            sendPacket(ScoreboardReflection.objectivePacket(objective, ScoreboardReflection.ObjectiveMode.UPDATE));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard title", t);
        }
    }

    /**
     * Updates the scoreboard title with an Adventure component.
     *
     * @param title the new title as an Adventure component
     */
    public synchronized void updateTitle(Component title) {
        Objects.requireNonNull(title, "title");
        updateTitle(LegacyComponentSerializer.legacySection().serialize(title));
    }

    /**
     * Returns a copy of the current lines.
     *
     * @return the lines
     */
    public synchronized List<String> getLines() {
        return new ArrayList<>(this.lines);
    }

    /**
     * Returns the line at the given index.
     *
     * @param line the line index
     * @return the line text
     */
    public synchronized String getLine(int line) {
        checkLineNumber(line, true, false);
        return this.lines.get(line);
    }

    /**
     * Updates a single line.
     *
     * @param line the line index
     * @param text the new text
     */
    public synchronized void updateLine(int line, String text) {
        updateLine(line, text, null);
    }

    /**
     * Updates a single line, optionally with a custom score text (1.20.3+).
     *
     * @param line      the line index
     * @param text      the new text
     * @param scoreText the custom score text, or null for blank
     */
    public synchronized void updateLine(int line, String text, String scoreText) {
        checkLineNumber(line, false, false);
        try {
            if (line < size()) {
                this.lines.set(line, text);
                this.scores.set(line, scoreText);
                sendLineChange(getScoreByLine(line));
                if (ScoreboardReflection.customScoresSupported()) {
                    sendScorePacket(getScoreByLine(line), ScoreboardReflection.ScoreboardAction.CHANGE);
                }
                return;
            }

            List<String> newLines = new ArrayList<>(this.lines);
            List<String> newScores = new ArrayList<>(this.scores);
            if (line > size()) {
                for (int i = size(); i < line; i++) {
                    newLines.add("");
                    newScores.add(null);
                }
            }
            newLines.add(text);
            newScores.add(scoreText);
            updateLines(newLines, newScores);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard line", t);
        }
    }

    /**
     * Updates a single line from an Adventure component.
     *
     * @param line the line index
     * @param text the new text as an Adventure component
     */
    public synchronized void updateLine(int line, Component text) {
        Objects.requireNonNull(text, "text");
        updateLine(line, LegacyComponentSerializer.legacySection().serialize(text));
    }

    /**
     * Removes a line at the given index.
     *
     * @param line the line index
     */
    public synchronized void removeLine(int line) {
        checkLineNumber(line, false, false);
        if (line >= size()) {
            return;
        }
        List<String> newLines = new ArrayList<>(this.lines);
        List<String> newScores = new ArrayList<>(this.scores);
        newLines.remove(line);
        newScores.remove(line);
        updateLines(newLines, newScores);
    }

    /**
     * Updates all lines.
     *
     * @param lines the new lines
     */
    public synchronized void updateLines(String... lines) {
        updateLines(Arrays.asList(lines));
    }

    /**
     * Updates all lines from Adventure components.
     *
     * @param lines the new lines as Adventure components
     */
    public synchronized void updateLines(Component... lines) {
        Objects.requireNonNull(lines, "lines");
        List<String> serialized = new ArrayList<>(lines.length);
        for (Component line : lines) {
            serialized.add(line == null ? "" : LegacyComponentSerializer.legacySection().serialize(line));
        }
        updateLines(serialized);
    }

    /**
     * Updates all lines.
     *
     * @param lines the new lines
     */
    public synchronized void updateLines(Collection<String> lines) {
        updateLines(lines, null);
    }

    /**
     * Updates all lines and their custom score texts (1.20.3+).
     *
     * @param lines  the new lines
     * @param scores the custom score texts, or null for blank
     */
    public synchronized void updateLines(Collection<String> lines, Collection<String> scores) {
        Objects.requireNonNull(lines, "lines");
        checkLineNumber(lines.size(), false, true);

        if (scores != null && scores.size() != lines.size()) {
            throw new IllegalArgumentException("The size of scores must match the size of lines");
        }

        List<String> oldLines = new ArrayList<>(this.lines);
        this.lines.clear();
        this.lines.addAll(lines);

        List<String> oldScores = new ArrayList<>(this.scores);
        this.scores.clear();
        this.scores.addAll(scores != null ? scores : Collections.nCopies(lines.size(), null));

        try {
            if (oldLines.size() != this.lines.size()) {
                if (oldLines.size() > this.lines.size()) {
                    for (int i = oldLines.size(); i > this.lines.size(); i--) {
                        sendTeamPacket(i - 1, ScoreboardReflection.TeamMode.REMOVE);
                        sendScorePacket(i - 1, ScoreboardReflection.ScoreboardAction.REMOVE);
                    }
                } else {
                    for (int i = oldLines.size(); i < this.lines.size(); i++) {
                        sendScorePacket(i, ScoreboardReflection.ScoreboardAction.CHANGE);
                        sendTeamPacket(i, ScoreboardReflection.TeamMode.CREATE);
                    }
                }
            }

            for (int i = 0; i < this.lines.size(); i++) {
                if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(i))) {
                    sendLineChange(i);
                }
                if (!Objects.equals(getLineByScore(oldScores, i), getLineByScore(this.scores, i))) {
                    sendScorePacket(i, ScoreboardReflection.ScoreboardAction.CHANGE);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard lines", t);
        }
    }

    /**
     * Returns the player who owns this scoreboard.
     *
     * @return the player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns whether this scoreboard has been deleted.
     *
     * @return true if deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Returns the number of lines.
     *
     * @return the size
     */
    public synchronized int size() {
        return this.lines.size();
    }

    /**
     * Deletes this scoreboard and removes it from the player.
     */
    public synchronized void delete() {
        if (this.deleted) {
            return;
        }
        try {
            for (int i = 0; i < this.lines.size(); i++) {
                sendTeamPacket(i, ScoreboardReflection.TeamMode.REMOVE);
            }
            Object objective = ScoreboardReflection.newObjective(this.id, ScoreboardComponentConverter.toComponent(this.title));
            sendPacket(ScoreboardReflection.objectivePacket(objective, ScoreboardReflection.ObjectiveMode.REMOVE));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to delete scoreboard", t);
        }
        this.deleted = true;
    }

    private void sendLineChange(int score) throws Throwable {
        String line = getLineByScore(score);
        if (line == null) {
            line = "";
        }

        boolean legacy = ScoreboardReflection.versionType().ordinal() < ScoreboardReflection.VersionType.V1_13.ordinal();
        int maxLength = legacy ? 16 : 1024;

        String prefix;
        String suffix = "";

        if (line.isEmpty()) {
            prefix = ScoreboardReflection.COLOR_CODES[score] + ChatColor.RESET;
        } else if (line.length() <= maxLength) {
            prefix = line;
        } else {
            int index = line.charAt(maxLength - 1) == ChatColor.COLOR_CHAR
                    ? (maxLength - 1) : maxLength;
            prefix = line.substring(0, index);
            String suffixTmp = line.substring(index);
            ChatColor chatColor = null;
            if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
                chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
            }
            String color = ChatColor.getLastColors(prefix);
            boolean addColor = chatColor == null || chatColor.isFormat();
            suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET.toString() : color) : "") + suffixTmp;
        }

        if (prefix.length() > maxLength) {
            prefix = prefix.substring(0, maxLength);
        }
        if (suffix.length() > maxLength) {
            suffix = suffix.substring(0, maxLength);
        }

        Object prefixComponent = ScoreboardComponentConverter.toComponent(prefix);
        Object suffixComponent = ScoreboardComponentConverter.toComponent(suffix);
        sendTeamPacket(score, ScoreboardReflection.TeamMode.UPDATE, prefixComponent, suffixComponent);
    }

    private void sendScorePacket(int score, ScoreboardReflection.ScoreboardAction action) throws Throwable {
        Object customFormat = null;
        if (action == ScoreboardReflection.ScoreboardAction.CHANGE && ScoreboardReflection.customScoresSupported()) {
            String scoreText = getLineByScore(this.scores, score);
            customFormat = scoreText != null
                    ? ScoreboardReflection.fixedNumberFormat(ScoreboardComponentConverter.toComponent(scoreText))
                    : ScoreboardReflection.blankNumberFormat();
        }
        sendPacket(ScoreboardReflection.scorePacket(score, action, this.id, customFormat));
    }

    private void sendTeamPacket(int score, ScoreboardReflection.TeamMode mode) throws Throwable {
        sendTeamPacket(score, mode, null, null);
    }

    private void sendTeamPacket(int score, ScoreboardReflection.TeamMode mode, Object prefix, Object suffix) throws Throwable {
        String teamName = this.id + ":" + score;
        sendPacket(ScoreboardReflection.teamPacket(teamName, score, mode, prefix, suffix));
    }

    private void sendPacket(Object packet) throws Throwable {
        if (this.deleted) {
            throw new IllegalStateException("This scoreboard is deleted");
        }
        if (this.player.isOnline()) {
            ScoreboardReflection.sendPacket(this.player, packet);
        }
    }

    private void checkLineNumber(int line, boolean checkInRange, boolean checkMax) {
        if (line < 0) {
            throw new IllegalArgumentException("Line number must be positive");
        }
        if (checkInRange && line >= this.lines.size()) {
            throw new IllegalArgumentException("Line number must be under " + this.lines.size());
        }
        if (checkMax && line >= ScoreboardReflection.COLOR_CODES.length) {
            throw new IllegalArgumentException("Line number is too high: " + line);
        }
    }

    private int getScoreByLine(int line) {
        return this.lines.size() - line - 1;
    }

    private String getLineByScore(int score) {
        return getLineByScore(this.lines, score);
    }

    private String getLineByScore(List<String> lines, int score) {
        return score < lines.size() ? lines.get(lines.size() - score - 1) : null;
    }
}
