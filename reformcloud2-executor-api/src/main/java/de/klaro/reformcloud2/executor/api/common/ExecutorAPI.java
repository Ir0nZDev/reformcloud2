package de.klaro.reformcloud2.executor.api.common;

import de.klaro.reformcloud2.executor.api.ExecutorType;
import de.klaro.reformcloud2.executor.api.common.base.Conditions;

public abstract class ExecutorAPI {

    protected ExecutorType type;

    /* ========================== */

    private static ExecutorAPI instance;

    public static void setInstance(ExecutorAPI instance) {
        Conditions.isTrue(ExecutorAPI.instance == null, "Executor api instance is already defined");
        ExecutorAPI.instance = instance;
    }

    public static ExecutorAPI getInstance() {
        return instance;
    }

    /* ========================== */

    public ExecutorType getType() {
        return type;
    }
}
