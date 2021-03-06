package systems.reformcloud.reformcloud2.commands.plugin.velocity.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import systems.reformcloud.reformcloud2.executor.api.api.API;
import systems.reformcloud.reformcloud2.executor.api.common.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessInformation;
import systems.reformcloud.reformcloud2.executor.api.velocity.VelocityExecutor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class CommandLeave implements Command {

    public CommandLeave(@Nonnull List<String> aliases) {
        this.aliases = aliases;
    }

    private final List<String> aliases;

    @Override
    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (!(commandSource instanceof Player)) {
            return;
        }

        final Player player = (Player) commandSource;

        Optional<ServerConnection> currentServer = player.getCurrentServer();
        ProcessInformation process = null;
        if (currentServer.isPresent()) {
            process = ExecutorAPI.getInstance().getSyncAPI().getProcessSyncAPI().getProcess(currentServer.get().getServerInfo().getName());
        }

        if (!currentServer.isPresent() || process == null || process.isLobby()) {
            player.sendMessage(TextComponent.of(
                    VelocityExecutor.getInstance().getMessages().format(
                            VelocityExecutor.getInstance().getMessages().getAlreadyConnectedToHub()
                    )
            ));
            return;
        }

        ProcessInformation lobby = VelocityExecutor.getBestLobbyForPlayer(
                API.getInstance().getCurrentProcessInformation(),
                player,
                player::hasPermission
        );
        if (lobby != null) {
            player.sendMessage(TextComponent.of(
                    VelocityExecutor.getInstance().getMessages().format(
                            VelocityExecutor.getInstance().getMessages().getConnectingToHub(), lobby.getName()
                    )
            ));
            VelocityExecutor.getInstance().getProxyServer().getServer(lobby.getName()).ifPresent(e -> player.createConnectionRequest(e).fireAndForget());
            return;
        }

        player.sendMessage(TextComponent.of(
                VelocityExecutor.getInstance().getMessages().format(
                        VelocityExecutor.getInstance().getMessages().getNoHubServerAvailable()
                )
        ));
    }

    @Nonnull
    public List<String> getAliases() {
        return aliases;
    }
}
