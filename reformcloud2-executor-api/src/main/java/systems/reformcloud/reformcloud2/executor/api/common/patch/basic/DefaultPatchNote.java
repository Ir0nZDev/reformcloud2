package systems.reformcloud.reformcloud2.executor.api.common.patch.basic;

import systems.reformcloud.reformcloud2.executor.api.common.patch.PatchNote;

public final class DefaultPatchNote implements PatchNote {

    public DefaultPatchNote(String newVersion, String updateMessage, String name) {
        this.newVersion = newVersion;
        this.name = name;
        this.updateMessage = updateMessage;
    }

    private String newVersion;

    private String updateMessage;

    private String name;

    @Override
    public String newVersion() {
        return newVersion;
    }

    @Override
    public String updateMessage() {
        return updateMessage;
    }

    @Override
    public String getName() {
        return name;
    }
}