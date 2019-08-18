package de.klaro.reformcloud2.executor.api.common.commands.basic.manager;

import de.klaro.reformcloud2.executor.api.common.commands.AllowedCommandSources;
import de.klaro.reformcloud2.executor.api.common.commands.Command;
import de.klaro.reformcloud2.executor.api.common.commands.dispatcher.command.CommandEvent;
import de.klaro.reformcloud2.executor.api.common.commands.manager.CommandManager;
import de.klaro.reformcloud2.executor.api.common.commands.source.CommandSource;
import de.klaro.reformcloud2.executor.api.common.language.LanguageManager;
import de.klaro.reformcloud2.executor.api.common.utility.list.Links;

import java.util.*;
import java.util.function.Consumer;

public final class DefaultCommandManager implements CommandManager {

    private static final String NO_PERMISSIONS = "You do not have permission to execute this command";

    private final List<Command> commands = new ArrayList<>();

    private final Map<Command, String> noPermissionMessagePerCommand = new HashMap<>();

    @Override
    public CommandManager register(Command command) {
        dispatchCommandEvent(CommandEvent.ADD, command);
        return this;
    }

    @Override
    public CommandManager register(Class<? extends Command> command) {
        try {
            register(command.newInstance());
        } catch (final InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return this;
    }

    @Override
    public void unregisterCommand(Command command) {
        dispatchCommandEvent(CommandEvent.REMOVE, command);
    }

    @Override
    public Command unregisterAndGetCommand(String line) {
        line = line.toLowerCase();
        for (Command command : Links.newList(commands)) {
            if (command.mainCommand().equals(line) || command.aliases().contains(line)) {
                unregisterCommand(command);
                return command;
            }
        }

        return null;
    }

    @Override
    public Command dispatchCommandEvent(CommandEvent commandEvent, Command command) {
        switch (commandEvent) {
            case ADD: {
                commands.add(command);
                return command;
            }

            case REMOVE: {
                commands.remove(command);
                noPermissionMessagePerCommand.remove(command);
                return command;
            }

            case UNREGISTER_ALL: {
                commands.clear();
                return null;
            }

            case FIND:
            case UPDATE:
            default: {
                throw new UnsupportedOperationException("Not supported operation");
            }
        }
    }

    @Override
    public Command dispatchCommandEvent(CommandEvent commandEvent, Command command, Command update) {
        switch (commandEvent) {
            case UNREGISTER_ALL:
            case ADD:
            case REMOVE: {
                return dispatchCommandEvent(commandEvent, command);
            }

            case UPDATE: {
                dispatchCommandEvent(CommandEvent.REMOVE, command);
                return dispatchCommandEvent(CommandEvent.ADD, command);
            }

            case FIND:
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public Command dispatchCommandEvent(CommandEvent commandEvent, Command command, Command update, String line) {
        switch (commandEvent) {
            case UNREGISTER_ALL:
            case UPDATE:
            case REMOVE:
            case ADD: {
                return dispatchCommandEvent(commandEvent, command, update);
            }

            case FIND: {
                line = line.toLowerCase();
                for (Command cmd : commands) {
                    if (cmd.mainCommand().equals(line) || cmd.aliases().contains(line)) {
                        return cmd;
                    }
                }

                return null;
            }

            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public List<Command> getCommands() {
        return Links.unmodifiable(commands);
    }

    @Override
    public void unregisterAll() {
        dispatchCommandEvent(CommandEvent.UNREGISTER_ALL, null);
    }

    @Override
    public Command getCommand(String command) {
        return dispatchCommandEvent(CommandEvent.FIND, null, null, command);
    }

    @Override
    public Command findCommand(String commandPreLine) {
        commandPreLine = commandPreLine.toLowerCase();
        for (Command command : commands) {
            if (command.mainCommand().startsWith(commandPreLine)) {
                return command;
            }

            for (String alias : command.aliases()) {
                if (alias.startsWith(commandPreLine)) {
                    return command;
                }
            }
        }

        return null;
    }

    @Override
    public void register(String noPermissionMessage, Command command) {
        dispatchCommandEvent(CommandEvent.ADD, command);
        this.noPermissionMessagePerCommand.put(command, noPermissionMessage);
    }

    @Override
    public void dispatchCommand(CommandSource commandSource, AllowedCommandSources commandSources, String commandLine, Consumer<String> result) {
        commandLine = commandLine.contains(" ") ? commandLine : commandLine + " ";
        String[] split = commandLine.split(" ");

        Command command = getCommand(split[0]);
        if (command == null) {
            result.accept(LanguageManager.get("command-unknown", split[0]));
            return;
        }

        if (!commandSource.hasPermission(command.permission())) {
            String noPermMessage = this.noPermissionMessagePerCommand.getOrDefault(command, NO_PERMISSIONS);
            result.accept(noPermMessage);
            return;
        }

        if (!command.sources().equals(AllowedCommandSources.ALL) && !command.sources().equals(commandSources)) {
            result.accept(LanguageManager.get("command-source-not-allowed", commandSources.name()));
            return;
        }

        if (split.length == 1) {
            String[] strings = new String[0];
            command.handleCommand(commandSource, strings);
        } else {
            String[] strings = Arrays.copyOfRange(split, 1, split.length);
            command.handleCommand(commandSource, strings);
        }
    }
}