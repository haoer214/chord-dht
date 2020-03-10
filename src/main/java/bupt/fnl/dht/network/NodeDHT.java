package bupt.fnl.dht.network;

import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.runnable.CommandThread;
import bupt.fnl.dht.runnable.SocketThread;
import bupt.fnl.dht.service.FingerService;
import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bupt.fnl.dht.jdbc.DataBase.*;

/**
 * 入口类，用于搭建dht网络
 */
public class NodeDHT {

    /**
     * -----【 Main 函数 】-----
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        NodeInfo nodeInfo = null;
        NodeService nodeService = null;
        FingerService fingerService = null;
        MakeConnection makeConnection = null; // 用于节点间传输数据
        Print print = null;



        System.out.println("*************启动DHT网络************");
        nodeService.initNode(args);
        System.out.println("本节点 ID: " + nodeInfo.getMe().getID() + "  前继节点 ID: " + nodeInfo.getPred().getID());
        fingerService.initTable(args);

        // 线程池
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // 监听后台键盘输入
        threadPool.execute(new CommandThread());

        // 监听其他节点连接
        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket newCon = serverSocket.accept();
            threadPool.execute(new SocketThread(newCon));
        }
    }
















}