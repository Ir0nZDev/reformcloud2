package systems.reformcloud.reformcloud2.executor.api.network.packets.out;

import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.network.NetworkUtil;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.JsonPacket;

import java.util.UUID;

public final class APIPacketOutPlayerCommandExecute extends JsonPacket {

    public APIPacketOutPlayerCommandExecute(String name, UUID uuid, String message) {
        super(NetworkUtil.PLAYER_INFORMATION_BUS + 5, new JsonConfiguration()
                .add("name", name)
                .add("uuid", uuid)
                .add("command", message)
        );
    }
}
