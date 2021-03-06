package systems.reformcloud.reformcloud2.executor.api.common.groups.template.backend.basic;

import systems.reformcloud.reformcloud2.executor.api.common.groups.ProcessGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.Template;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.backend.TemplateBackend;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.common.utility.system.SystemHelper;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileBackend implements TemplateBackend {

    public static final String NAME = "FILE";

    @Override
    public boolean existsTemplate(String group, String template) {
        return Files.exists(format(group, template));
    }

    @Nonnull
    @Override
    public Task<Void> createTemplate(String group, String template) {
        if (!existsTemplate(group, template)) {
            SystemHelper.createDirectory(Paths.get("reformcloud/templates", group, template, "plugins"));
        }

        return Task.completedTask(null);
    }

    @Nonnull
    @Override
    public Task<Void> loadTemplate(String group, String template, Path target) {
        if (!existsTemplate(group, template)) {
            createTemplate(group, template);
            return Task.completedTask(null);
        }

        SystemHelper.copyDirectory(format(group, template), target);
        return Task.completedTask(null);
    }

    @Nonnull
    @Override
    public Task<Void> loadGlobalTemplates(ProcessGroup group, Path target) {
        Streams.allOf(group.getTemplates(), Template::isGlobal).forEach(e -> loadTemplate(group.getName(), e.getName(), target));
        return Task.completedTask(null);
    }

    @Nonnull
    @Override
    public Task<Void> loadPath(String path, Path target) {
        File from = new File(path);
        if (from.isDirectory()) {
            SystemHelper.copyDirectory(from.toPath(), target);
        }

        return Task.completedTask(null);
    }

    @Nonnull
    @Override
    public Task<Void> deployTemplate(String group, String template, Path current) {
        if (!existsTemplate(group, template)) {
            return Task.completedTask(null);
        }

        SystemHelper.copyDirectory(current, format(group, template), Arrays.asList("log-out.log", "runner.jar"));
        return Task.completedTask(null);
    }

    @Override
    public void deleteTemplate(String group, String template) {
        if (!existsTemplate(group, template)) {
            return;
        }

        SystemHelper.deleteDirectory(format(group, template));
    }

    @Nonnull
    @Override
    public String getName() {
        return NAME;
    }

    private Path format(String group, String template) {
        return Paths.get("reformcloud/templates/" + group + "/" + template);
    }
}
