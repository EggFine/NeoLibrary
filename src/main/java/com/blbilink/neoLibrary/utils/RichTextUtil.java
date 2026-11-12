package com.blbilink.neoLibrary.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 套件共享富文本与交互式组件工具（原生支持 {@code &} 颜色码、{@code &#RRGGBB} 十六进制真彩色、
 * {@code <gradient:#RRGGBB:#RRGGBB>...</gradient>} 多段真彩渐变、{@code <rainbow>...</rainbow>} 彩虹字、
 * ActionBar、可点击聊天组件及进度条渲染）。
 */
public final class RichTextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile(
            "<gradient:(#[A-Fa-f0-9]{6}(?::#[A-Fa-f0-9]{6})+)>([\\s\\S]*?)</gradient>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RAINBOW_PATTERN = Pattern.compile(
            "<rainbow>([\\s\\S]*?)</rainbow>",
            Pattern.CASE_INSENSITIVE
    );

    private RichTextUtil() {
    }

    /**
     * 将包含 {@code <gradient:...>}、{@code <rainbow>}、{@code &#RRGGBB} 和传统 {@code &} 颜色码的文本转换为客户端格式。
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String stage = applyGradients(applyRainbows(input));
        Matcher matcher = HEX_PATTERN.matcher(stage);
        StringBuilder buffer = new StringBuilder(stage.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(toSectionHex(hex)));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * 剥离所有传统颜色码、渐变、彩虹标记与十六进制色彩标记，返回纯文本。
     */
    public static String stripColors(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String stripped = ChatColor.stripColor(colorize(input));
        return stripped != null ? stripped : input;
    }

    /**
     * 向玩家发送跨平台兼容的 ActionBar 提示。
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) {
            return;
        }
        BaseComponent[] components = TextComponent.fromLegacyText(colorize(message));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
    }

    /**
     * 构建可复用的富文本悬浮/点击组件（无 NMS，100% 兼容 Spigot / Paper / Folia）。
     */
    public static TextComponent buildInteractiveText(
            String displayText,
            String hoverTip,
            ClickEvent.Action clickAction,
            String clickValue
    ) {
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(colorize(displayText)));
        if (clickAction != null && clickValue != null && !clickValue.isEmpty()) {
            component.setClickEvent(new ClickEvent(clickAction, clickValue));
        }
        if (hoverTip != null && !hoverTip.isEmpty()) {
            component.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(colorize(hoverTip)))
            ));
        }
        return component;
    }

    /**
     * 向在线玩家安全发送组合富文本组件链。
     */
    public static void sendComponents(Player player, BaseComponent... components) {
        if (player == null || !player.isOnline() || components == null || components.length == 0) {
            return;
        }
        player.spigot().sendMessage(components);
    }

    /**
     * 向玩家发送带单个可点击交互按钮的消息（如危险落点二次确认按钮）。
     */
    public static void sendClickableOption(
            Player player,
            String leadingMessage,
            String buttonLabel,
            String hoverTip,
            String commandToRun
    ) {
        if (player == null || !player.isOnline()) {
            return;
        }
        TextComponent root = new TextComponent();
        for (BaseComponent c : TextComponent.fromLegacyText(colorize(leadingMessage + " "))) {
            root.addExtra(c);
        }
        root.addExtra(buildButton(buttonLabel, hoverTip, commandToRun));
        player.spigot().sendMessage(root);
    }

    /**
     * 向玩家发送带“接受 / 拒绝”双可点击按钮的聊天交互卡片（如 {@code /tpa} 与 {@code /is invite} 请求）。
     */
    public static void sendAcceptDenyButtons(
            Player player,
            String leadingText,
            String acceptLabel,
            String acceptHover,
            String acceptCommand,
            String denyLabel,
            String denyHover,
            String denyCommand
    ) {
        if (player == null || !player.isOnline()) {
            return;
        }
        TextComponent root = new TextComponent();
        if (leadingText != null && !leadingText.isEmpty()) {
            for (BaseComponent c : TextComponent.fromLegacyText(colorize(leadingText + " "))) {
                root.addExtra(c);
            }
        }
        root.addExtra(buildButton(acceptLabel, acceptHover, acceptCommand));
        root.addExtra(new TextComponent("  "));
        root.addExtra(buildButton(denyLabel, denyHover, denyCommand));
        player.spigot().sendMessage(root);
    }

    /**
     * 生成紧凑可视化分段进度条（用于传送预热倒计时、世界重置、单方块阶段进度等）。
     */
    public static String buildProgressBar(int current, int total, int segments, String activeColor, String inactiveColor) {
        int safeSegments = Math.max(1, segments);
        int filled = total <= 0 ? safeSegments : Math.max(0, Math.min(safeSegments, Math.round((float) current / total * safeSegments)));
        StringBuilder sb = new StringBuilder(safeSegments * 4);
        sb.append(activeColor != null ? activeColor : "&a");
        for (int i = 0; i < filled; i++) {
            sb.append('■');
        }
        sb.append(inactiveColor != null ? inactiveColor : "&8");
        for (int i = filled; i < safeSegments; i++) {
            sb.append('■');
        }
        return colorize(sb.toString());
    }

    private static TextComponent buildButton(String label, String hoverTip, String command) {
        return buildInteractiveText(label, hoverTip, ClickEvent.Action.RUN_COMMAND, command);
    }

    private static String applyGradients(String text) {
        if (!text.contains("<gradient:") && !text.contains("<GRADIENT:")) {
            return text;
        }
        Matcher m = GRADIENT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 64);
        while (m.find()) {
            String[] stopTokens = m.group(1).split(":");
            int[][] stops = new int[stopTokens.length][3];
            for (int i = 0; i < stopTokens.length; i++) {
                String hex = stopTokens[i].substring(1);
                stops[i][0] = Integer.parseInt(hex.substring(0, 2), 16);
                stops[i][1] = Integer.parseInt(hex.substring(2, 4), 16);
                stops[i][2] = Integer.parseInt(hex.substring(4, 6), 16);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(interpolateStops(m.group(2), stops)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String applyRainbows(String text) {
        if (!text.contains("<rainbow>") && !text.contains("<RAINBOW>")) {
            return text;
        }
        int[][] rainbowStops = {
                {255, 85, 85}, {255, 170, 0}, {255, 255, 85},
                {85, 255, 85}, {85, 255, 255}, {85, 85, 255}, {255, 85, 255}
        };
        Matcher m = RAINBOW_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 64);
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(interpolateStops(m.group(1), rainbowStops)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String interpolateStops(String content, int[][] stops) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        int visibleCount = 0;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if ((ch == '&' || ch == '§') && i + 1 < content.length() && "klmnorKLMNOR".indexOf(content.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            if (!Character.isWhitespace(ch)) {
                visibleCount++;
            }
        }
        if (visibleCount == 0) {
            return content;
        }
        StringBuilder out = new StringBuilder(content.length() * 14);
        String activeFormats = "";
        int index = 0;
        int segments = stops.length - 1;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if ((ch == '&' || ch == '§') && i + 1 < content.length()) {
                char code = Character.toLowerCase(content.charAt(i + 1));
                if (code == 'r') {
                    activeFormats = "";
                    i++;
                    continue;
                }
                if ("klmno".indexOf(code) >= 0) {
                    activeFormats = activeFormats + "&" + code;
                    i++;
                    continue;
                }
            }
            if (Character.isWhitespace(ch)) {
                out.append(ch);
                continue;
            }
            double ratio = visibleCount <= 1 ? 0.0d : (double) index / (visibleCount - 1);
            double scaled = ratio * segments;
            int segIdx = Math.min(segments - 1, (int) Math.floor(scaled));
            double localT = scaled - segIdx;
            int r = (int) Math.round(stops[segIdx][0] + (stops[segIdx + 1][0] - stops[segIdx][0]) * localT);
            int g = (int) Math.round(stops[segIdx][1] + (stops[segIdx + 1][1] - stops[segIdx][1]) * localT);
            int b = (int) Math.round(stops[segIdx][2] + (stops[segIdx + 1][2] - stops[segIdx][2]) * localT);
            out.append(String.format("&#%02X%02X%02X", clampByte(r), clampByte(g), clampByte(b)))
                    .append(activeFormats)
                    .append(ch);
            index++;
        }
        return out.toString();
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String toSectionHex(String hex) {
        StringBuilder replacement = new StringBuilder("§x");
        for (int i = 0; i < 6; i++) {
            replacement.append('§').append(Character.toLowerCase(hex.charAt(i)));
        }
        return replacement.toString();
    }
}
