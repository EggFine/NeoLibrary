package com.blbilink.neoLibrary.bridge;

import com.blbilink.neoLibrary.utils.RichTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 套件共享零强依赖基岩版（Geyser / Floodgate + Cumulus）检测与原生表单桥接器。
 * <p>
 * 所有对 {@code org.geysermc.*} 的引用均严格隔离在静态内部类 {@link Impl} 中，
 * 确保未安装 Floodgate 的纯 Java 版服务端永远不会触发外部类加载错误。
 * </p>
 */
public final class BedrockBridge {

    public sealed interface FormElement permits LabelElement, InputElement, DropdownElement, ToggleElement {}

    public record LabelElement(String text) implements FormElement {}

    public record InputElement(String label, String placeholder, String defaultText) implements FormElement {}

    public record DropdownElement(String label, List<String> options, int defaultIndex) implements FormElement {}

    public record ToggleElement(String label, boolean defaultValue) implements FormElement {}

    public static final class CustomFormSpec {
        private final String title;
        private final List<FormElement> elements = new ArrayList<>();
        private Runnable closedOrInvalidHandler;

        private CustomFormSpec(String title) {
            this.title = RichTextUtil.stripColors(title);
        }

        public static CustomFormSpec builder(String title) {
            return new CustomFormSpec(title);
        }

        public CustomFormSpec label(String text) {
            elements.add(new LabelElement(RichTextUtil.stripColors(text)));
            return this;
        }

        public CustomFormSpec input(String label, String placeholder) {
            return input(label, placeholder, "");
        }

        public CustomFormSpec input(String label, String placeholder, String defaultText) {
            elements.add(new InputElement(
                    RichTextUtil.stripColors(label),
                    RichTextUtil.stripColors(placeholder),
                    defaultText != null ? RichTextUtil.stripColors(defaultText) : ""
            ));
            return this;
        }

        public CustomFormSpec dropdown(String label, List<String> options) {
            return dropdown(label, options, 0);
        }

        public CustomFormSpec dropdown(String label, List<String> options, int defaultIndex) {
            List<String> cleanOptions = options == null ? List.of() : options.stream()
                    .map(RichTextUtil::stripColors)
                    .toList();
            int safeDefault = cleanOptions.isEmpty() ? 0 : Math.max(0, Math.min(cleanOptions.size() - 1, defaultIndex));
            elements.add(new DropdownElement(RichTextUtil.stripColors(label), cleanOptions, safeDefault));
            return this;
        }

        public CustomFormSpec toggle(String label, boolean defaultValue) {
            elements.add(new ToggleElement(RichTextUtil.stripColors(label), defaultValue));
            return this;
        }

        public CustomFormSpec onClosedOrInvalid(Runnable handler) {
            this.closedOrInvalidHandler = handler;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public List<FormElement> getElements() {
            return Collections.unmodifiableList(elements);
        }

        public Runnable getClosedOrInvalidHandler() {
            return closedOrInvalidHandler;
        }
    }

    public static final class CustomFormValues {
        private final List<Object> values;
        private int cursor = 0;

        CustomFormValues(List<Object> values) {
            this.values = values;
        }

        public String nextString() {
            Object v = nextRaw();
            return v != null ? v.toString() : "";
        }

        public int nextDropdown() {
            Object v = nextRaw();
            return v instanceof Number n ? n.intValue() : 0;
        }

        public boolean nextToggle() {
            Object v = nextRaw();
            return Boolean.TRUE.equals(v);
        }

        private Object nextRaw() {
            if (cursor >= values.size()) {
                return null;
            }
            return values.get(cursor++);
        }
    }

    public static boolean isFloodgateAvailable() {
        return Bukkit.getServer() != null && Bukkit.getPluginManager().isPluginEnabled("floodgate");
    }

    public static boolean isFloodgatePlayer(UUID uuid) {
        if (uuid == null || !isFloodgateAvailable()) {
            return false;
        }
        try {
            return Impl.isFloodgatePlayer(uuid);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player, false, null);
    }

    public static boolean isBedrockPlayer(Player player, boolean checkFloodgateZeroUuid, String namePrefix) {
        if (player == null) {
            return false;
        }
        if (checkFloodgateZeroUuid && player.getUniqueId().toString().startsWith("00000000-0000-0000-")) {
            return true;
        }
        if (namePrefix != null && !namePrefix.isEmpty() && player.getName().startsWith(namePrefix)) {
            return true;
        }
        return isFloodgatePlayer(player.getUniqueId());
    }

    public static boolean sendSimpleForm(
            Player player,
            String title,
            String content,
            List<String> buttonLabels,
            IntConsumer onButtonClicked
    ) {
        if (player == null || buttonLabels == null || buttonLabels.isEmpty() || onButtonClicked == null || !isFloodgatePlayer(player.getUniqueId())) {
            return false;
        }
        try {
            List<String> strippedButtons = buttonLabels.stream().map(RichTextUtil::stripColors).toList();
            return Impl.sendSimple(
                    player.getUniqueId(),
                    RichTextUtil.stripColors(title),
                    RichTextUtil.stripColors(content),
                    strippedButtons,
                    onButtonClicked
            );
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean sendModalForm(
            Player player,
            String title,
            String content,
            String primaryButton,
            String secondaryButton,
            Runnable onPrimary,
            Runnable onSecondary
    ) {
        if (player == null || !isFloodgatePlayer(player.getUniqueId())) {
            return false;
        }
        try {
            return Impl.sendModal(
                    player.getUniqueId(),
                    RichTextUtil.stripColors(title),
                    RichTextUtil.stripColors(content),
                    RichTextUtil.stripColors(primaryButton),
                    RichTextUtil.stripColors(secondaryButton),
                    onPrimary,
                    onSecondary
            );
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean sendCustomForm(
            Player player,
            CustomFormSpec spec,
            Consumer<CustomFormValues> onSubmit
    ) {
        if (player == null || spec == null || onSubmit == null || !isFloodgatePlayer(player.getUniqueId())) {
            return false;
        }
        try {
            return Impl.sendCustom(player.getUniqueId(), spec, onSubmit);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class Impl {

        static boolean isFloodgatePlayer(UUID uuid) {
            FloodgateApi api = FloodgateApi.getInstance();
            return api != null && api.isFloodgatePlayer(uuid);
        }

        static boolean sendSimple(
                UUID uuid,
                String title,
                String content,
                List<String> buttons,
                IntConsumer onSelect
        ) {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null || !api.isFloodgatePlayer(uuid)) {
                return false;
            }
            SimpleForm.Builder builder = SimpleForm.builder().title(title).content(content);
            for (String btn : buttons) {
                builder.button(Objects.toString(btn, ""));
            }
            builder.validResultHandler(resp -> {
                int id = resp.clickedButtonId();
                if (id >= 0 && id < buttons.size()) {
                    onSelect.accept(id);
                }
            });
            return api.sendForm(uuid, builder.build());
        }

        static boolean sendModal(
                UUID uuid,
                String title,
                String content,
                String btn1,
                String btn2,
                Runnable onBtn1,
                Runnable onBtn2
        ) {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null || !api.isFloodgatePlayer(uuid)) {
                return false;
            }
            ModalForm form = ModalForm.builder()
                    .title(title)
                    .content(content)
                    .button1(btn1)
                    .button2(btn2)
                    .validResultHandler(resp -> {
                        if (resp.clickedButtonId() == 0) {
                            if (onBtn1 != null) onBtn1.run();
                        } else if (onBtn2 != null) {
                            onBtn2.run();
                        }
                    })
                    .build();
            return api.sendForm(uuid, form);
        }

        static boolean sendCustom(
                UUID uuid,
                CustomFormSpec spec,
                Consumer<CustomFormValues> onSubmit
        ) {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null || !api.isFloodgatePlayer(uuid)) {
                return false;
            }
            CustomForm.Builder builder = CustomForm.builder().title(spec.getTitle());
            List<FormElement> interactiveElements = new ArrayList<>();
            for (FormElement el : spec.getElements()) {
                if (el instanceof LabelElement lbl) {
                    builder.label(lbl.text());
                } else if (el instanceof InputElement inp) {
                    if (inp.defaultText() != null && !inp.defaultText().isEmpty()) {
                        builder.input(inp.label(), inp.placeholder(), inp.defaultText());
                    } else {
                        builder.input(inp.label(), inp.placeholder());
                    }
                    interactiveElements.add(inp);
                } else if (el instanceof DropdownElement dd) {
                    builder.dropdown(dd.label(), dd.options(), dd.defaultIndex());
                    interactiveElements.add(dd);
                } else if (el instanceof ToggleElement tg) {
                    builder.toggle(tg.label(), tg.defaultValue());
                    interactiveElements.add(tg);
                }
            }
            builder.validResultHandler(resp -> {
                List<Object> parsed = new ArrayList<>(interactiveElements.size());
                for (FormElement el : interactiveElements) {
                    if (el instanceof InputElement) {
                        parsed.add(resp.asInput());
                    } else if (el instanceof DropdownElement) {
                        parsed.add(resp.asDropdown());
                    } else if (el instanceof ToggleElement) {
                        parsed.add(resp.asToggle());
                    }
                }
                onSubmit.accept(new CustomFormValues(parsed));
            });
            if (spec.getClosedOrInvalidHandler() != null) {
                builder.closedOrInvalidResultHandler(() -> spec.getClosedOrInvalidHandler().run());
            }
            return api.sendForm(uuid, builder.build());
        }
    }
}
