package systems.reformcloud.reformcloud2.executor.controller.api.group;

import systems.reformcloud.reformcloud2.executor.api.common.api.group.GroupAsyncAPI;
import systems.reformcloud.reformcloud2.executor.api.common.api.group.GroupSyncAPI;
import systems.reformcloud.reformcloud2.executor.api.common.base.Conditions;
import systems.reformcloud.reformcloud2.executor.api.common.groups.MainGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.ProcessGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.Template;
import systems.reformcloud.reformcloud2.executor.api.common.groups.utils.PlayerAccessConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.groups.utils.StartupConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.groups.utils.StartupEnvironment;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.defaults.DefaultTask;
import systems.reformcloud.reformcloud2.executor.controller.ControllerExecutor;
import systems.reformcloud.reformcloud2.executor.controller.config.ControllerExecutorConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupAPIImplementation implements GroupAsyncAPI, GroupSyncAPI {

    private final ControllerExecutorConfig controllerExecutorConfig = ControllerExecutor.getInstance().getControllerExecutorConfig();

    @Nonnull
    @Override
    public Task<MainGroup> createMainGroupAsync(@Nonnull String name) {
        return createMainGroupAsync(name, new ArrayList<>());
    }

    @Nonnull
    @Override
    public Task<MainGroup> createMainGroupAsync(@Nonnull String name, @Nonnull List<String> subgroups) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            MainGroup mainGroup = new MainGroup(name, subgroups);
            task.complete(controllerExecutorConfig.createMainGroup(mainGroup));
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull String name) {
        return createProcessGroupAsync(name, new ArrayList<>());
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull String name, @Nonnull List<Template> templates) {
        return createProcessGroupAsync(name, templates, new StartupConfiguration(
                -1, 1, 1, templates.isEmpty() ? 41000 : templates.get(0).getVersion().getDefaultPort(),
                StartupEnvironment.JAVA_RUNTIME, true, new ArrayList<>()
        ));
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration) {
        return createProcessGroupAsync(name, templates, startupConfiguration,  new PlayerAccessConfiguration(
                "reformcloud.join.full",
                false,
                "reformcloud.join.maintenance",
                false,
                null,
                true,
                true,
                true,
                50
        ));
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration, @Nonnull PlayerAccessConfiguration playerAccessConfiguration) {
        return createProcessGroupAsync(name, templates, startupConfiguration, playerAccessConfiguration, false);
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration, @Nonnull PlayerAccessConfiguration playerAccessConfiguration, boolean staticGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessGroup processGroup = new ProcessGroup(
                    name,
                    true,
                    startupConfiguration,
                    templates,
                    playerAccessConfiguration,
                    staticGroup
            );
            task.complete(createProcessGroupAsync(processGroup).getUninterruptedly());
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> createProcessGroupAsync(@Nonnull ProcessGroup processGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(controllerExecutorConfig.createProcessGroup(processGroup)));
        return task;
    }

    @Nonnull
    @Override
    public Task<MainGroup> updateMainGroupAsync(@Nonnull MainGroup mainGroup) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            controllerExecutorConfig.updateMainGroup(mainGroup);
            task.complete(mainGroup);
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> updateProcessGroupAsync(@Nonnull ProcessGroup processGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            controllerExecutorConfig.updateProcessGroup(processGroup);
            task.complete(processGroup);
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<MainGroup> getMainGroupAsync(@Nonnull String name) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Streams.filter(controllerExecutorConfig.getMainGroups(), mainGroup -> mainGroup.getName().equals(name))));
        return task;
    }

    @Nonnull
    @Override
    public Task<ProcessGroup> getProcessGroupAsync(@Nonnull String name) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Streams.filter(controllerExecutorConfig.getProcessGroups(), processGroup -> processGroup.getName().equals(name))));
        return task;
    }

    @Nonnull
    @Override
    public Task<Void> deleteMainGroupAsync(@Nonnull String name) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            Streams.filterToReference(controllerExecutorConfig.getMainGroups(), mainGroup -> mainGroup.getName().equals(name)).ifPresent(controllerExecutorConfig::deleteMainGroup);
            task.complete(null);
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<Void> deleteProcessGroupAsync(@Nonnull String name) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            Streams.filterToReference(controllerExecutorConfig.getProcessGroups(), processGroup -> processGroup.getName().equals(name)).ifPresent(controllerExecutorConfig::deleteProcessGroup);
            task.complete(null);
        });
        return task;
    }

    @Nonnull
    @Override
    public Task<List<MainGroup>> getMainGroupsAsync() {
        Task<List<MainGroup>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Collections.unmodifiableList(controllerExecutorConfig.getMainGroups())));
        return task;
    }

    @Nonnull
    @Override
    public Task<List<ProcessGroup>> getProcessGroupsAsync() {
        Task<List<ProcessGroup>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Collections.unmodifiableList(controllerExecutorConfig.getProcessGroups())));
        return task;
    }

    @Nonnull
    @Override
    public MainGroup createMainGroup(@Nonnull String name) {
        MainGroup mainGroup = createMainGroupAsync(name).getUninterruptedly();
        Conditions.nonNull(mainGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return mainGroup;
    }

    @Nonnull
    @Override
    public MainGroup createMainGroup(@Nonnull String name, @Nonnull List<String> subgroups) {
        MainGroup mainGroup = createMainGroupAsync(name, subgroups).getUninterruptedly();
        Conditions.nonNull(mainGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return mainGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull String name) {
        ProcessGroup processGroup = createProcessGroupAsync(name).getUninterruptedly();
        Conditions.nonNull(processGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull String name, @Nonnull List<Template> templates) {
        ProcessGroup processGroup = createProcessGroupAsync(name, templates).getUninterruptedly();
        Conditions.nonNull(processGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration) {
        ProcessGroup processGroup = createProcessGroupAsync(name, templates, startupConfiguration).getUninterruptedly();
        Conditions.nonNull(processGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration, @Nonnull PlayerAccessConfiguration playerAccessConfiguration) {
        ProcessGroup processGroup = createProcessGroupAsync(name, templates, startupConfiguration, playerAccessConfiguration).getUninterruptedly();
        Conditions.nonNull(processGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull String name, @Nonnull List<Template> templates, @Nonnull StartupConfiguration startupConfiguration, @Nonnull PlayerAccessConfiguration playerAccessConfiguration, boolean staticGroup) {
        ProcessGroup processGroup = createProcessGroupAsync(name, templates, startupConfiguration, playerAccessConfiguration, staticGroup).getUninterruptedly();
        Conditions.nonNull(processGroup, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup;
    }

    @Nonnull
    @Override
    public ProcessGroup createProcessGroup(@Nonnull ProcessGroup processGroup) {
        ProcessGroup processGroup1 = createProcessGroupAsync(processGroup).getUninterruptedly();
        Conditions.nonNull(processGroup1, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return processGroup1;
    }

    @Nonnull
    @Override
    public MainGroup updateMainGroup(@Nonnull MainGroup mainGroup) {
        MainGroup group = updateMainGroupAsync(mainGroup).getUninterruptedly();
        Conditions.nonNull(group, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return group;
    }

    @Nonnull
    @Override
    public ProcessGroup updateProcessGroup(@Nonnull ProcessGroup processGroup) {
        ProcessGroup group = updateProcessGroupAsync(processGroup).getUninterruptedly();
        Conditions.nonNull(group, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return group;
    }

    @Nullable
    @Override
    public MainGroup getMainGroup(@Nonnull String name) {
        return getMainGroupAsync(name).getUninterruptedly();
    }

    @Nullable
    @Override
    public ProcessGroup getProcessGroup(@Nonnull String name) {
        return getProcessGroupAsync(name).getUninterruptedly();
    }

    @Override
    public void deleteMainGroup(@Nonnull String name) {
        deleteMainGroupAsync(name).awaitUninterruptedly();
    }

    @Override
    public void deleteProcessGroup(@Nonnull String name) {
        deleteProcessGroupAsync(name).awaitUninterruptedly();
    }

    @Nonnull
    @Override
    public List<MainGroup> getMainGroups() {
        List<MainGroup> mainGroups = getMainGroupsAsync().getUninterruptedly();
        Conditions.nonNull(mainGroups, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return mainGroups;
    }

    @Nonnull
    @Override
    public List<ProcessGroup> getProcessGroups() {
        List<ProcessGroup> groups = getProcessGroupsAsync().getUninterruptedly();
        Conditions.nonNull(groups, "An internal error occurred while executing api method. !!! This is not a bug of reformcloud !!! Please do not report this as a bug"); // Should never happen
        return groups;
    }
}
