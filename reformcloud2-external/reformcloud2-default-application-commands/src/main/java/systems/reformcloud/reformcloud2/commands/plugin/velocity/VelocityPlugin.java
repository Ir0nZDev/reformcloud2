package systems.reformcloud.reformcloud2.commands.plugin.velocity;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import systems.reformcloud.reformcloud2.commands.config.CommandsConfig;
import systems.reformcloud.reformcloud2.commands.plugin.CommandConfigHandler;
import systems.reformcloud.reformcloud2.commands.plugin.packet.out.PacketOutGetCommandsConfig;
import systems.reformcloud.reformcloud2.commands.plugin.velocity.commands.CommandLeave;
import systems.reformcloud.reformcloud2.commands.plugin.velocity.commands.CommandReformCloud;
import systems.reformcloud.reformcloud2.executor.api.common.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.common.network.NetworkUtil;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.PacketSender;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.manager.DefaultChannelManager;

import javax.annotation.Nonnull;

@Plugin(
        id = "reformcloud_2_commands",
        name = "ReformCloud2Commands",
        version = "2.0",
        description = "Get access to default reformcloud2 commands",
        url = "https://reformcloud.systems",
        authors = {"derklaro"},
        dependencies = {@Dependency(id = "reformcloud_2_api_executor")}
)
public class VelocityPlugin {

    @Inject
    public VelocityPlugin(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;

        CommandConfigHandler.setInstance(new ConfigHandler());
        NetworkUtil.EXECUTOR.execute(() -> {
            PacketSender sender = DefaultChannelManager.INSTANCE.get("Controller").orNothing();
            while (sender == null) {
                sender = DefaultChannelManager.INSTANCE.get("Controller").orNothing();
            }

            ExecutorAPI.getInstance().getPacketHandler().getQueryHandler().sendQueryAsync(sender, new PacketOutGetCommandsConfig())
                    .onComplete(e -> {
                        CommandsConfig commandsConfig = e.content().get("content", new TypeToken<CommandsConfig>() {});
                        if (commandsConfig == null) {
                            return;
                        }

                        CommandConfigHandler.getInstance().handleCommandConfigRelease(commandsConfig);
                    });
        });
    }

    private final ProxyServer proxyServer;

    private class ConfigHandler extends CommandConfigHandler {

        private CommandLeave commandLeave;

        private CommandReformCloud commandReformCloud;

        @Override
        public void handleCommandConfigRelease(@Nonnull CommandsConfig commandsConfig) {
            unregisterAllCommands();
            if (commandsConfig.isLeaveCommandEnabled() && commandsConfig.getLeaveCommands().size() > 0) {
                this.commandLeave = new CommandLeave(commandsConfig.getLeaveCommands());
                VelocityPlugin.this.proxyServer.getCommandManager().register(
                        this.commandLeave,
                        commandsConfig.getLeaveCommands().toArray(new String[0])
                );
            }

            if (commandsConfig.isReformCloudCommandEnabled() && commandsConfig.getReformCloudCommands().size() > 0) {
                this.commandReformCloud = new CommandReformCloud(commandsConfig.getReformCloudCommands());
                VelocityPlugin.this.proxyServer.getCommandManager().register(
                        this.commandReformCloud,
                        commandsConfig.getReformCloudCommands().toArray(new String[0])
                );
            }
        }

        @Override
        public void unregisterAllCommands() {
            if (this.commandLeave != null)  {
                this.commandLeave.getAliases().forEach(VelocityPlugin.this.proxyServer.getCommandManager()::unregister);
                this.commandLeave = null;
            }

            if (this.commandReformCloud != null) {
                this.commandReformCloud.getAliases().forEach(VelocityPlugin.this.proxyServer.getCommandManager()::unregister);
                this.commandReformCloud = null;
            }
        }
    }
}
