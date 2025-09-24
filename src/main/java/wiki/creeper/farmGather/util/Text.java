package wiki.creeper.farmGather.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .build();

    private Text() {
    }

    public static Component colorize(String message) {
        if (message == null || message.isBlank()) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(message);
    }

    public static TextComponent error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }
}
