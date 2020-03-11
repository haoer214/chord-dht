package bupt.fnl.dht.service.impl;

import bupt.fnl.dht.domain.FingerTable;
import bupt.fnl.dht.domain.Message;
import bupt.fnl.dht.domain.Node;
import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.network.MakeConnection;
import bupt.fnl.dht.dao.DataBase;
import bupt.fnl.dht.service.FingerService;
import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("fingerService")
public class FingerServiceImpl implements FingerService {
    @Autowired
    NodeInfo nodeInfo;
    int m, numDHT;
    Node me, pred;
    List<Node> nodeList;
    FingerTable[] finger;
    @Autowired
    NodeService nodeService;
    @Autowired
    MakeConnection makeConnection;
    @Autowired
    DataBase dataBase;
    @Autowired
    Print print;
    /**
     * initTable - 初始化数据库、路由表
     * @param args
     */
    public void initTable(String... args){

        m = nodeInfo.getM();
        numDHT = nodeInfo.getNumDHT();
        me = nodeInfo.getMe();
        pred = nodeInfo.getPred();
        finger = nodeInfo.getFinger();


        dataBase.initParam(); // 初始化数据库连接信息

        try {
            dataBase.createTable(me.getID()); // 创建表
            System.out.println("成功创建数据库表【node" + me.getID() + "】");
        } catch (Exception e) {
            System.out.println("表创建失败，请重新创建...");
            System.exit(1);
        }

        System.out.println();
        System.out.println("正在创建路由表...");
        for (int i = 1; i <= m; i++) {
            finger[i] = new FingerTable();
            finger[i].setStart((me.getID() + (int)Math.pow(2,i-1)) % numDHT);
            finger[i].setSuccessor(me);
        }
        for (int i = 1; i < m; i++) {
            finger[i].setInterval(finger[i].getStart(),finger[i+1].getStart());
        }
        finger[m].setInterval(finger[m].getStart(),finger[1].getStart()-1);


        if (args.length == 2) {
            System.out.println("路由表创建完成，此节点是网络中唯一节点！");
            print.printFingerInfo();
            System.out.println("开始创建节点列表...");
            nodeList = new ArrayList<>();
            nodeList.add(me);
            nodeInfo.setNodeList(nodeList);
            System.out.println("节点列表创建完成！");

        } else if(args.length == 4){
            try {
                init_finger_table(pred);
                System.out.println("路由表创建完成！");
                print.printFingerInfo();
                update_others();
                System.out.println("其它节点路由表已更新。");

                System.out.println("开始创建节点列表...");
                nodeService.buildNodeList();
                System.out.println("节点列表创建完成！");
                nodeService.updateOthersList();
                System.out.println("其它节点的列表已更新。");
            } catch (Exception e) {
                System.out.println("路由表创建失败...");
                System.exit(1);
            }
            // 将部分数据从后继迁移到当前节点
            try {
                dataBase.transferPart(me.getID(), finger[1].getSuccessor().getID());
            } catch (Exception e) {
                System.out.println("节点加入时，数据迁移失败！");
                System.exit(1);
            }
        }

        System.out.println();
        System.out.println("【系统提示】- " + "节点 " + me.getID() + " 已经在DHT网络中！");
        print.printNodeInfo();
    }

    // 初始化路由表
    public void init_finger_table(Node n) {
        int myID, nextID;

        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("findSuc/" + finger[1].getStart());
        Message result = makeConnection.makeConnectionByObject(n.getIP(),n.getPort(),request);

        String[] tokens = result.getInitInfo().split("/");
        finger[1].setSuccessor(new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]));

        request.setInitInfo("getPred");
        Message result2 = makeConnection.makeConnectionByObject(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request);
        String[] tokens2 = result2.getInitInfo().split("/");
        pred = new Node(Integer.parseInt(tokens2[0]),tokens2[1],tokens2[2]);

        request.setInitInfo("setPred/" + me.getID() + "/" + me.getIP() + "/" + me.getPort());
        makeConnection.makeConnectionByObject(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request);

        int normalInterval;
        for (int i = 1; i <= m-1; i++) {

            myID = me.getID();
            nextID = finger[i].getSuccessor().getID();

            if (myID >= nextID)
                normalInterval = 0;
            else normalInterval = 1;

            if ( (normalInterval==1 && (finger[i+1].getStart() >= myID && finger[i+1].getStart() <= nextID))
                    || (normalInterval==0 && (finger[i+1].getStart() >= myID || finger[i+1].getStart() <= nextID))) {

                finger[i+1].setSuccessor(finger[i].getSuccessor());
            } else {

                request.setInitInfo("findSuc/" + finger[i+1].getStart());
                Message result4 = makeConnection.makeConnectionByObject(n.getIP(),n.getPort(),request);
                String[] tokens4 = result4.getInitInfo().split("/");

                int fiStart = finger[i+1].getStart();
                int succ = Integer.parseInt(tokens4[0]);
                int fiSucc = finger[i+1].getSuccessor().getID();
                if (fiStart > succ)
                    succ = succ + numDHT;
                if (fiStart > fiSucc)
                    fiSucc = fiSucc + numDHT;

                if ( fiStart <= succ && succ <= fiSucc ) {
                    finger[i+1].setSuccessor(new Node(Integer.parseInt(tokens4[0]),tokens4[1],tokens4[2]));
                }
            }
        }
    }

    // 更新影响到节点的路由表（波及到的节点范围：向前1～2^m-1）
    public void update_others() {
        Node p;
        for (int i = 1; i <= m; i++) {
            int id = me.getID() - (int)Math.pow(2,i-1) + 1;
            if (id < 0)
                id = id + numDHT;

            p = nodeService.find_predecessor(id);

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("updateFing/" + me.getID() + "/" + me.getIP() + "/" + me.getPort() + "/" + i);
            makeConnection.makeConnectionByObject(p.getIP(),p.getPort(),request);
        }
    }
    // 更新路由表
    public void update_finger_table(Node s, int i)
    {
        Node p;
        int normalInterval;
        int myID = me.getID();
        int nextID = finger[i].getSuccessor().getID();
        if (myID >= nextID)
            normalInterval = 0;
        else
            normalInterval = 1;

        if ( ((normalInterval==1 && (s.getID() >= myID && s.getID() < nextID)) ||
                (normalInterval==0 && (s.getID() >= myID || s.getID() < nextID)))
                && (me.getID() != s.getID() ) ) {

            finger[i].setSuccessor(s);
            p = pred;

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("updateFing/" + s.getID() + "/" + s.getIP() + "/" + s.getPort() + "/" + i);
            makeConnection.makeConnectionByObject(p.getIP(),p.getPort(),request);
        }
    }

    public void quit_update_finger_table(Node s, int exitID){
        for(int i = 1; i <= m; i++){
            if (finger[i].getSuccessor().getID() == exitID){
                finger[i].setSuccessor(s);
            }
        }
    }
}
