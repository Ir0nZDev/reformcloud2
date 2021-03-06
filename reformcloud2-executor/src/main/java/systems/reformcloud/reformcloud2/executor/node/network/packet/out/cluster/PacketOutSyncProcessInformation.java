package systems.reformcloud.reformcloud2.executor.node.network.packet.out.cluster;

import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.network.NetworkUtil;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.JsonPacket;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessInformation;

import java.util.Collection;

public class PacketOutSyncProcessInformation extends JsonPacket {

    public PacketOutSyncProcessInformation(Collection<ProcessInformation> processInformation) {
        super(NetworkUtil.NODE_TO_NODE_BUS + 12, new JsonConfiguration().add("info", processInformation));
    }
}
