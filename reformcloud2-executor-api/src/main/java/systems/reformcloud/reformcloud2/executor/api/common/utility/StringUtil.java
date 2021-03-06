package systems.reformcloud.reformcloud2.executor.api.common.utility;

import systems.reformcloud.reformcloud2.executor.api.common.base.Conditions;
import systems.reformcloud.reformcloud2.executor.api.common.language.LanguageManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

public final class StringUtil {

    public static final String RUNNER_DOWNLOAD_URL = "https://internal.reformcloud.systems/runner.jar";

    public static final String NULL_PATH = new File("reformcloud/.bin/dev/null").getAbsolutePath();

    public static void sendHeader() {
        if (Boolean.getBoolean("reformcloud.disable.header.show")) {
            System.out.println();
            return;
        }

        System.out.println(
                        "\n" +
                        "    __       __                        ___ _                 _ ____  \n" +
                        "   /__\\ ___ / _| ___  _ __ _ __ ___   / __\\ | ___  _   _  __| |___ \\ \n" +
                        "  / \\/// _ \\ |_ / _ \\| '__| '_ ` _ \\ / /  | |/ _ \\| | | |/ _` | __) |\n" +
                        " / _  \\  __/  _| (_) | |  | | | | | / /___| | (_) | |_| | (_| |/ __/\n" +
                        " \\/ \\_/\\___|_|  \\___/|_|  |_| |_| |_\\____/|_|\\___/ \\__,_|\\__,_|_____| git:"
                                + StringUtil.class.getPackage().getSpecificationVersion() + "\n" +
                        " \n" +
                        "                   Not just a cloud system, but an experience.\n"
        );
    }

    public static String generateString(int times) {
        Conditions.isTrue(times > 0);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            stringBuilder.append(UUID.randomUUID().toString().replace("-", ""));
        }

        return stringBuilder.toString();
    }

    public static String getConsolePrompt() {
        return LanguageManager.get("logger.console.prompt")
                .replace("%version%", System.getProperty("reformcloud.runner.version", "c-build"))
                .replace("%user_name%", System.getProperty("user.name", "unknown")) + " ";
    }

    public static String formatError(@Nonnull String error) {
        return String.format("Unable to process action %s. Please report this DIRECTLY to reformcloud it is a fatal error", error);
    }

    @Nonnull
    public static Properties calcProperties(@Nonnull String[] strings, int from) {
        Properties properties = new Properties();
        if (strings.length < from) {
            return properties;
        }

        String[] copy = Arrays.copyOfRange(strings, from, strings.length);
        for (String string : copy) {
            if (!string.startsWith("--") && !string.contains("=")) {
                continue;
            }

            String[] split = string.replaceFirst("--", "").split("=");
            if (split.length != 2) {
                continue;
            }

            properties.setProperty(split[0], split[1]);
        }

        return properties;
    }
}
