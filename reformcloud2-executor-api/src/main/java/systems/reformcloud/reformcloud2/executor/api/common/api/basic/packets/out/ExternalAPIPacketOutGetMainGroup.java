package systems.reformcloud.reformcloud2.executor.api.common.api.basic.packets.out;

import systems.reformcloud.reformcloud2.executor.api.common.api.basic.ExternalAPIImplementation;
import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.JsonPacket;

public final class ExternalAPIPacketOutGetMainGroup extends JsonPacket {

    public ExternalAPIPacketOutGetMainGroup(String name) {
        super(ExternalAPIImplementation.EXTERNAL_PACKET_ID + 20, new JsonConfiguration().add("name", name));
    }
}
