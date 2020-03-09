package bupt.fnl.dht.node.info;

import bupt.fnl.dht.node.FingerTable;
import bupt.fnl.dht.node.Node;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 获取新加入网络的节点信息
 */
public class NodeInfo {

    private List<Node> nodeList;    // 节点列表
    private int numDHT;             // 网络最大节点数

    // 获取新加入的dht节点 ID
    public int getNodeInfo(String nodeIP, String nodePort) {

        int nodeID;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            String hashString = nodeIP + nodePort;
            md.update(hashString.getBytes());
            byte[] hashBytes = md.digest();
            BigInteger hashNum = new BigInteger(1, hashBytes);
            nodeID = Math.abs(hashNum.intValue()) % numDHT;

            // 判断节点hash是否重复
            Set<Integer> nodesIDSet = new HashSet<>();
            nodeList.forEach(node -> nodesIDSet.add(node.getID()));

            while (nodesIDSet.contains(nodeID)) {
                md.reset();
                md.update(hashBytes);
                hashBytes = md.digest();
                hashNum = new BigInteger(1, hashBytes);
                nodeID = Math.abs(hashNum.intValue()) % numDHT;
            }

            System.out.println("新节点加入... ");
            return nodeID;

        } catch (NoSuchAlgorithmException e) {
            System.out.println("获取节点ID失败...");
            return -1;
        }
    }
}
