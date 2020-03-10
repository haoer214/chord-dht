package bupt.fnl.dht.service.impl;

import bupt.fnl.dht.domain.FingerTable;
import bupt.fnl.dht.domain.Message;
import bupt.fnl.dht.domain.Node;
import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.network.MakeConnection;
import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static bupt.fnl.dht.jdbc.DataBase.deleteTable;
import static bupt.fnl.dht.jdbc.DataBase.transferAll;

public class NodeServiceImpl implements NodeService {

    NodeInfo nodeInfo;
    String knownIP, knownPort;
    Node me;
    List<Node> nodeList;
    NodeService nodeService;
    MakeConnection makeConnection;
    Print print;
    /**
     * initNode - 初始化节点信息
     *
     * 通过输入参数的个数区分第一个节点和之后其他节点的加入
     *
     *      args参数为2位: [当前节点监听端口] [numNodes]
     *      说明是第一个加入的节点 args[0] args[1]
     *
     *      args参数为4位: [已知节点IP] [已知节点监听端口] [当前节点监听端口] [numNodes]
     *      说明不是第一个加入的节点 args[0]  args[1]  args[2]  args[3]
     */
    public void initNode(String... args) {

        if (args.length == 2){
            String myPort = args[0];
            me = getCurrentNode(Integer.parseInt(args[1]));
            nodeInfo.setMyPort(myPort);
            nodeInfo.setMe(me);
            nodeInfo.setPred(me);

        } else if(args.length == 4){

            knownIP = args[0];
            knownPort = args[1];
            nodeInfo.setKnownHostIP(knownIP);
            nodeInfo.setKnownHostPort(knownPort);
            String myPort = args[2];
            me = getCurrentNode(Integer.parseInt(args[3]));
            nodeInfo.setMyPort(myPort);
            nodeInfo.setMe(me);

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("findPred/" + nodeInfo.getMyID());
            Message result = makeConnection.makeConnectionByObject(knownIP, knownPort, request);
            String[] tokens = result.getInitInfo().split("/");
            Node pred = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
            nodeInfo.setPred(pred);

        } else {
            System.out.println("Syntax one - NodeDHT-First [LocalPortNumber] [numNodes]");
            System.out.println("Syntax two - NodeDHT-Others [Known-HostIP]  [Known-HostPortNumber] [LocalPortNumber] [numNodes]");
            System.out.println("[LocalPortNumber] = is the port number which the main.java.bupt.fnl.dht.domain.Node will be listening waiting for connections.");
            System.out.println("[Known-HostName] = is the hostIP of one DHTNode already in the net.");
            System.out.println("[Known-HostPortNumber] = is the port which the Known-Host listening waiting for connections.");
            System.exit(1);
        }
    }

    /**
     * getCurrentNode - 获取当前节点
     * @param maxNumNodes 用户输入的最大节点数
     * @return 返回当前节点
     */
    public Node getCurrentNode(int maxNumNodes) {
        // 路由表条数
        int m = (int) Math.ceil(Math.log(maxNumNodes) / Math.log(2));
        nodeInfo.setM(m);
        // 路由表
        nodeInfo.setFinger(new FingerTable[m+1]);
        // 网络最大节点数
        nodeInfo.setNumDHT((int)Math.pow(2,m));
        // 当前节点 IP
        InetAddress mIP = null;
        try {
            mIP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("ip获取失败...");
            System.exit(1);
        }
        String myIP = mIP.getHostAddress();
        nodeInfo.setMyIP(myIP);
        System.out.println("本节点IP地址: " + myIP);
        // 当前节点端口
        String myPort = nodeInfo.getMyPort();
        // 当前节点 ID
        int myID = nodeService.getNodeID(myIP,myPort);
        nodeInfo.setMyID(myID);
        return new Node(myID,myIP,myPort);
    }


    // 获取新加入网络的节点ID
    public int getNodeID(String nodeIP, String nodePort) {

        int nodeID;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            String hashString = nodeIP + nodePort;
            md.update(hashString.getBytes());
            byte[] hashBytes = md.digest();
            BigInteger hashNum = new BigInteger(1, hashBytes);
            nodeID = Math.abs(hashNum.intValue()) % nodeInfo.getNumDHT();

            // 判断节点hash是否重复
            Set<Integer> nodesIDSet = new HashSet<>();
            nodeList.forEach(node -> nodesIDSet.add(node.getID()));

            while (nodesIDSet.contains(nodeID)) {
                md.reset();
                md.update(hashBytes);
                hashBytes = md.digest();
                hashNum = new BigInteger(1, hashBytes);
                nodeID = Math.abs(hashNum.intValue()) % nodeInfo.getNumDHT();
            }

            System.out.println("新节点加入... ");
            return nodeID;

        } catch (NoSuchAlgorithmException e) {
            System.out.println("获取节点ID失败...");
            return -1;
        }
    }

    // 处理返回的m个node信息并生成list(路由表中最多只有m个node)
    public void getNode(String str) {
        String[] tokens = str.split("/");
        Node newNode;
        for (int i = 1; i <= (tokens.length / 3); i++) {
            newNode = new Node(Integer.parseInt(tokens[3 * (i - 1)]), tokens[1 + 3 * (i - 1)], tokens[2 + 3 * (i - 1)]);
            nodeList.add(newNode);
        }
    }

    // 创建节点列表
    public void buildNodeList() {
        nodeList = nodeInfo.getNodeList();
        nodeList.add(me);
        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("load/");
        Message result = makeConnection.makeConnectionByObject(knownIP, knownPort, request);
        getNode(result.getInitInfo());
    }


    // 更新nodeList
    public void updateList(Node node) {
        nodeInfo.getNodeList().add(node);
        System.out.println();
        System.out.println("【系统提示】- "+"新节点 "+node.getID()+"加入DHT网络");
        print.printNodeInfo();
    }

    // 更新其它节点的nodeList
    public void updateOthersList() {
        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("updateList/"+me.getID()+"/"+me.getIP()+"/"+me.getPort());
        for (Node node : nodeList) {
            if (node == me) {
                continue;
            }
            makeConnection.makeConnectionByObject(node.getIP(), node.getPort(), request);
        }
    }

    // 通过当前节点的路由表查询某个NID的前继节点
    public Node find_predecessor(int id){
        Node n = me;
        FingerTable[] finger = nodeInfo.getFinger();
        int myID = n.getID();
        int succID = finger[1].getSuccessor().getID();
        int normalInterval = 1;
        if (myID >= succID)
            normalInterval = 0;

        while ((normalInterval==1 && (id <= myID || id > succID)) ||
                (normalInterval==0 && (id <= myID && id > succID))) {

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("closetPred/" + id);
            Message result = makeConnection.makeConnectionByObject(n.getIP(),n.getPort(),request);
            String[] tokens = result.getInitInfo().split("/");

            n = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);

            myID = n.getID();

            request.setInitInfo("getSuc/");
            Message result2 = makeConnection.makeConnectionByObject(n.getIP(),n.getPort(),request);
            String[] tokens2 = result2.getInitInfo().split("/");

            succID = Integer.parseInt(tokens2[0]);

            if (myID >= succID)
                normalInterval = 0;
            else
                normalInterval = 1;
        }
        return n;
    }
    // 通过当前节点的路由表查询某个NID的后继节点
    public Node find_successor(int id) {
        Node n = find_predecessor(id);

        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("getSuc/");
        Message result = makeConnection.makeConnectionByObject(n.getIP(),n.getPort(),request);
        String[] tokens = result.getInitInfo().split("/");
        return new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
    }

    // 设置当前节点的前继节点
    public void setPredecessor(Node n) // throws RemoteException
    {
        nodeInfo.setPred(n);
    }

    // 获取当前节点的前继节点
    public Node getPredecessor() //throws RemoteException
    {
        return nodeInfo.getPred();
    }

    // 获取当前节点的后继节点
    public Node getSuccessor() {
        FingerTable[] finger = nodeInfo.getFinger();
        return finger[1].getSuccessor();
    }

    // 获取当前节点路由表中距离目标id最近的节点
    public Node closet_preceding_finger(int id)
    {
        int normalInterval = 1;
        int myID = me.getID();
        if (myID >= id) {
            normalInterval = 0;
        }
        FingerTable[] finger = nodeInfo.getFinger();
        for (int i = nodeInfo.getM(); i >= 1; i--) {
            int nodeID = finger[i].getSuccessor().getID();
            if (normalInterval == 1) {
                if (nodeID > myID && nodeID < id)
                    return finger[i].getSuccessor();
            } else {
                if (nodeID > myID || nodeID < id)
                    return finger[i].getSuccessor();
            }
        }
        return me;
    }



    // 广播消息
    public void noticeOthers(String message) {
        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo(message);
        Iterator<Node> iterator = nodeList.iterator();
        while(iterator.hasNext()) {
            Node node =iterator.next();
            if(node==me)
                continue;
            makeConnection.makeConnectionByObject(node.getIP(),node.getPort(),request);
        }
        System.out.println("已通知所有节点");
    }

    public String loadNode(){
        Node node;
        String results="";
        for (int i = 0; i < nodeList.size() - 1; i++) {
            node = nodeList.get(i);
            results = results + node.getID() + "/" + node.getIP() + "/" + node.getPort() + "/";
        }
        results = results + nodeList.get(nodeList.size() - 1).getID() + "/" + nodeList.get(nodeList.size() - 1).getIP() + "/" + nodeList.get(nodeList.size() - 1).getPort() + "/";
        return results;
    }

    /**
     * beforeExit() - 节点退出网络
     * 分 单个节点、两个节点、多个节点 退出3种情况
     */
    public void beforeExit() {
        if (nodeList.size() == 1) {
            // 节点退出时，删除数据表
            deleteTable(me.getID());
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("【系统提示】- 节点 " + me.getID() + " 已经退出DHT网络");
            System.out.println("【系统提示】- 网络已关闭");
        } else {
            FingerTable[] finger = nodeInfo.getFinger();
            Node pred = nodeInfo.getPred();
            // 数据迁移到后继节点
            transferAll(me.getID(), finger[1].getSuccessor().getID());
            Message request = new Message();
            request.setInitNode_flag(1);
            if (nodeList.size() == 2)
                request.setInitInfo("quitOfTwoNodes/");
            else
                request.setInitInfo("quitOfManyNodes/" + pred.getID() + "/" + pred.getIP() + "/" + pred.getPort());
            makeConnection.makeConnectionByObject(finger[1].getSuccessor().getIP(), finger[1].getSuccessor().getPort(), request);
            // 删除数据表
            deleteTable(me.getID());
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("【系统提示】- 节点 " + me.getID() + " 已经退出DHT网络");
        }
    }
}
