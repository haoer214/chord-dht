package bupt.fnl.dht.runnable;

import bupt.fnl.dht.node.FingerTable;
import bupt.fnl.dht.node.Node;
import bupt.fnl.dht.node.print.Print;

import java.io.IOException;
import java.util.List;

import static bupt.fnl.dht.jdbc.DataBase.createTable;
import static bupt.fnl.dht.jdbc.DataBase.initParam;

/**
 * 第一个加入dht网络的节点启动的线程
 */
public class FirstNodeThread implements Runnable{

    private Node me;                // 当前节点
    private List<Node> nodeList;    // 节点列表
    private FingerTable[] finger;   // 路由表
    private int m;                  // 路由表条数
    private int numDHT;             // 网络最大节点数
    private Print print;            // 打印信息


    public FirstNodeThread(Node me, List<Node> nodeList, FingerTable[] finger, int m, int numDHT) {
        this.me = me;
        this.nodeList = nodeList;
        this.finger = finger;
        this.m = m;
        this.numDHT = numDHT;
    }

    @Override
    public void run() {
        try {
            initParam(); // 初始化数据库连接信息
        } catch (IOException e) {
            System.out.println("数据库连接失败，请尝试重新连接...");
            System.exit(1);
        }

        try {
            createTable(me.getID()); // 创建表
            System.out.println("成功创建表【node" + me.getID() + "】");
        } catch (Exception e) {
            System.out.println("表创建失败，请重新创建...");
            System.exit(1);
        }

        /*
          以上是数据库连接部分，以下是节点配置部分
         */

        System.out.println("正在创建路由表...");
        for (int i = 1; i <= m; i++) {
            finger[i] = new FingerTable();
            finger[i].setStart((me.getID() + (int)Math.pow(2,i-1)) % numDHT);
            if (i == m)
                finger[i].setInterval(finger[i].getStart(),finger[1].getStart()-1);
            else
                finger[i].setInterval(finger[i].getStart(),finger[i+1].getStart());
            finger[i].setSuccessor(me);
        }
        System.out.println("路由表创建完成，此节点是网络中唯一节点！");
        print.printFingerInfo();

        System.out.println("开始创建节点列表...");
        nodeList.add(me);
        System.out.println("节点列表创建完成！");

        System.out.println();
        System.out.println("【系统提示】- "+"节点 " + me.getID() + " 已经在DHT网络中！");
        print.printNodeInfo();
    }
}

