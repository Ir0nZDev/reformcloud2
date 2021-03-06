package systems.reformcloud.reformcloud2.backends.sftp;

import com.google.gson.reflect.TypeToken;
import com.jcraft.jsch.*;
import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.groups.ProcessGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.backend.TemplateBackend;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.backend.TemplateBackendManager;
import systems.reformcloud.reformcloud2.executor.api.common.network.NetworkUtil;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.common.utility.system.SystemHelper;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.defaults.DefaultTask;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public final class SFTPTemplateBackend implements TemplateBackend {

    public static void load(String baseDirectory) {
        if (Files.notExists(Paths.get(baseDirectory, "sftp.json"))) {
            new JsonConfiguration()
                    .add("config", new SFTPConfig(
                            false, "127.0.0.1", 22, "rc", "password", "/home/templates/"
                    )).write(Paths.get(baseDirectory, "sftp.json"));
        }

        SFTPConfig config = JsonConfiguration.read(Paths.get(baseDirectory, "sftp.json")).get("config", new TypeToken<SFTPConfig>() {});
        if (config == null || !config.isEnabled()) {
            return;
        }

        TemplateBackendManager.registerBackend(new SFTPTemplateBackend(config));
    }

    public static void unload() {
        TemplateBackendManager.unregisterBackend("SFTP");
    }

    private static final BlockingDeque<Runnable> TASKS = new LinkedBlockingDeque<>();

    private Session session;

    private ChannelSftp channel;

    private final SFTPConfig config;

    private SFTPTemplateBackend(SFTPConfig config) {
        this.config = config;
        this.open();

        NetworkUtil.EXECUTOR.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    Runnable runnable = TASKS.poll(20, TimeUnit.SECONDS);
                    boolean available = !this.isDisconnected();

                    if (runnable == null) {
                        if (available) {
                            this.channel.disconnect();
                        }

                        continue;
                    }

                    if (!available) {
                        this.open();
                    }

                    runnable.run();
                } catch (final InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private boolean isDisconnected() {
        return this.session == null || !this.session.isConnected() || this.channel == null || !this.channel.isConnected();
    }

    @Override
    public boolean existsTemplate(String group, String template) {
        if (isDisconnected()) {
            return false;
        }

        try {
            SftpATTRS attrs = this.channel.stat(this.config.getBaseDirectory() + group + "/" + template);
            return attrs != null && attrs.isDir();
        } catch (final SftpException ex) {
            return false;
        }
    }

    @Nonnull
    @Override
    public Task<Void> createTemplate(String group, String template) {
        if (isDisconnected()) {
            return Task.completedTask(null);
        }

        return future(() -> this.makeDirectory(this.config.getBaseDirectory() + group + "/" + template));
    }

    @Nonnull
    @Override
    public Task<Void> loadTemplate(String group, String template, Path target) {
        if (isDisconnected()) {
            return Task.completedTask(null);
        }

        return future(() -> this.downloadDirectory(this.config.getBaseDirectory() + group + "/" + template, target.toString()));
    }

    public void downloadDirectory(String remotePath, String localPath) {
        try {
            Collection<ChannelSftp.LsEntry> entries = this.listFiles(remotePath);
            if (entries == null) {
                return;
            }

            Path dir = Paths.get(localPath);
            SystemHelper.recreateDirectory(dir);
            if (!localPath.endsWith("/")) {
                localPath += "/";
            }

            if (!remotePath.endsWith("/")) {
                remotePath += "/";
            }

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getAttrs().isDir()) {
                    this.downloadDirectory(remotePath + entry.getFilename(), localPath + entry.getFilename());
                } else {
                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(localPath, entry.getFilename()))) {
                        this.channel.get(remotePath + entry.getFilename(), outputStream);
                    }
                }
            }
        } catch (final SftpException | IOException ex) {
            ex.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public Task<Void> loadGlobalTemplates(ProcessGroup group, Path target) {
        return future(() -> Streams.allOf(group.getTemplates(), e -> e.getBackend().equals(getName())
                && e.isGlobal()).forEach(e -> this.loadTemplate(group.getName(), e.getName(), target)));
    }

    @Nonnull
    @Override
    public Task<Void> loadPath(String path, Path target) {
        if (isDisconnected()) {
            return Task.completedTask(null);
        }

        return future(() -> this.downloadDirectory(this.config.getBaseDirectory() + path, target.toString()));
    }

    @Nonnull
    @Override
    public Task<Void> deployTemplate(String group, String template, Path current) {
        if (isDisconnected()) {
            return Task.completedTask(null);
        }

        return future(() -> {
            try {
                File[] files = current.toFile().listFiles();
                if (files == null || files.length == 0) {
                    return;
                }

                this.makeDirectory(this.config.getBaseDirectory() + group + "/" + template);
                for (File file : files) {
                    this.upload(this.config.getBaseDirectory() + group + "/" + template, file);
                }
            } catch (final SftpException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void upload(String path, File file) throws SftpException {
        String currentPath = path + "/" + file.getName();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            this.makeDirectory(currentPath);
            for (File next : files) {
                this.upload(currentPath, next);
            }
        } else if (file.isFile()) {
            this.channel.put(file.getPath(), currentPath);
        }
    }

    @Override
    public void deleteTemplate(String group, String template) {
        if (isDisconnected()) {
            return;
        }

        this.deleteAll(this.config.getBaseDirectory() + group + "/" + template);
    }

    private void deleteAll(String path) {
        try {
            Collection<ChannelSftp.LsEntry> entries = this.listFiles(path);
            if (entries == null) {
                return;
            }

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getAttrs().isDir()) {
                    this.deleteAll(path + "/" + entry.getFilename());
                } else {
                    this.channel.rm(path + "/" + entry.getFilename());
                }
            }

            this.channel.rmdir(path);
        } catch (final SftpException ex) {
            ex.printStackTrace();
        }
    }

    private void makeDirectory(String path) {
        StringBuilder builder = new StringBuilder();
        for (String pathSegment : path.split("/")) {
            builder.append('/').append(pathSegment);
            try {
                this.channel.mkdir(builder.toString());
            } catch (final SftpException ignored) {
                // dir already exists
            }
        }

        this.goToBase();
    }

    private void goToBase() {
        try {
            this.channel.cd(this.config.getBaseDirectory().startsWith("/") ? this.config.getBaseDirectory() : "/" + this.config.getBaseDirectory());
        } catch (final SftpException ex) {
            ex.printStackTrace();
        }
    }

    public Collection<ChannelSftp.LsEntry> listFiles(String directory) {
        Collection<ChannelSftp.LsEntry> entries = new ArrayList<>();
        try {
            this.channel.ls(directory, lsEntry -> {
                if (!lsEntry.getFilename().equals("..") && !lsEntry.getFilename().equals(".")) {
                    entries.add(lsEntry);
                }

                return 0;
            });
        } catch (final SftpException ex) {
            return null;
        }

        return entries;
    }

    private static Task<Void> future(@Nonnull Runnable runnable) {
        Task<Void> completableFuture = new DefaultTask<>();
        Runnable newRunnable = () -> {
            runnable.run();
            completableFuture.complete(null);
        };
        TASKS.offerLast(newRunnable);
        return completableFuture;
    }

    private void open() {
        try {
            this.session = new JSch().getSession(config.getUser(), config.getHost(), config.getPort());
            this.session.setPassword(config.getPassword());
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.connect(2500);

            this.channel = (ChannelSftp) this.session.openChannel("sftp");
            if (this.channel == null) {
                this.session.disconnect();
                this.session = null;
                return;
            }

            this.channel.connect();
            this.channel.setFilenameEncoding(StandardCharsets.UTF_8.name());
        } catch (final JSchException | SftpException ex) {
            ex.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "SFTP";
    }
}
