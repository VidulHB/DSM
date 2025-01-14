package me.Danker.features;

import me.Danker.DankersSkyblockMod;
import me.Danker.commands.MoveCommand;
import me.Danker.commands.ScaleCommand;
import me.Danker.commands.ToggleCommand;
import me.Danker.events.RenderOverlayEvent;
import me.Danker.handlers.TextRenderer;
import me.Danker.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;

public class DungeonScore {

    int failedPuzzles;
    int deaths;
    int skillScore;
    String secrets;
    int exploreScore;
    int timeScore;
    int bonusScore;

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (!ToggleCommand.dungeonScore || !Utils.inDungeons) return;

        if (message.contains("PUZZLE FAIL! ") || message.contains("chose the wrong answer! I shall never forget this moment")) {
            failedPuzzles++;
        }

        if (message.contains(":")) return;

        if (message.contains(" and became a ghost.")) {
            deaths++;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (!ToggleCommand.dungeonScore || !Utils.inDungeons) return;

        if (DankersSkyblockMod.tickAmount % 20 == 0) {
            int missingPuzzles = 0;
            double openedRooms = 0;
            double completedRooms = 0;
            double roomScore = 0;
            double secretScore = 0;

            if (Minecraft.getMinecraft().getNetHandler() == null) return;
            Collection<NetworkPlayerInfo> players = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap();
            for (NetworkPlayerInfo player : players) {
                if (player == null || player.getDisplayName() == null) continue;
                String display = player.getDisplayName().getUnformattedText();

                if (display.startsWith(" Opened Rooms: ")) {
                    openedRooms = Double.parseDouble(display.replaceAll("[^\\d]", ""));
                } else if (display.startsWith(" Completed Rooms: ")) {
                    completedRooms = Double.parseDouble(display.replaceAll("[^\\d]", ""));
                } else if (display.startsWith(" Secrets Found: ") && display.endsWith("%")) {
                    secrets = player.getDisplayName().getFormattedText();

                    double secretCount = Double.parseDouble(display.replaceAll("[^\\d.]", ""));

                    switch (Utils.currentFloor) {
                        case F1:
                            secretScore = secretCount / 30D;
                            break;
                        case F2:
                            secretScore = secretCount / 40D;
                            break;
                        case F3:
                            secretScore = secretCount / 50D;
                            break;
                        case F4:
                            secretScore = secretCount / 60D;
                            break;
                        case F5:
                            secretScore = secretCount / 70D;
                            break;
                        case F6:
                            secretScore = secretCount / 85D;
                            break;
                        default:
                            secretScore = secretCount / 100D;
                    }
                } else if (display.startsWith("Time Elapsed: ")) {
                    String timeText = display.substring(display.indexOf(":") + 2).replaceAll("\\s", "");
                    int minutes = Integer.parseInt(timeText.substring(0, timeText.indexOf("m")));
                    int seconds = Integer.parseInt(timeText.substring(timeText.indexOf("m") + 1, timeText.indexOf("s")));
                    int time = minutes * 60 + seconds;

                    if (Utils.currentFloor == Utils.DungeonFloor.F2) time -= 120;

                    int base;
                    switch (Utils.currentFloor) {
                        case F1:
                        case F2:
                        case F3:
                        case F5:
                            base = 600;
                            break;
                        case F4:
                        case F6:
                        case F7:
                            base = 720;
                            break;
                        default:
                            base = 480;
                    }

                    if (time <= base) {
                        timeScore = 100;
                    } else if (time <= base + 100) {
                        timeScore = (int) Math.ceil(100 - 0.1 * (time - base));
                    } else if (time <= base + 500) {
                        timeScore = (int) Math.ceil(90 - 0.05 * (time - base - 100));
                    } else if (time < base + 2600) {
                        timeScore = (int) Math.ceil(70 - (1/30D) * (time - base - 1100));
                    } else {
                        timeScore = 0;
                    }
                } else if (display.startsWith(" Crypts: ")) {
                    bonusScore = MathHelper.clamp_int(Integer.parseInt(display.replaceAll("[^\\d]", "")), 0, 5);
                } else if (display.contains("[✦]")) {
                    missingPuzzles++;
                }
            }

            if (openedRooms != 0) {
                roomScore = completedRooms / openedRooms;
            }

            skillScore = MathHelper.clamp_int(100 - 14 * (failedPuzzles + missingPuzzles) - 2 * deaths, 0, 100);
            exploreScore = (int) (60 * roomScore + 40 * MathHelper.clamp_double(secretScore, 0, 1));
        }
    }

    @SubscribeEvent
    public void renderPlayerInfo(RenderOverlayEvent event) {
        if (ToggleCommand.dungeonScore && Utils.inDungeons) {
            Minecraft mc = Minecraft.getMinecraft();

            int totalScore = skillScore + exploreScore + timeScore + bonusScore;
            String total;
            if (totalScore >= 300) {
                total = EnumChatFormatting.GOLD + "S+";
            } else if (totalScore >= 270) {
                total = EnumChatFormatting.GOLD + "S";
            } else if (totalScore >= 230) {
                total = EnumChatFormatting.DARK_PURPLE + "A";
            } else if (totalScore >= 160) {
                total = EnumChatFormatting.GREEN + "B";
            } else if (totalScore >= 100) {
                total = EnumChatFormatting.BLUE + "C";
            } else {
                total = EnumChatFormatting.RED + "D";
            }

            String scoreText = secrets + "\n" +
                    EnumChatFormatting.GOLD + "Skill:\n" +
                    EnumChatFormatting.GOLD + "Explore:\n" +
                    EnumChatFormatting.GOLD + "Speed:\n" +
                    EnumChatFormatting.GOLD + "Bonus:\n" +
                    EnumChatFormatting.GOLD + "Total:";
            String score = "\n" +
                    EnumChatFormatting.GOLD + skillScore + "\n" +
                    EnumChatFormatting.GOLD + exploreScore + "\n" +
                    EnumChatFormatting.GOLD + timeScore + "\n" +
                    EnumChatFormatting.GOLD + bonusScore + "\n" +
                    EnumChatFormatting.GOLD + totalScore + EnumChatFormatting.GRAY + " (" + total + EnumChatFormatting.GRAY + ")";
            new TextRenderer(mc, scoreText, MoveCommand.dungeonScoreXY[0], MoveCommand.dungeonScoreXY[1], ScaleCommand.dungeonScoreScale);
            new TextRenderer(mc, score, MoveCommand.dungeonScoreXY[0] + 80, MoveCommand.dungeonScoreXY[1], ScaleCommand.dungeonScoreScale);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        failedPuzzles = 0;
        deaths = 0;
        skillScore = 100;
        secrets = "";
        exploreScore = 0;
        timeScore = 100;
        bonusScore = 0;
    }

}
