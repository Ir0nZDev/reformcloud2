package systems.reformcloud.reformcloud2.permissions.sponge.subject.util;

import systems.reformcloud.reformcloud2.executor.api.api.API;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessInformation;
import systems.reformcloud.reformcloud2.permissions.util.group.PermissionGroup;
import systems.reformcloud.reformcloud2.permissions.util.permission.PermissionNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class SubjectGroupPermissionCalculator {

    private SubjectGroupPermissionCalculator() {
        throw new UnsupportedOperationException();
    }

    public static Map<String, Boolean> getPermissionsOf(PermissionGroup group) {
        Map<String, Boolean> out = new HashMap<>();
        final ProcessInformation current = API.getInstance().getCurrentProcessInformation();
        Collection<PermissionNode> permissionNodes = group.getPerGroupPermissions().get(current.getProcessGroup().getName());
        if (permissionNodes != null) {
            permissionNodes.forEach(e -> {
                if (!e.isValid()) {
                    return;
                }

                out.put(e.getActualPermission(), e.isSet());
            });
        }

        group.getPermissionNodes().forEach(e -> {
            if (!e.isValid()) {
                return;
            }

            out.put(e.getActualPermission(), e.isSet());
        });
        return out;
    }
}
