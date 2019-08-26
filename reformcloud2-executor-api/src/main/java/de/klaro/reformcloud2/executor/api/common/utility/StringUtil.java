package de.klaro.reformcloud2.executor.api.common.utility;

import de.klaro.reformcloud2.executor.api.common.base.Conditions;

import java.util.UUID;

public final class StringUtil {

    public static void sendHeader() {
        System.out.println(
                "   __       __                        ___ _                 _ ____  \n" +
                "  /__\\ ___ / _| ___  _ __ _ __ ___   / __\\ | ___  _   _  __| |___ \\ \n" +
                " / \\/// _ \\ |_ / _ \\| '__| '_ ` _ \\ / /  | |/ _ \\| | | |/ _` | __) |\n" +
                "/ _  \\  __/  _| (_) | |  | | | | | / /___| | (_) | |_| | (_| |/ __/ \n" +
                "\\/ \\_/\\___|_|  \\___/|_|  |_| |_| |_\\____/|_|\\___/ \\__,_|\\__,_|_____|\n" +
                "\n" +
                "                  Not just a cloud system, but an experience.\n"
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
}
