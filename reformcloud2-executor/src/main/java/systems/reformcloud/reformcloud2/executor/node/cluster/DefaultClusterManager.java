package systems.reformcloud.reformcloud2.executor.node.cluster;

import systems.reformcloud.reformcloud2.executor.api.common.node.NodeInformation;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.node.cluster.ClusterManager;
import systems.reformcloud.reformcloud2.executor.api.node.cluster.InternalNetworkCluster;
import systems.reformcloud.reformcloud2.executor.controller.network.packets.out.event.ControllerEventProcessClosed;
import systems.reformcloud.reformcloud2.executor.node.NodeExecutor;
import systems.reformcloud.reformcloud2.executor.node.cluster.sync.DefaultClusterSyncManager;

import java.util.ArrayList;
import java.util.Collection;

public class DefaultClusterManager implements ClusterManager {

    private final Collection<NodeInformation> nodeInformation = new ArrayList<>();

    private NodeInformation head;

    @Override
    public void init() {
        nodeInformation.add(NodeExecutor.getInstance().getNodeNetworkManager().getCluster().getSelfNode());
    }

    @Override
    public void handleNodeDisconnect(InternalNetworkCluster cluster, String name) {
        Streams.allOf(Streams.newList(nodeInformation), e -> e.getName().equals(name)).forEach(e -> {
            this.nodeInformation.remove(e);
            cluster.getConnectedNodes().remove(e);
            Streams.allOf(Streams.newList(NodeExecutor.getInstance().getNodeNetworkManager().getNodeProcessHelper().getClusterProcesses()),
                    i -> i.getNodeUniqueID().equals(e.getNodeUniqueID())
            ).forEach(i -> {
                NodeExecutor.getInstance().getNodeNetworkManager().getNodeProcessHelper().handleProcessStop(i);
                DefaultClusterSyncManager.sendToAllExcludedNodes(new ControllerEventProcessClosed(i));
            });

            if (head != null && head.getNodeUniqueID().equals(e.getNodeUniqueID())) {
                head = null;
            }
        });

        recalculateHead();
    }

    @Override
    public void handleConnect(InternalNetworkCluster cluster, NodeInformation nodeInformation) {
        if (this.nodeInformation.stream().anyMatch(e -> e.getName().equals(nodeInformation.getName()))) {
            return;
        }

        this.nodeInformation.add(nodeInformation);
        cluster.getConnectedNodes().add(nodeInformation);
        this.recalculateHead();
    }

    @Override
    public int getOnlineAndWaiting(String groupName) {
        int onlineOrWaiting = Streams.allOf(NodeExecutor.getInstance().getNodeNetworkManager().getNodeProcessHelper().getClusterProcesses(),
                e -> e.getProcessGroup().getName().equals(groupName) && e.getProcessState().isValid()).size();
        onlineOrWaiting += Streams.deepFilter(NodeExecutor.getInstance().getNodeNetworkManager().getQueuedProcesses(),
                v -> v.getValue().equals(groupName)).size();
        return onlineOrWaiting;
    }

    @Override
    public int getWaiting(String groupName) {
        return Streams.deepFilter(NodeExecutor.getInstance().getNodeNetworkManager().getQueuedProcesses(), v -> v.getValue().equals(groupName)).size();
    }

    @Override
    public NodeInformation getHeadNode() {
        if (head == null) {
            recalculateHead();
        }

        return head;
    }

    private void recalculateHead() {
        for (NodeInformation information : nodeInformation) {
            if (head == null) {
                head = information;
                continue;
            }

            if (information.getStartupTime() < head.getStartupTime()) {
                head = information;
            }
        }
    }
}
