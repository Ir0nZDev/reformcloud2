package systems.reformcloud.reformcloud2.executor.controller;

import org.reflections.Reflections;
import systems.reformcloud.reformcloud2.executor.api.ExecutorType;
import systems.reformcloud.reformcloud2.executor.api.common.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.common.application.ApplicationLoader;
import systems.reformcloud.reformcloud2.executor.api.common.application.InstallableApplication;
import systems.reformcloud.reformcloud2.executor.api.common.application.LoadedApplication;
import systems.reformcloud.reformcloud2.executor.api.common.application.basic.DefaultApplicationLoader;
import systems.reformcloud.reformcloud2.executor.api.common.client.ClientRuntimeInformation;
import systems.reformcloud.reformcloud2.executor.api.common.client.basic.DefaultClientRuntimeInformation;
import systems.reformcloud.reformcloud2.executor.api.common.commands.AllowedCommandSources;
import systems.reformcloud.reformcloud2.executor.api.common.commands.Command;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.ConsoleCommandSource;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.DefaultCommandSource;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.commands.CommandClear;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.commands.CommandHelp;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.commands.CommandReload;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.commands.CommandStop;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.manager.DefaultCommandManager;
import systems.reformcloud.reformcloud2.executor.api.common.commands.manager.CommandManager;
import systems.reformcloud.reformcloud2.executor.api.common.commands.source.CommandSource;
import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.database.Database;
import systems.reformcloud.reformcloud2.executor.api.common.database.DatabaseReader;
import systems.reformcloud.reformcloud2.executor.api.common.database.basic.drivers.file.FileDatabase;
import systems.reformcloud.reformcloud2.executor.api.common.database.basic.drivers.mongo.MongoDatabase;
import systems.reformcloud.reformcloud2.executor.api.common.database.basic.drivers.mysql.MySQLDatabase;
import systems.reformcloud.reformcloud2.executor.api.common.event.EventManager;
import systems.reformcloud.reformcloud2.executor.api.common.event.basic.DefaultEventManager;
import systems.reformcloud.reformcloud2.executor.api.common.groups.MainGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.ProcessGroup;
import systems.reformcloud.reformcloud2.executor.api.common.groups.utils.*;
import systems.reformcloud.reformcloud2.executor.api.common.language.LanguageManager;
import systems.reformcloud.reformcloud2.executor.api.common.language.loading.LanguageWorker;
import systems.reformcloud.reformcloud2.executor.api.common.logger.LoggerBase;
import systems.reformcloud.reformcloud2.executor.api.common.logger.coloured.ColouredLoggerHandler;
import systems.reformcloud.reformcloud2.executor.api.common.logger.other.DefaultLoggerHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.auth.Auth;
import systems.reformcloud.reformcloud2.executor.api.common.network.auth.NetworkType;
import systems.reformcloud.reformcloud2.executor.api.common.network.auth.defaults.DefaultAuth;
import systems.reformcloud.reformcloud2.executor.api.common.network.auth.defaults.DefaultServerAuthHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.handler.NetworkHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.manager.DefaultChannelManager;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.defaults.DefaultPacketHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.handler.PacketHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.server.DefaultNetworkServer;
import systems.reformcloud.reformcloud2.executor.api.common.network.server.NetworkServer;
import systems.reformcloud.reformcloud2.executor.api.common.patch.Patcher;
import systems.reformcloud.reformcloud2.executor.api.common.patch.basic.DefaultPatcher;
import systems.reformcloud.reformcloud2.executor.api.common.plugins.InstallablePlugin;
import systems.reformcloud.reformcloud2.executor.api.common.plugins.Plugin;
import systems.reformcloud.reformcloud2.executor.api.common.plugins.basic.DefaultInstallablePlugin;
import systems.reformcloud.reformcloud2.executor.api.common.plugins.basic.DefaultPlugin;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessInformation;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessState;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.auth.basic.DefaultWebServerAuth;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.http.server.DefaultWebServer;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.http.server.WebServer;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.request.RequestListenerHandler;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.request.defaults.DefaultRequestListenerHandler;
import systems.reformcloud.reformcloud2.executor.api.common.restapi.user.WebUser;
import systems.reformcloud.reformcloud2.executor.api.common.utility.StringUtil;
import systems.reformcloud.reformcloud2.executor.api.common.utility.function.Double;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Links;
import systems.reformcloud.reformcloud2.executor.api.common.utility.system.DownloadHelper;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.defaults.DefaultTask;
import systems.reformcloud.reformcloud2.executor.api.controller.Controller;
import systems.reformcloud.reformcloud2.executor.api.controller.process.ProcessManager;
import systems.reformcloud.reformcloud2.executor.controller.commands.CommandReformCloud;
import systems.reformcloud.reformcloud2.executor.controller.config.ControllerConfig;
import systems.reformcloud.reformcloud2.executor.controller.config.ControllerExecutorConfig;
import systems.reformcloud.reformcloud2.executor.controller.config.DatabaseConfig;
import systems.reformcloud.reformcloud2.executor.controller.packet.out.api.ControllerAPIAction;
import systems.reformcloud.reformcloud2.executor.controller.packet.out.api.ControllerExecuteCommand;
import systems.reformcloud.reformcloud2.executor.controller.packet.out.api.ControllerPluginAction;
import systems.reformcloud.reformcloud2.executor.controller.process.ClientManager;
import systems.reformcloud.reformcloud2.executor.controller.process.DefaultProcessManager;
import systems.reformcloud.reformcloud2.executor.controller.process.startup.AutoStartupHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ControllerExecutor extends Controller {

    private static ControllerExecutor instance;

    private static volatile boolean running = false;

    private LoggerBase loggerBase;

    private AutoStartupHandler autoStartupHandler;

    private ControllerExecutorConfig controllerExecutorConfig;

    private ControllerConfig controllerConfig;

    private Database database;

    private RequestListenerHandler requestListenerHandler;

    private final CommandManager commandManager = new DefaultCommandManager();

    private final CommandSource console = new ConsoleCommandSource(commandManager);

    private final ApplicationLoader applicationLoader = new DefaultApplicationLoader();

    private final NetworkServer networkServer = new DefaultNetworkServer();

    private final WebServer webServer = new DefaultWebServer();

    private final PacketHandler packetHandler = new DefaultPacketHandler();

    private final ProcessManager processManager = new DefaultProcessManager();

    private final Patcher patcher = new DefaultPatcher();

    private final DatabaseConfig databaseConfig = new DatabaseConfig();

    private final EventManager eventManager = new DefaultEventManager();

    ControllerExecutor() {
        ExecutorAPI.setInstance(this);
        super.type = ExecutorType.CONTROLLER;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }, "Shutdown-Hook"));

        bootstrap();
    }

    @Override
    protected void bootstrap() {
        long current = System.currentTimeMillis();
        instance = this;

        try {
            if (Boolean.getBoolean("reformcloud2.disable.colours")) {
                this.loggerBase = new DefaultLoggerHandler();
            } else {
                this.loggerBase = new ColouredLoggerHandler();
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        this.controllerExecutorConfig = new ControllerExecutorConfig();
        this.requestListenerHandler = new DefaultRequestListenerHandler(new DefaultWebServerAuth(this));

        applicationLoader.detectApplications();
        applicationLoader.installApplications();

        this.controllerConfig = controllerExecutorConfig.getControllerConfig();
        this.controllerConfig.getNetworkListener().forEach(stringIntegerMap -> stringIntegerMap.forEach((s, integer) -> ControllerExecutor.this.networkServer.bind(s, integer, new DefaultServerAuthHandler(
                packetHandler,
                packetSender -> {
                    ClientManager.INSTANCE.disconnectClient(packetSender.getName());
                    processManager.onChannelClose(packetSender.getName());
                },
                packet -> {
                    DefaultAuth auth = packet.content().get("auth", Auth.TYPE);
                    if (!auth.key().equals(controllerExecutorConfig.getConnectionKey())) {
                        System.out.println(LanguageManager.get("network-channel-auth-failed", auth.getName()));
                        return new Double<>(auth.getName(), false);
                    }

                    if (auth.type().equals(NetworkType.CLIENT)) {
                        System.out.println(LanguageManager.get("client-connected", auth.getName()));
                        DefaultClientRuntimeInformation runtimeInformation = auth.extra().get("info", ClientRuntimeInformation.TYPE);
                        ClientManager.INSTANCE.connectClient(runtimeInformation);
                    } else {
                        ProcessInformation information = processManager.getProcess(auth.getName());
                        information.getNetworkInfo().setConnected(true);
                        information.setProcessState(ProcessState.READY);
                        processManager.update(information);

                        System.out.println(LanguageManager.get("process-connected", auth.getName(), auth.parent()));
                    }

                    System.out.println(LanguageManager.get("network-channel-auth-success", auth.getName(), auth.parent()));
                    return new Double<>(auth.getName(), true);
                }
        ))));

        databaseConfig.load();
        switch (databaseConfig.getType()) {
            case FILE: {
                this.database = new FileDatabase();
                this.databaseConfig.connect(this.database);
                break;
            }

            case MONGO: {
                this.database = new MongoDatabase();
                this.databaseConfig.connect(this.database);
                break;
            }

            case MYSQL: {
                this.database = new MySQLDatabase();
                this.databaseConfig.connect(this.database);
                break;
            }
        }

        applicationLoader.loadApplications();

        this.autoStartupHandler = new AutoStartupHandler();
        sendGroups();
        loadCommands();
        loadPacketHandlers();

        createDatabase("internal_users");
        if (controllerExecutorConfig.isFirstStartup()) {
            final String token = StringUtil.generateString(2);
            WebUser webUser = new WebUser("admin", token, Collections.singletonList("*"));
            insert("internal_users", webUser.getName(), "", new JsonConfiguration().add("user", webUser));

            System.out.println(LanguageManager.get("setup-created-default-user", webUser.getName(), token));
        }

        this.controllerExecutorConfig.getControllerConfig().getHttpNetworkListener().forEach(map -> map.forEach((host, port) -> {
                this.webServer.add(host, port, this.requestListenerHandler);
            })
        );

        applicationLoader.enableApplications();

        if (Files.exists(Paths.get("reformcloud/.client"))) {
            try {
                DownloadHelper.downloadAndDisconnect(StringUtil.RUNNER_DOWNLOAD_URL, "reformcloud/.client/runner.jar");
                Process process = new ProcessBuilder()
                        .command(Arrays.asList("java", "-jar", "runner.jar").toArray(new String[0]))
                        .directory(new File("reformcloud/.client"))
                        .start();
                ClientManager.INSTANCE.setProcess(process);
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        running = true;
        System.out.println(LanguageManager.get("startup-done", Long.toString(System.currentTimeMillis() - current)));
        runConsole();
    }

    @Override
    public void reload() {
        final long current = System.currentTimeMillis();
        System.out.println(LanguageManager.get("runtime-try-reload"));

        this.applicationLoader.disableApplications();

        this.commandManager.unregisterAll();
        this.packetHandler.clearHandlers();
        this.packetHandler.getQueryHandler().clearQueries(); //Unsafe? May produce exception if query is sent but not handled after reload

        this.controllerExecutorConfig.getProcessGroups().clear();
        this.controllerExecutorConfig.getMainGroups().clear();

        this.applicationLoader.detectApplications();
        this.applicationLoader.installApplications();

        LanguageWorker.doReload(); //Reloads the language files

        this.controllerExecutorConfig = new ControllerExecutorConfig();

        this.autoStartupHandler.update(); //Update the automatic startup handler to re-sort the groups per priority

        this.controllerConfig = controllerExecutorConfig.getControllerConfig();

        this.applicationLoader.loadApplications();

        this.sendGroups();
        this.loadCommands();
        this.loadPacketHandlers();

        this.applicationLoader.enableApplications();
        System.out.println(LanguageManager.get("runtime-reload-done", Long.toString(System.currentTimeMillis() - current)));
    }

    @Override
    public void shutdown() throws Exception {
        if (running) {
            running = false;
        } else {
            return;
        }

        System.out.println(LanguageManager.get("runtime-try-shutdown"));

        this.networkServer.closeAll(); //Close network first that all channels now that the controller is disconnecting
        this.webServer.close();
        ClientManager.INSTANCE.onShutdown();

        this.loggerBase.close();
        this.autoStartupHandler.interrupt();
        this.applicationLoader.disableApplications();
    }

    public ControllerExecutorConfig getControllerExecutorConfig() {
        return controllerExecutorConfig;
    }

    public static ControllerExecutor getInstance() {
        if (instance == null) {
            return (ControllerExecutor) Controller.getInstance();
        }

        return instance;
    }

    @Override
    public NetworkServer getNetworkServer() {
        return networkServer;
    }

    @Override
    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    @Override
    public CommandManager getCommandManager() {
        return commandManager;
    }

    public RequestListenerHandler getRequestListenerHandler() {
        return requestListenerHandler;
    }

    public LoggerBase getLoggerBase() {
        return loggerBase;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public ControllerConfig getControllerConfig() {
        return controllerConfig;
    }

    public Patcher getPatcher() {
        return patcher;
    }

    public AutoStartupHandler getAutoStartupHandler() {
        return autoStartupHandler;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    private void runConsole() {
        String line;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                loggerBase.getConsoleReader().setPrompt("");
                loggerBase.getConsoleReader().resetPromptLine("", "", 0);

                while ((line = loggerBase.readLine()) != null && !line.trim().isEmpty() && running) {
                    loggerBase.getConsoleReader().setPrompt("");
                    commandManager.dispatchCommand(console, AllowedCommandSources.ALL, line, System.out::println);
                }
            } catch (final Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private void sendGroups() {
        this.controllerExecutorConfig.getMainGroups().forEach(mainGroup -> System.out.println(LanguageManager.get("loading-main-group", mainGroup.getName())));
        this.controllerExecutorConfig.getProcessGroups().forEach(processGroup -> System.out.println(LanguageManager.get("loading-process-group", processGroup.getName(), processGroup.getParentGroup())));
    }

    private void loadCommands() {
        this.commandManager
                .register(CommandStop.class)
                .register(new CommandReformCloud())
                .register(new CommandReload(this))
                .register(new CommandClear(loggerBase))
                .register(new CommandHelp(commandManager));
    }

    private void loadPacketHandlers() {
        new Reflections("systems.reformcloud.reformcloud2.executor.controller.packet.in").getSubTypesOf(NetworkHandler.class).forEach(packetHandler::registerHandler);
    }

    @Override
    public Task<Boolean> loadApplicationAsync(InstallableApplication application) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(applicationLoader.doSpecificApplicationInstall(application)));
        return task;
    }

    @Override
    public Task<Boolean> unloadApplicationAsync(LoadedApplication application) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(applicationLoader.doSpecificApplicationUninstall(application)));
        return task;
    }

    @Override
    public Task<Boolean> unloadApplicationAsync(String application) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(applicationLoader.doSpecificApplicationUninstall(application)));
        return task;
    }

    @Override
    public Task<LoadedApplication> getApplicationAsync(String name) {
        Task<LoadedApplication> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(applicationLoader.getApplication(name)));
        return task;
    }

    @Override
    public Task<List<LoadedApplication>> getApplicationsAsync() {
        Task<List<LoadedApplication>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(applicationLoader.getApplications()));
        return task;
    }

    @Override
    public boolean loadApplication(InstallableApplication application) {
        return applicationLoader.doSpecificApplicationInstall(application);
    }

    @Override
    public boolean unloadApplication(LoadedApplication application) {
        return applicationLoader.doSpecificApplicationUninstall(application);
    }

    @Override
    public boolean unloadApplication(String application) {
        return applicationLoader.doSpecificApplicationUninstall(application);
    }

    @Override
    public LoadedApplication getApplication(String name) {
        return applicationLoader.getApplication(name);
    }

    @Override
    public List<LoadedApplication> getApplications() {
        return applicationLoader.getApplications();
    }

    @Override
    public Task<Void> sendColouredLineAsync(String line) throws IllegalAccessException {
        if (!(loggerBase instanceof ColouredLoggerHandler)) {
            throw new IllegalAccessException();
        }

        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            try {
                sendColouredLine(line);
                task.complete(null);
            } catch (IllegalAccessException ignored) {
                //already catches above
            }
        });
        return task;
    }

    @Override
    public Task<Void> sendRawLineAsync(String line) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            loggerBase.logRaw(line);
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<String> dispatchCommandAndGetResultAsync(String commandLine) {
        Task<String> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> commandManager.dispatchCommand(new DefaultCommandSource(task::complete, commandManager), AllowedCommandSources.ALL, commandLine, task::complete));
        return task;
    }

    @Override
    public Task<Command> getControllerCommandAsync(String name) {
        Task<Command> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(commandManager.getCommand(name)));
        return task;
    }

    @Override
    public Task<Boolean> isControllerCommandRegisteredAsync(String name) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(commandManager.getCommand(name) != null));
        return task;
    }

    @Override
    public void sendColouredLine(String line) throws IllegalAccessException {
        if (!(loggerBase instanceof ColouredLoggerHandler)) {
            throw new IllegalAccessException();
        }

        loggerBase.log(line);
    }

    @Override
    public void sendRawLine(String line) {
        loggerBase.logRaw(line);
    }

    @Override
    public String dispatchCommandAndGetResult(String commandLine) {
        return dispatchCommandAndGetResultAsync(commandLine).getUninterruptedly();
    }

    @Override
    public Command getControllerCommand(String name) {
        return getControllerCommandAsync(name).getUninterruptedly();
    }

    @Override
    public boolean isControllerCommandRegistered(String name) {
        return getControllerCommand(name) != null;
    }

    @Override
    public Task<MainGroup> createMainGroupAsync(String name) {
        return createMainGroupAsync(name, new ArrayList<>());
    }

    @Override
    public Task<MainGroup> createMainGroupAsync(String name, List<String> subgroups) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            MainGroup mainGroup = new MainGroup(name, subgroups);
            task.complete(controllerExecutorConfig.createMainGroup(mainGroup));
        });
        return task;
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name) {
        return createProcessGroupAsync(name, null);
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name, String parent) {
        return createProcessGroupAsync(name, parent, Collections.singletonList(
                new Template(0, "default", "#", null, "§8A ReformCloud2 default process", new RuntimeConfiguration(
                        512, new ArrayList<>(), new HashMap<>()
                ), Version.PAPER_1_8_8)
        ));
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name, String parent, List<Template> templates) {
        return createProcessGroupAsync(name, parent, templates, new StartupConfiguration(
                -1, 1, 1, 41000, StartupEnvironment.JAVA_RUNTIME, true, new ArrayList<>()
        ));
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration) {
        return createProcessGroupAsync(name, parent, templates, startupConfiguration, new PlayerAccessConfiguration(
                false, "reformcloud.join.maintenance", false,
                null, true, true, true, 50
        ));
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration, PlayerAccessConfiguration playerAccessConfiguration) {
        return createProcessGroupAsync(name, parent, templates, startupConfiguration, playerAccessConfiguration, false);
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration, PlayerAccessConfiguration playerAccessConfiguration, boolean staticGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessGroup processGroup = new ProcessGroup(
                    name,
                    true,
                    parent,
                    startupConfiguration,
                    templates,
                    playerAccessConfiguration,
                    staticGroup
            );
            task.complete(createProcessGroupAsync(processGroup).getUninterruptedly());
        });
        return task;
    }

    @Override
    public Task<ProcessGroup> createProcessGroupAsync(ProcessGroup processGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(controllerExecutorConfig.createProcessGroup(processGroup)));
        return task;
    }

    @Override
    public Task<MainGroup> updateMainGroupAsync(MainGroup mainGroup) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            controllerExecutorConfig.updateMainGroup(mainGroup);
            task.complete(mainGroup);
        });
        return task;
    }

    @Override
    public Task<ProcessGroup> updateProcessGroupAsync(ProcessGroup processGroup) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            controllerExecutorConfig.updateProcessGroup(processGroup);
            task.complete(processGroup);
        });
        return task;
    }

    @Override
    public Task<MainGroup> getMainGroupAsync(String name) {
        Task<MainGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Links.filter(controllerExecutorConfig.getMainGroups(), mainGroup -> mainGroup.getName().equals(name))));
        return task;
    }

    @Override
    public Task<ProcessGroup> getProcessGroupAsync(String name) {
        Task<ProcessGroup> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Links.filter(controllerExecutorConfig.getProcessGroups(), processGroup -> processGroup.getName().equals(name))));
        return task;
    }

    @Override
    public Task<Void> deleteMainGroupAsync(String name) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            Links.filterToReference(controllerExecutorConfig.getMainGroups(), mainGroup -> mainGroup.getName().equals(name)).ifPresent(mainGroup -> controllerExecutorConfig.deleteMainGroup(mainGroup));
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> deleteProcessGroupAsync(String name) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            Links.filterToReference(controllerExecutorConfig.getProcessGroups(), processGroup -> processGroup.getName().equals(name)).ifPresent(processGroup -> controllerExecutorConfig.deleteProcessGroup(processGroup));
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<List<MainGroup>> getMainGroupsAsync() {
        Task<List<MainGroup>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Collections.unmodifiableList(controllerExecutorConfig.getMainGroups())));
        return task;
    }

    @Override
    public Task<List<ProcessGroup>> getProcessGroupsAsync() {
        Task<List<ProcessGroup>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Collections.unmodifiableList(controllerExecutorConfig.getProcessGroups())));
        return task;
    }

    @Override
    public MainGroup createMainGroup(String name) {
        return createMainGroupAsync(name).getUninterruptedly();
    }

    @Override
    public MainGroup createMainGroup(String name, List<String> subgroups) {
        return createMainGroupAsync(name, subgroups).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name) {
        return createProcessGroupAsync(name).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name, String parent) {
        return createProcessGroupAsync(name, parent).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name, String parent, List<Template> templates) {
        return createProcessGroupAsync(name, parent, templates).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration) {
        return createProcessGroupAsync(name, parent, templates, startupConfiguration).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration, PlayerAccessConfiguration playerAccessConfiguration) {
        return createProcessGroupAsync(name, parent, templates, startupConfiguration, playerAccessConfiguration).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(String name, String parent, List<Template> templates, StartupConfiguration startupConfiguration, PlayerAccessConfiguration playerAccessConfiguration, boolean staticGroup) {
        return createProcessGroupAsync(name, parent, templates, startupConfiguration, playerAccessConfiguration, staticGroup).getUninterruptedly();
    }

    @Override
    public ProcessGroup createProcessGroup(ProcessGroup processGroup) {
        return createProcessGroupAsync(processGroup).getUninterruptedly();
    }

    @Override
    public MainGroup updateMainGroup(MainGroup mainGroup) {
        return updateMainGroupAsync(mainGroup).getUninterruptedly();
    }

    @Override
    public ProcessGroup updateProcessGroup(ProcessGroup processGroup) {
        return updateProcessGroupAsync(processGroup).getUninterruptedly();
    }

    @Override
    public MainGroup getMainGroup(String name) {
        return getMainGroupAsync(name).getUninterruptedly();
    }

    @Override
    public ProcessGroup getProcessGroup(String name) {
        return getProcessGroupAsync(name).getUninterruptedly();
    }

    @Override
    public void deleteMainGroup(String name) {
        deleteMainGroupAsync(name).awaitUninterruptedly();
    }

    @Override
    public void deleteProcessGroup(String name) {
        deleteProcessGroupAsync(name).awaitUninterruptedly();
    }

    @Override
    public List<MainGroup> getMainGroups() {
        return getMainGroupsAsync().getUninterruptedly();
    }

    @Override
    public List<ProcessGroup> getProcessGroups() {
        return getProcessGroupsAsync().getUninterruptedly();
    }

    @Override
    public Task<Void> sendMessageAsync(UUID player, String message) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnProxy(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.SEND_MESSAGE,
                        Arrays.asList(player, message)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> kickPlayerAsync(UUID player, String message) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnProxy(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.KICK_PLAYER,
                        Arrays.asList(player, message)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> kickPlayerFromServerAsync(UUID player, String message) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.KICK_PLAYER,
                        Arrays.asList(player, message)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> playSoundAsync(UUID player, String sound, float f1, float f2) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.PLAY_SOUND,
                        Arrays.asList(player, sound, f1, f2)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> sendTitleAsync(UUID player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnProxy(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.SEND_TITLE,
                        Arrays.asList(player, title, subTitle, fadeIn, stay, fadeOut)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> playEffectAsync(UUID player, String entityEffect) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.PLAY_ENTITY_EFFECT,
                        Arrays.asList(player, entityEffect)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public <T> Task<Void> playEffectAsync(UUID player, String effect, T data) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnProxy(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.PLAY_EFFECT,
                        Arrays.asList(player, effect, data)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> respawnAsync(UUID player) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.RESPAWN,
                        Collections.singletonList(player)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> teleportAsync(UUID player, String world, double x, double y, double z, float yaw, float pitch) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.LOCATION_TELEPORT,
                        Arrays.asList(player, world, x, y, z, yaw, pitch)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> connectAsync(UUID player, String server) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnProxy(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.CONNECT,
                        Arrays.asList(player, server)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> connectAsync(UUID player, ProcessInformation server) {
        return connectAsync(player, server.getName());
    }

    @Override
    public Task<Void> connectAsync(UUID player, UUID target) {
        ProcessInformation targetServer = getPlayerOnServer(target);
        return connectAsync(player, targetServer);
    }

    @Override
    public Task<Void> setResourcePackAsync(UUID player, String pack) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = ControllerExecutor.this.getPlayerOnServer(player);
            if (processInformation != null) {
                DefaultChannelManager.INSTANCE.get(processInformation.getName()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerAPIAction(
                        ControllerAPIAction.APIAction.SET_RESOURCE_PACK,
                        Arrays.asList(player, pack)
                )));
            }
            task.complete(null);
        });
        return task;
    }

    @Override
    public void sendMessage(UUID player, String message) {
        sendMessageAsync(player, message).awaitUninterruptedly();
    }

    @Override
    public void kickPlayer(UUID player, String message) {
        kickPlayerAsync(player, message).awaitUninterruptedly();
    }

    @Override
    public void kickPlayerFromServer(UUID player, String message) {
        kickPlayerFromServerAsync(player, message).awaitUninterruptedly();
    }

    @Override
    public void playSound(UUID player, String sound, float f1, float f2) {
        playSoundAsync(player, sound, f1, f2).awaitUninterruptedly();
    }

    @Override
    public void sendTitle(UUID player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        sendTitleAsync(player, title, subTitle, fadeIn, stay, fadeOut).awaitUninterruptedly();
    }

    @Override
    public void playEffect(UUID player, String entityEffect) {
        playEffectAsync(player, entityEffect).awaitUninterruptedly();
    }

    @Override
    public <T> void playEffect(UUID player, String effect, T data) {
        playEffectAsync(player, effect, data).awaitUninterruptedly();
    }

    @Override
    public void respawn(UUID player) {
        respawnAsync(player).awaitUninterruptedly();
    }

    @Override
    public void teleport(UUID player, String world, double x, double y, double z, float yaw, float pitch) {
        teleportAsync(player, world, x, y, z, yaw, pitch).awaitUninterruptedly();
    }

    @Override
    public void connect(UUID player, String server) {
        connectAsync(player, server).awaitUninterruptedly();
    }

    @Override
    public void connect(UUID player, ProcessInformation server) {
        connectAsync(player, server).awaitUninterruptedly();
    }

    @Override
    public void connect(UUID player, UUID target) {
        connectAsync(player, target).awaitUninterruptedly();
    }

    @Override
    public void setResourcePack(UUID player, String pack) {
        setResourcePackAsync(player, pack).awaitUninterruptedly();
    }

    @Override
    public Task<Void> installPluginAsync(String process, InstallablePlugin plugin) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            DefaultChannelManager.INSTANCE.get(process).ifPresent(packetSender -> packetSender.sendPacket(new ControllerPluginAction(
                    ControllerPluginAction.Action.INSTALL,
                    new DefaultInstallablePlugin(
                            plugin.getDownloadURL(),
                            plugin.getName(),
                            plugin.version(),
                            plugin.author(),
                            plugin.main()
                    )
            )));
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> installPluginAsync(ProcessInformation process, InstallablePlugin plugin) {
        return installPluginAsync(process.getName(), plugin);
    }

    @Override
    public Task<Void> unloadPluginAsync(String process, Plugin plugin) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            DefaultChannelManager.INSTANCE.get(process).ifPresent(packetSender -> packetSender.sendPacket(new ControllerPluginAction(
                    ControllerPluginAction.Action.UNINSTALL,
                    new DefaultPlugin(
                            plugin.version(),
                            plugin.author(),
                            plugin.main(),
                            plugin.depends(),
                            plugin.softpends(),
                            plugin.enabled(),
                            plugin.getName()
                    )
            )));
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Void> unloadPluginAsync(ProcessInformation process, Plugin plugin) {
        return unloadPluginAsync(process.getName(), plugin);
    }

    @Override
    public Task<Plugin> getInstalledPluginAsync(String process, String name) {
        Task<Plugin> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = getProcess(process);
            if (processInformation == null) {
                task.complete(null);
                return;
            }

            task.complete(Links.filter(processInformation.getPlugins(), defaultPlugin -> defaultPlugin.getName().equals(name)));
        });
        return task;
    }

    @Override
    public Task<Plugin> getInstalledPluginAsync(ProcessInformation process, String name) {
        return getInstalledPluginAsync(process.getName(), name);
    }

    @Override
    public Task<Collection<DefaultPlugin>> getPluginsAsync(String process, String author) {
        Task<Collection<DefaultPlugin>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = getProcess(process);
            if (processInformation == null) {
                task.complete(null);
                return;
            }

            task.complete(Links.allOf(processInformation.getPlugins(), defaultPlugin -> defaultPlugin.author().equals(author)));
        });
        return task;
    }

    @Override
    public Task<Collection<DefaultPlugin>> getPluginsAsync(ProcessInformation process, String author) {
        return getPluginsAsync(process.getName(), author);
    }

    @Override
    public Task<Collection<DefaultPlugin>> getPluginsAsync(String process) {
        Task<Collection<DefaultPlugin>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation processInformation = getProcess(process);
            if (processInformation == null) {
                task.complete(null);
                return;
            }

            task.complete(processInformation.getPlugins());
        });
        return task;
    }

    @Override
    public Task<Collection<DefaultPlugin>> getPluginsAsync(ProcessInformation processInformation) {
        return getPluginsAsync(processInformation.getName());
    }

    @Override
    public void installPlugin(String process, InstallablePlugin plugin) {
        installPluginAsync(process, plugin).awaitUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public void installPlugin(ProcessInformation process, InstallablePlugin plugin) {
        installPluginAsync(process, plugin).awaitUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public void unloadPlugin(String process, Plugin plugin) {
        unloadPluginAsync(process, plugin).awaitUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public void unloadPlugin(ProcessInformation process, Plugin plugin) {
        unloadPluginAsync(process, plugin).awaitUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Plugin getInstalledPlugin(String process, String name) {
        return getInstalledPluginAsync(process, name).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Plugin getInstalledPlugin(ProcessInformation process, String name) {
        return getInstalledPluginAsync(process, name).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Collection<DefaultPlugin> getPlugins(String process, String author) {
        return getPluginsAsync(process, author).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Collection<DefaultPlugin> getPlugins(ProcessInformation process, String author) {
        return getPluginsAsync(process, author).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Collection<DefaultPlugin> getPlugins(String process) {
        return getPluginsAsync(process).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Collection<DefaultPlugin> getPlugins(ProcessInformation processInformation) {
        return getPluginsAsync(processInformation).getUninterruptedly(TimeUnit.SECONDS, 5);
    }

    @Override
    public Task<ProcessInformation> startProcessAsync(String groupName) {
        return startProcessAsync(groupName, null);
    }

    @Override
    public Task<ProcessInformation> startProcessAsync(String groupName, String template) {
        return startProcessAsync(groupName, template, new JsonConfiguration());
    }

    @Override
    public Task<ProcessInformation> startProcessAsync(String groupName, String template, JsonConfiguration configurable) {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.startProcess(groupName, template, configurable)));
        return task;
    }

    @Override
    public Task<ProcessInformation> stopProcessAsync(String name) {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.stopProcess(name)));
        return task;
    }

    @Override
    public Task<ProcessInformation> stopProcessAsync(UUID uniqueID) {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.stopProcess(uniqueID)));
        return task;
    }

    @Override
    public Task<ProcessInformation> getProcessAsync(String name) {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.getProcess(name)));
        return task;
    }

    @Override
    public Task<ProcessInformation> getProcessAsync(UUID uniqueID) {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.getProcess(uniqueID)));
        return task;
    }

    @Override
    public Task<List<ProcessInformation>> getAllProcessesAsync() {
        Task<List<ProcessInformation>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.getAllProcesses()));
        return task;
    }

    @Override
    public Task<List<ProcessInformation>> getProcessesAsync(String group) {
        Task<List<ProcessInformation>> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.getProcesses(group)));
        return task;
    }

    @Override
    public Task<Void> executeProcessCommandAsync(String name, String commandLine) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ProcessInformation information = getProcess(name);
            DefaultChannelManager.INSTANCE.get(information.getParent()).ifPresent(packetSender -> packetSender.sendPacket(new ControllerExecuteCommand(name, commandLine)));
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Integer> getGlobalOnlineCountAsync(Collection<String> ignoredProxies) {
        Task<Integer> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(processManager.getAllProcesses().stream().filter(processInformation -> !processInformation.getTemplate().isServer() && !ignoredProxies.contains(processInformation.getName())).mapToInt(ProcessInformation::getOnlineCount).sum()));
        return task;
    }

    @Override
    public Task<ProcessInformation> getThisProcessInformationAsync() {
        Task<ProcessInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(getThisProcessInformation()));
        return task;
    }

    @Override
    public ProcessInformation startProcess(String groupName) {
        return startProcessAsync(groupName).getUninterruptedly();
    }

    @Override
    public ProcessInformation startProcess(String groupName, String template) {
        return startProcessAsync(groupName, template).getUninterruptedly();
    }

    @Override
    public ProcessInformation startProcess(String groupName, String template, JsonConfiguration configurable) {
        return startProcessAsync(groupName, template, configurable).getUninterruptedly();
    }

    @Override
    public ProcessInformation stopProcess(String name) {
        return stopProcessAsync(name).getUninterruptedly();
    }

    @Override
    public ProcessInformation stopProcess(UUID uniqueID) {
        return stopProcessAsync(uniqueID).getUninterruptedly();
    }

    @Override
    public ProcessInformation getProcess(String name) {
        return getProcessAsync(name).getUninterruptedly();
    }

    @Override
    public ProcessInformation getProcess(UUID uniqueID) {
        return getProcessAsync(uniqueID).getUninterruptedly();
    }

    @Override
    public List<ProcessInformation> getAllProcesses() {
        return getAllProcessesAsync().getUninterruptedly();
    }

    @Override
    public List<ProcessInformation> getProcesses(String group) {
        return getProcessesAsync(group).getUninterruptedly();
    }

    @Override
    public void executeProcessCommand(String name, String commandLine) {
        executeProcessCommandAsync(name, commandLine).awaitUninterruptedly();
    }

    @Override
    public int getGlobalOnlineCount(Collection<String> ignoredProxies) {
        return getGlobalOnlineCountAsync(ignoredProxies).getUninterruptedly();
    }

    @Override
    public ProcessInformation getThisProcessInformation() {
        return null;
    }

    @Override
    public void update(ProcessInformation processInformation) {
        this.processManager.update(processInformation);
    }

    private ProcessInformation getPlayerOnProxy(UUID uniqueID) {
        return Links.filter(processManager.getAllProcesses(), processInformation -> !processInformation.getTemplate().isServer() && Links.filterToReference(processInformation.getOnlinePlayers(), player -> player.getUniqueID().equals(uniqueID)).isPresent());
    }

    private ProcessInformation getPlayerOnServer(UUID uniqueID) {
        return Links.filter(processManager.getAllProcesses(), processInformation -> processInformation.getTemplate().isServer() && Links.filterToReference(processInformation.getOnlinePlayers(), player -> player.getUniqueID().equals(uniqueID)).isPresent());
    }

    @Override
    public Task<Boolean> isClientConnectedAsync(String name) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(Links.filterToReference(ClientManager.INSTANCE.getClientRuntimeInformation(), clientRuntimeInformation -> clientRuntimeInformation.getName().equals(name)).isPresent()));
        return task;
    }

    @Override
    public Task<String> getClientStartHostAsync(String name) {
        Task<String> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ClientRuntimeInformation information = Links.filter(ClientManager.INSTANCE.getClientRuntimeInformation(), clientRuntimeInformation -> clientRuntimeInformation.getName().equals(name));
            if (information == null) {
                task.complete(null);
            } else {
                task.complete(information.startHost());
            }
        });
        return task;
    }

    @Override
    public Task<Integer> getMaxMemoryAsync(String name) {
        Task<Integer> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ClientRuntimeInformation information = Links.filter(ClientManager.INSTANCE.getClientRuntimeInformation(), clientRuntimeInformation -> clientRuntimeInformation.getName().equals(name));
            if (information == null) {
                task.complete(null);
            } else {
                task.complete(information.maxMemory());
            }
        });
        return task;
    }

    @Override
    public Task<Integer> getMaxProcessesAsync(String name) {
        Task<Integer> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ClientRuntimeInformation information = Links.filter(ClientManager.INSTANCE.getClientRuntimeInformation(), clientRuntimeInformation -> clientRuntimeInformation.getName().equals(name));
            if (information == null) {
                task.complete(null);
            } else {
                task.complete(information.maxProcessCount());
            }
        });
        return task;
    }

    @Override
    public Task<ClientRuntimeInformation> getClientInformationAsync(String name) {
        Task<ClientRuntimeInformation> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            ClientRuntimeInformation information = Links.filter(ClientManager.INSTANCE.getClientRuntimeInformation(), clientRuntimeInformation -> clientRuntimeInformation.getName().equals(name));
            task.complete(information);
        });
        return task;
    }

    @Override
    public boolean isClientConnected(String name) {
        return isClientConnectedAsync(name).getUninterruptedly();
    }

    @Override
    public String getClientStartHost(String name) {
        return getClientStartHostAsync(name).getUninterruptedly();
    }

    @Override
    public int getMaxMemory(String name) {
        return getMaxMemoryAsync(name).getUninterruptedly();
    }

    @Override
    public int getMaxProcesses(String name) {
        return getMaxProcessesAsync(name).getUninterruptedly();
    }

    @Override
    public ClientRuntimeInformation getClientInformation(String name) {
        return getClientInformationAsync(name).getUninterruptedly();
    }

    @Override
    public Task<JsonConfiguration> findAsync(String table, String key, String identifier) {
        Task<JsonConfiguration> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            DatabaseReader databaseReader = ControllerExecutor.this.database.createForTable(table);
            JsonConfiguration result = databaseReader.find(key).getUninterruptedly();
            if (result != null) {
                task.complete(result);
            } else if (identifier != null) {
                task.complete(databaseReader.findIfAbsent(identifier).getUninterruptedly());
            } else {
                task.complete(null);
            }
        });
        return task;
    }

    @Override
    public <T> Task<T> findAsync(String table, String key, String identifier, Function<JsonConfiguration, T> function) {
        Task<T> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            JsonConfiguration jsonConfiguration = findAsync(table, key, identifier).getUninterruptedly();
            if (jsonConfiguration != null) {
                task.complete(function.apply(jsonConfiguration));
            } else {
                task.complete(null);
            }
        });
        return task;
    }

    @Override
    public Task<Void> insertAsync(String table, String key, String identifier, JsonConfiguration data) {
        Task<Void> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> {
            database.createForTable(table).insert(key, identifier, data).awaitUninterruptedly();
            task.complete(null);
        });
        return task;
    }

    @Override
    public Task<Boolean> updateAsync(String table, String key, JsonConfiguration newData) {
        return database.createForTable(table).update(key, newData);
    }

    @Override
    public Task<Boolean> updateIfAbsentAsync(String table, String identifier, JsonConfiguration newData) {
        return database.createForTable(table).updateIfAbsent(identifier, newData);
    }

    @Override
    public Task<Void> removeAsync(String table, String key) {
        return database.createForTable(table).remove(key);
    }

    @Override
    public Task<Void> removeIfAbsentAsync(String table, String identifier) {
        return database.createForTable(table).removeIfAbsent(identifier);
    }

    @Override
    public Task<Boolean> createDatabaseAsync(String name) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(database.createDatabase(name)));
        return task;
    }

    @Override
    public Task<Boolean> deleteDatabaseAsync(String name) {
        Task<Boolean> task = new DefaultTask<>();
        Task.EXECUTOR.execute(() -> task.complete(database.deleteDatabase(name)));
        return task;
    }

    @Override
    public Task<Boolean> containsAsync(String table, String key) {
        return database.createForTable(table).contains(key);
    }

    @Override
    public Task<Integer> sizeAsync(String table) {
        return database.createForTable(table).size();
    }

    @Override
    public JsonConfiguration find(String table, String key, String identifier) {
        return findAsync(table, key, identifier).getUninterruptedly();
    }

    @Override
    public <T> T find(String table, String key, String identifier, Function<JsonConfiguration, T> function) {
        return findAsync(table, key, identifier, function).getUninterruptedly();
    }

    @Override
    public void insert(String table, String key, String identifier, JsonConfiguration data) {
        insertAsync(table, key, identifier, data).awaitUninterruptedly();
    }

    @Override
    public boolean update(String table, String key, JsonConfiguration newData) {
        return updateAsync(table, key, newData).getUninterruptedly();
    }

    @Override
    public boolean updateIfAbsent(String table, String identifier, JsonConfiguration newData) {
        return updateIfAbsentAsync(table, identifier, newData).getUninterruptedly();
    }

    @Override
    public void remove(String table, String key) {
        removeAsync(table, key).awaitUninterruptedly();
    }

    @Override
    public void removeIfAbsent(String table, String identifier) {
        removeIfAbsentAsync(table, identifier).awaitUninterruptedly();
    }

    @Override
    public boolean createDatabase(String name) {
        return createDatabaseAsync(name).getUninterruptedly();
    }

    @Override
    public boolean deleteDatabase(String name) {
        return deleteDatabaseAsync(name).getUninterruptedly();
    }

    @Override
    public boolean contains(String table, String key) {
        return containsAsync(table, key).getUninterruptedly();
    }

    @Override
    public int size(String table) {
        return sizeAsync(table).getUninterruptedly();
    }
}