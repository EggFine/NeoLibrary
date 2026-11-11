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
 * 套件共享富文本与交互式组件工具（原生支持 {@code &} 颜色码、{@code &#RRGGBB} 十六进制真彩色、ActionBar、可点击聊天按钮及进度条渲染）。
 */
public final class RichTextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private RichTextUtil() {
    }

    /**
     * 将包含 {@code &#RRGGBB} 十六进制色彩和传统 {@code &} 颜色码的文本转换为 Minecraft 客户端标准格式。
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder(input.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (int i = 0; i < 6; i++) {
                replacement.append('§').append(Character.toLowerCase(hex.charAt(i)));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * 剥离所有传统颜色码与十六进制色彩标记，返回纯文本。
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
     * 向玩家发送带“接受 / 拒绝”双可点击按钮的聊天交互卡片（如 {@code /tpa} 与 {@code /tpahere} 请求）。
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
     * 生成紧凑可视化分段进度条（用于传送预热倒计时、世界重置进度等）。
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
        TextComponent btn = new TextComponent(TextComponent.fromLegacyText(colorize(label)));
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hoverTip != null && !hoverTip.isEmpty()) {
            btn.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(colorize(hoverTip)))
            ));
        }
        return btn;
    }
}
