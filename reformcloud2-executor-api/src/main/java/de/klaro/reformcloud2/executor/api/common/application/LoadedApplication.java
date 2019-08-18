package de.klaro.reformcloud2.executor.api.common.application;

import de.klaro.reformcloud2.executor.api.common.ExecutorAPI;
import de.klaro.reformcloud2.executor.api.common.utility.annotiations.Nullable;
import de.klaro.reformcloud2.executor.api.common.utility.name.Nameable;

public interface LoadedApplication extends Nameable {

    ApplicationLoader loader();

    ExecutorAPI api();

    ApplicationConfig applicationConfig();

    ApplicationStatus applicationStatus();

    @Nullable Class<?> mainClass();

    void setApplicationStatus(ApplicationStatus status);

    @Override
    default String getName() {
        return applicationConfig().getName();
    }
}