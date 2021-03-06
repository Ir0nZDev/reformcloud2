package systems.reformcloud.reformcloud2.executor.api.common.commands.basic.commands.shared;

import systems.reformcloud.reformcloud2.executor.api.common.commands.Command;
import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.GlobalCommand;
import systems.reformcloud.reformcloud2.executor.api.common.commands.manager.CommandManager;
import systems.reformcloud.reformcloud2.executor.api.common.commands.source.CommandSource;
import systems.reformcloud.reformcloud2.executor.api.common.language.LanguageManager;

import javax.annotation.Nonnull;

public final class CommandHelp extends GlobalCommand {

    public CommandHelp(CommandManager commandManager) {
        super("help", null, GlobalCommand.DEFAULT_DESCRIPTION, "ask", "?");
        this.commandManager = commandManager;
    }

    private final CommandManager commandManager;

    @Override
    public void describeCommandToSender(@Nonnull CommandSource source) {
        source.sendMessage(LanguageManager.get("command-help-description"));
    }

    @Override
    public boolean handleCommand(@Nonnull CommandSource commandSource, @Nonnull String[] strings) {
        if (strings.length != 1) {
            commandSource.sendMessage("ReformCloud git:runner:"
                    + System.getProperty("reformcloud.runner.version", "c-build")
                    + ":"
                    + CommandHelp.class.getPackage().getSpecificationVersion()
                    + " by derklaro and ReformCloud-Community"
            );
            commandSource.sendMessage(" ");

            commandManager.getCommands().forEach(command -> commandSource.sendMessage("   -> " + command.mainCommand() + " " + command.aliases()));
            commandSource.sendMessage(LanguageManager.get("command-help-use"));
            return true;
        }

        Command command = commandManager.getCommand(strings[0]);
        if (command == null) {
            commandSource.sendMessage(LanguageManager.get("command-help-command-unknown"));
            return true;
        }

        command.describeCommandToSender(commandSource);
        return true;
    }
}
