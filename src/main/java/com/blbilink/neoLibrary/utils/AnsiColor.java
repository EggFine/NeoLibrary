package com.blbilink.neoLibrary.utils;

/**
 * 用于在支持 ANSI Escape Code 的终端中显示彩色文本和格式的工具类。
 * <p>
 * 使用方法: System.out.println(AnsiColor.RED + "这段文本是红色的" + AnsiColor.RESET);
 * </p>
 */
public class AnsiColor {

    // --- 控制代码 ---

    /**
     * 重置所有文本格式和颜色，恢复到终端默认样式。应在每次颜色或格式使用完毕后调用。
     */
    public static final String RESET = "\u001B[0m";

    // --- 文本格式 ---

    /**
     * 使文本变为粗体或高亮，具体效果取决于终端的支持情况。
     */
    public static final String BOLD = "\u001B[1m";
    /**
     * 使文本变为斜体，并非所有终端都支持此效果。
     */
    public static final String ITALIC = "\u001B[3m";
    /**
     * 为文本添加下划线。
     */
    public static final String UNDERLINE = "\u001B[4m";
    /**
     * 为文本添加删除线，并非所有终端都支持此效果。
     */
    public static final String STRIKETHROUGH = "\u001B[9m";


    // --- 标准前景颜色 (8色) ---

    /**
     * 将文字颜色设置为黑色。
     */
    public static final String BLACK = "\u001B[30m";
    /**
     * 将文字颜色设置为深蓝色。
     */
    public static final String DARK_BLUE = "\u001B[34m";
    /**
     * 将文字颜色设置为深绿色。
     */
    public static final String DARK_GREEN = "\u001B[32m";
    /**
     * 将文字颜色设置为深水绿色（青色/蓝绿色）。
     */
    public static final String DARK_AQUA = "\u001B[36m";
    /**
     * 将文字颜色设置为深红色。
     */
    public static final String DARK_RED = "\u001B[31m";
    /**
     * 将文字颜色设置为深紫色（洋红色）。
     */
    public static final String DARK_PURPLE = "\u001B[35m";
    /**
     * 将文字颜色设置为金色（黄色/棕色）。
     */
    public static final String GOLD = "\u001B[33m";
    /**
     * 将文字颜色设置为灰色。
     */
    public static final String GRAY = "\u001B[37m";


    // --- 高亮/明亮前景颜色 (8色) ---

    /**
     * 将文字颜色设置为深灰色（亮黑色）。
     */
    public static final String DARK_GRAY = "\u001B[90m";
    /**
     * 将文字颜色设置为亮蓝色。
     */
    public static final String BLUE = "\u001B[94m";
    /**
     * 将文字颜色设置为亮绿色。
     */
    public static final String GREEN = "\u001B[92m";
    /**
     * 将文字颜色设置为亮水绿色（亮青色）。
     */
    public static final String AQUA = "\u001B[96m";
    /**
     * 将文字颜色设置为亮红色。
     */
    public static final String RED = "\u001B[91m";
    /**
     * 将文字颜色设置为亮紫色（亮洋红色）。
     */
    public static final String LIGHT_PURPLE = "\u001B[95m";
    /**
     * 将文字颜色设置为亮黄色。
     */
    public static final String YELLOW = "\u001B[93m";
    /**
     * 将文字颜色设置为白色（亮白色）。
     */
    public static final String WHITE = "\u001B[97m";


    // --- 标准背景颜色 (8色) ---

    /**
     * 将文字背景设置为黑色。
     */
    public static final String BACKGROUND_BLACK = "\u001B[40m";
    /**
     * 将文字背景设置为红色。
     */
    public static final String BACKGROUND_RED = "\u001B[41m";
    /**
     * 将文字背景设置为绿色。
     */
    public static final String BACKGROUND_GREEN = "\u001B[42m";
    /**
     * 将文字背景设置为黄色。
     */
    public static final String BACKGROUND_YELLOW = "\u001B[43m";
    /**
     * 将文字背景设置为蓝色。
     */
    public static final String BACKGROUND_BLUE = "\u001B[44m";
    /**
     * 将文字背景设置为紫色。
     */
    public static final String BACKGROUND_PURPLE = "\u001B[45m";
    /**
     * 将文字背景设置为青色（蓝绿色）。
     */
    public static final String BACKGROUND_CYAN = "\u001B[46m";
    /**
     * 将文字背景设置为白色（浅灰色）。
     */
    public static final String BACKGROUND_WHITE = "\u001B[47m";


    // --- 高亮/明亮背景颜色 (8色) ---

    /**
     * 将文字背景设置为深灰色（亮黑色）。
     */
    public static final String BRIGHT_BACKGROUND_BLACK = "\u001B[100m";
    /**
     * 将文字背景设置为亮红色。
     */
    public static final String BRIGHT_BACKGROUND_RED = "\u001B[101m";
    /**
     * 将文字背景设置为亮绿色。
     */
    public static final String BRIGHT_BACKGROUND_GREEN = "\u001B[102m";
    /**
     * 将文字背景设置为亮黄色。
     */
    public static final String BRIGHT_BACKGROUND_YELLOW = "\u001B[103m";
    /**
     * 将文字背景设置为亮蓝色。
     */
    public static final String BRIGHT_BACKGROUND_BLUE = "\u001B[104m";
    /**
     * 将文字背景设置为亮紫色。
     */
    public static final String BRIGHT_BACKGROUND_PURPLE = "\u001B[105m";
    /**
     * 将文字背景设置为亮青色。
     */
    public static final String BRIGHT_BACKGROUND_CYAN = "\u001B[106m";
    /**
     * 将文字背景设置为白色。
     */
    public static final String BRIGHT_BACKGROUND_WHITE = "\u001B[107m";
}
