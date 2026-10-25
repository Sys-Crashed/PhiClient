package org.dpdns.syscrashed.phiclient.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.storage.WorldInfo;

/**
 * The values of the running game that the tip card shows
 * (package {@code org.dpdns.syscrashed.phiclient.utils}).
 *
 * <p>Every method works without a world as well - the interface can be opened on the title screen of
 * the game, where there is no player and no world. In that case the methods return a placeholder
 * instead of throwing.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getTime()} - the time of the game world.</li>
 *   <li>{@link #getWeather()} - the weather of the game world.</li>
 *   <li>{@link #getDimension()} - the dimension the player is in.</li>
 *   <li>{@link #getCoordinates()} - the position of the player.</li>
 *   <li>{@link #getServerAddress()} - the address of the server.</li>
 *   <li>{@link #copy(String)} - copies a text into the clipboard.</li>
 * </ul>
 */
public final class PhiGameInfo {

    /** The length of a day in ticks. */
    private static final long DAY_LENGTH = 24000L;

    /** The tick of the day at which the sun rises. */
    private static final long SUNRISE = 6000L;

    /** The text that is shown when the game is not running. */
    private static final String UNKNOWN = "-";

    private PhiGameInfo() {
    }

    /**
     * @return the time of the game world in the format {@code HH:mm}.
     */
    public static String getTime() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld == null) {
            return UNKNOWN;
        }

        long dayTime = mc.theWorld.getWorldTime() % DAY_LENGTH;
        int hours = (int) ((dayTime + SUNRISE) % DAY_LENGTH / 1000L);
        int minutes = (int) (dayTime % 1000L * 60L / 1000L);

        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * @return the weather of the game world (clear, rain or thunderstorm).
     */
    public static String getWeather() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld == null) {
            return UNKNOWN;
        }

        WorldInfo info = mc.theWorld.getWorldInfo();

        if (info.isThundering()) {
            return "Thunder";
        }

        return info.isRaining() ? "Rain" : "Clear";
    }

    /**
     * @return the dimension the player is in (overworld, nether or end).
     */
    public static String getDimension() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.thePlayer == null) {
            return UNKNOWN;
        }

        switch (mc.thePlayer.dimension) {
            case 1:
                return "Nether";
            case 2:
                return "The End";
            default:
                return "Overworld";
        }
    }

    /**
     * @return the position of the player in the format {@code x, y, z}.
     */
    public static String getCoordinates() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.thePlayer == null) {
            return UNKNOWN;
        }

        BlockPos position = mc.thePlayer.getPosition();

        return position.getX() + ", " + position.getY() + ", " + position.getZ();
    }

    /**
     * @return the address of the server the player is on (or "Singleplayer").
     */
    public static String getServerAddress() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.isSingleplayer()) {
            return "Singleplayer";
        }

        if (mc.getCurrentServerData() == null || mc.getCurrentServerData().serverIP == null) {
            return UNKNOWN;
        }

        return mc.getCurrentServerData().serverIP;
    }

    /**
     * Copies a text into the clipboard of the system.
     *
     * @param text the text to copy.
     * @return true if the text could be copied.
     */
    public static boolean copy(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return true;
        } catch (Throwable t) {
            // The clipboard is not available on every system (for example without a display).
            return false;
        }
    }
}