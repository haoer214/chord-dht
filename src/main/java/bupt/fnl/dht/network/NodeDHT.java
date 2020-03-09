package bupt.fnl.dht.network;

import bupt.fnl.dht.node.FingerTable;
import bupt.fnl.dht.node.info.NodeInfo;
import bupt.fnl.dht.utils.Message;
import bupt.fnl.dht.node.Node;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.app.chaincode.authority.QueryAuthority;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static bupt.fnl.dht.jdbc.DataBase.*;
import static bupt.fnl.dht.utils.Decryption.*;
import static bupt.fnl.dht.network.Communication.*;

/**
 * 搭建dht网络
 */
public class NodeDHT {

    private int ID;
    private Socket connection;
    private static ServerSocket serverSocket = null;
    private static Node me, pred;
    private static int m;
    private static int numDHT;
    private static int busy;
    private static Object object = new Object();
    private static FingerTable[] finger;
    private static String knownHostIP;
    private static String knownHostPort;
    private static String myIP;
    private static String myPort;
    private static List<Node> nodeList = new ArrayList<>();


    private static NodeInfo nodeInfo; // 新加入网络的节点信息

    public NodeDHT(Socket s, int i) {
        this.connection = s;
        this.ID = i;
    }

    /**
     * 初始化节点信息
     *
     * 通过输入参数的个数区分第一个节点和之后其他节点的加入
     *
     *      args参数为2位: [当前节点监听端口] [numNodes]
     *      说明是第一个加入的节点 args[0] args[1]
     *
     *      args参数为4位: [已知节点IP] [已知节点监听端口] [当前节点监听端口] [numNodes]
     *      说明不是第一个加入的节点 args[0]  args[1]  args[2]  args[3]
     */
    public static void initNode(String... args) throws Exception{

        System.out.println("*************启动DHT网络************");

        if (args.length == 2){
            myPort = args[0];
            int maxNumNodes = Integer.parseInt(args[1]);
            me = getCurrentNode(maxNumNodes);
            pred = me;

        } else if(args.length == 4){
            knownHostIP = args[0];
            knownHostPort = args[1];
            myPort = args[2];
            int maxNumNodes = Integer.parseInt(args[3]);
            me = getCurrentNode(maxNumNodes);

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("findPred/"+initInfo);
            // 返回结果
            Message result = makeConnectionByObject(knownHostIP, knownHostPort, request);
            String[] tokens = result.getInitInfo().split("/");
            pred = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
        }
        System.out.println("本节点 ID:"+me.getID() + "   前继节点 ID:" +pred.getID());
    }
    public static Node getCurrentNode(int maxNumNodes) throws Exception{
        // 路由表条数
        m = (int) Math.ceil(Math.log(maxNumNodes) / Math.log(2));
        // 路由表
        finger = new FingerTable[m+1];
        // 网络最大节点数
        numDHT = (int)Math.pow(2,m);
        // 当前节点 IP
        InetAddress mIP = InetAddress.getLocalHost();
        myIP = mIP.getHostAddress();
        System.out.println("本节点IP地址: " + myIP);
        // 当前节点 ID
        int initInfo = nodeInfo.getNodeInfo(myIP,myPort);
        return new Node(initInfo,myIP,myPort);
    }

    public static void main(String[] args) throws Exception
    {

        if (args.length == 2) {

            initNode(args);


            // '第一个节点加入'的线程
            Runnable runnable = new NodeDHT(null, 0);
            Thread thread = new Thread(runnable);
            thread.start();

            // 监听键盘输入的线程
            Runnable inputRunnable = new NodeDHT(null, -2);
            Thread inputThread = new Thread(inputRunnable);
            inputThread.start();

            int count = 1;
            int port = Integer.parseInt(args[0]);
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                System.out.println("【系统提示】- " + "无法监听端口 - " + port);
                System.exit(-1);
            }

            while (true) {
                Socket newCon = serverSocket.accept();
                Runnable runnable2 = new NodeDHT(newCon, count++);
                Thread t = new Thread(runnable2);
                t.start();
            }
        } else if (args.length == 4) {


            // '非第一个节点加入'的线程
            Runnable runnable = new NodeDHT(null, -1);
            Thread thread = new Thread(runnable);
            thread.start();

            // 监听键盘输入的线程
            Runnable inputRunnable = new NodeDHT(null, -2);
            Thread inputThread = new Thread(inputRunnable);
            inputThread.start();

            int count = 1;
            int port = Integer.parseInt(myPort);
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                System.out.println("【系统提示】- " + "无法监听端口 - " + port);
                System.exit(-1);
            }

            while (true) {
                Socket newCon = serverSocket.accept();
                Runnable runnable2 = new NodeDHT(newCon, count++);
                Thread t = new Thread(runnable2);
                t.start();
            }
        } else {
            System.out.println("Syntax one - main.java.bupt.fnl.dht.network.NodeDHT-First [LocalPortNumber] [numNodes]");
            System.out.println("Syntax two - main.java.bupt.fnl.dht.network.NodeDHT-other [Known-HostIP]  [Known-HostPortNumber] [LocalPortNumber] [numNodes]");
            System.out.println("[LocalPortNumber] = is the port number which the main.java.bupt.fnl.dht.node.Node will be listening waiting for connections.");
            System.out.println("[Known-HostName] = is the hostIP of one DHTNode already in the net.");
            System.out.println("[Known-HostPortNumber] = is the port which the Known-Host listening waiting for connections.");
            System.exit(1);
        }	
      
    }




    @Override
    public void run() {


        // ID==-1时，非第一个加入的节点的入口
        else if (this.ID == -1) {
            // 初始化数据库连接信息
            try {
                initParam();
            } catch (Exception e) {
                System.out.println("数据库连接失败！");
            }
            // 创建数据表
            try {
                createTable(me.getID());
                System.out.println("成功创建数据表【node" + me.getID() + "】");
            } catch (Exception e) {
                System.out.println("数据表创建失败！");
            }

            /*
              以上是数据库连接部分，以下是节点配置部分
             */

            System.out.println("正在创建路由表...");
            for (int i = 1; i <= m; i++) {
                finger[i] = new FingerTable();
                finger[i].setStart((me.getID() + (int)Math.pow(2,i-1)) % numDHT);
            }
            for (int i = 1; i < m; i++) {
                finger[i].setInterval(finger[i].getStart(),finger[i+1].getStart()); 
            }
            finger[m].setInterval(finger[m].getStart(),finger[1].getStart()-1); 

            for (int i = 1; i <= m; i++) {
                    finger[i].setSuccessor(me);
            }

            try{
                    init_finger_table(pred);
                    System.out.println("路由表创建完成！");
                    printFingerInfo();
                    update_others();
                    System.out.println("其它节点路由表已更新。");
                    System.out.println("开始构建节点列表...");
                    buildNodeList();
                    System.out.println("节点列表创建完成！");
                    updateOthersList();
                    System.out.println("其它节点的列表已更新。");
            } catch (Exception e) {
            	e.printStackTrace();
            }

            /*
              以下是数据迁移部分
             */

            // 将部分数据从后继迁移到当前节点
            try {
                transferPart(me.getID(), finger[1].getSuccessor().getID());
            } catch (Exception e) {
                System.out.println("节点加入--数据迁移失败！");
            }


            try {
                // 释放对象锁
                finishJoining(me.getID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ID==-2时，指令集
        else if (this.ID == -2) {
            Scanner scan = new Scanner(System.in);
            label:
            while (scan.hasNext()) {
                String str1 = scan.nextLine();
                switch (str1) {
                    case "exit":  // 节点退出
                        try {
                            beforeExit();
                        } catch (Exception e) {
                            System.out.println("节点退出异常！");
                        }
                        break label;
                    case "printnodelist":
                        try {
                            printNodeInfo();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "printfinger":
                        printFingerInfo();
                        break;
                    case "printnum":
                        printNum();
                        break;
                    case "printpred":
                        printPred();
                        break;
                    case "printsuc":
                        printSuccessor();
                        break;
                    default:
                        System.out.print("命令格式不正确！请重新输入");
                        System.out.println();
                        break;
                }
            }
            scan.close();
            System.exit(0);
        }

        // ID等于其他值时，进行的是信息交互的部分
        else {
            /* 对象序列化 */
            try (
                    // 【注意】对于Object IO流，要先创建输出流对象，再创建输入流对象，不然程序会死锁
                    ObjectOutputStream outToControllerOrOtherNodes = new ObjectOutputStream(connection.getOutputStream());
                    ObjectInputStream inFromControllerOrOtherNodes = new ObjectInputStream(connection.getInputStream()))
            {

                Message received_message = (Message)inFromControllerOrOtherNodes.readObject();

                /* 收到Web消息后进行一系列权限校验 */
                outToControllerOrOtherNodes.writeObject(Communication.authentication(received_message));

            } catch (Exception e) {
                System.out.println("【系统提示】- "+"线程无法服务连接");
            	e.printStackTrace();
            }
        }
    }

    // 节点退出
    public void beforeExit() throws Exception{
    	if(nodeList.size()==1) {
            // 节点退出时，删除数据表
            deleteTable(me.getID());
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("【系统提示】- 节点 "+me.getID()+" 已经退出DHT网络");
			System.out.println("【系统提示】- 网络已关闭");
    	}
    	else if(nodeList.size()==2) {
    	    // 数据迁移到后继节点
            transferAll(me.getID(), finger[1].getSuccessor().getID());
            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("quitOfTwoNodes/");
    		makeConnectionByObject(finger[1].getSuccessor().getIP(), finger[1].getSuccessor().getPort(), request);
            // 删除数据表
            deleteTable(me.getID());
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("【系统提示】- 节点 "+me.getID()+" 已经退出DHT网络");
    	}
    	else {
            // 数据迁移到后继节点
            transferAll(me.getID(), finger[1].getSuccessor().getID());
            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("quitOfManyNodes/"+pred.getID()+"/"+pred.getIP()+"/"+pred.getPort());
    		makeConnectionByObject(finger[1].getSuccessor().getIP(), finger[1].getSuccessor().getPort(), request);
            // 删除数据表
            deleteTable(me.getID());
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("【系统提示】- 节点 "+me.getID()+" 已经退出DHT网络");
    	}
    }



    // 初始化路由表
    public static void init_finger_table(Node n) throws Exception {
        int myID, nextID;

        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("findSuc/" + finger[1].getStart());
        Message result = makeConnectionByObject(n.getIP(),n.getPort(),request);

        String[] tokens = result.getInitInfo().split("/");
        finger[1].setSuccessor(new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]));
        
        request.setInitInfo("getPred");
        Message result2 = makeConnectionByObject(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request);
        String[] tokens2 = result2.getInitInfo().split("/");
        pred = new Node(Integer.parseInt(tokens2[0]),tokens2[1],tokens2[2]);

        request.setInitInfo("setPred/" + me.getID() + "/" + me.getIP() + "/" + me.getPort());
        makeConnectionByObject(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request);

        int normalInterval = 1;
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
                Message result4 = makeConnectionByObject(n.getIP(),n.getPort(),request);
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
    // 更新影响到的节点的路由表（波及到的节点范围：向前1～2^m-1）
    public static void update_others() throws Exception{
        Node p;
        for (int i = 1; i <= m; i++) {
            int id = me.getID() - (int)Math.pow(2,i-1) + 1;
            if (id < 0)
                id = id + numDHT; 

            p = find_predecessor(id);

            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("updateFing/" + me.getID() + "/" + me.getIP() + "/" + me.getPort() + "/" + i);
            makeConnectionByObject(p.getIP(),p.getPort(),request);
        }
    }
    // 更新路由表
    public static void update_finger_table(Node s, int i) throws Exception // RemoteException,
           {
               Node p;
               int normalInterval = 1;
               int myID = me.getID();
               int nextID = finger[i].getSuccessor().getID();
               if (myID >= nextID) 
                   normalInterval = 0;
               else normalInterval = 1;

               if ( ((normalInterval==1 && (s.getID() >= myID && s.getID() < nextID)) ||
                           (normalInterval==0 && (s.getID() >= myID || s.getID() < nextID)))
                       && (me.getID() != s.getID() ) ) {

                   finger[i].setSuccessor(s);
                   p = pred;

                   Message request = new Message();
                   request.setInitNode_flag(1);
                   request.setInitInfo("updateFing/" + s.getID() + "/" + s.getIP() + "/" + s.getPort() + "/" + i);
                   makeConnectionByObject(p.getIP(),p.getPort(),request);
               }
    }

    public static void quit_update_finger_table(Node s, int exitID){
        for(int i = 1; i <= m; i++){
            if (finger[i].getSuccessor().getID() == exitID){
                finger[i].setSuccessor(s);
            }
        }
    }

    // 设置当前节点的前继节点
    public static void setPredecessor(Node n) // throws RemoteException
    {
        pred = n;
    }
    // 获取当前节点的前继节点
    public static Node getPredecessor() //throws RemoteException 
    {
        return pred;
    }
    // 通过当前节点的路由表查询某个NID的后继节点
    public static Node find_successor(int id) throws Exception {
        Node n = find_predecessor(id);

        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("getSuc/");
        Message result = makeConnectionByObject(n.getIP(),n.getPort(),request);
        String[] tokens = result.getInitInfo().split("/");
        return new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
    }
    // 通过当前节点的路由表查询某个NID的前继节点
    public static Node find_predecessor(int id)  throws Exception
    {
        Node n = me;
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
            Message result = makeConnectionByObject(n.getIP(),n.getPort(),request);
            String[] tokens = result.getInitInfo().split("/");

            n = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);

            myID = n.getID();

            request.setInitInfo("getSuc/");
            Message result2 = makeConnectionByObject(n.getIP(),n.getPort(),request);
            String[] tokens2 = result2.getInitInfo().split("/");

            succID = Integer.parseInt(tokens2[0]);

            if (myID >= succID)
                normalInterval = 0;
            else
                normalInterval = 1;
        }
        return n;
    }
    // 获取当前节点的后继节点
    public static Node getSuccessor() 
    {
        return finger[1].getSuccessor();
    }
    // 获取当前节点路由表中距离目标id最近的节点
    public static Node closet_preceding_finger(int id) 
    {
        int normalInterval = 1;
        int myID = me.getID();
        if (myID >= id) {
            normalInterval = 0;
        }

        for (int i = m; i >= 1; i--) {
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
    // 更新nodeList
    public synchronized static void updateList(Node node) throws Exception {
    	add(node);
    	System.out.println();
    	System.out.println("【系统提示】- "+"新节点 "+node.getID()+"加入DHT网络");
    	printNodeInfo();
    	printNum();
    }
    // 更新其它节点的nodeList
    public synchronized static void updateOthersList() throws Exception {
        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("updateList/"+me.getID()+"/"+me.getIP()+"/"+me.getPort());
        Iterator<Node> iterator = nodeList.iterator();
    	while(iterator.hasNext()) {
    		Node node = iterator.next();
            if (node == me) {
                continue;
            }
    	    makeConnectionByObject(node.getIP(),node.getPort(),request);
    	}
    }
    // 创建节点列表
    public synchronized static void buildNodeList() throws Exception{
    	add(me);
    	Message request = new Message();
    	request.setInitNode_flag(1);
    	request.setInitInfo("load/");
    	Message result = makeConnectionByObject(knownHostIP, knownHostPort, request);
    	getNode(result.getInitInfo());
    }

    // 处理返回的m个node信息并生成list(路由表中最多只有m个node)
    public synchronized static void getNode(String str) {
    	 String[] tokens = str.split("/");
    	 Node newNode;
        for (int i = 1; i <= (tokens.length / 3); i++) {
            newNode = new Node(Integer.parseInt(tokens[3 * (i - 1)]), tokens[1 + 3 * (i - 1)], tokens[2 + 3 * (i - 1)]);
            add(newNode);
        }
    }

    public synchronized static String loadNode(){
	     Node node;
	     String results="";
         for (int i = 0; i < nodeList.size() - 1; i++) {
             node = nodeList.get(i);
             results = results + node.getID() + "/" + node.getIP() + "/" + node.getPort() + "/";
         }
	     results = results + nodeList.get(nodeList.size() - 1).getID() + "/" + nodeList.get(nodeList.size() - 1).getIP() + "/" + nodeList.get(nodeList.size() - 1).getPort() + "/";
	     return results;
    }
    // 广播消息
    public synchronized static void noticeOthers(String message) throws Exception{
        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo(message);
    	Iterator<Node> iterator = nodeList.iterator();
    	while(iterator.hasNext()) {
    		Node node =iterator.next();
    		if(node==me)
    			continue;
    		makeConnectionByObject(node.getIP(),node.getPort(),request);
    	}
    	System.out.println("已通知所有节点");
    }
    // 打印节点个数
    public static void printNum(){
    	System.out.println("当前节点个数 ："+nodeList.size()+"个");
    	System.out.println();
    }
    // 打印前驱节点
    public static void printPred() {
    	System.out.println("本节点的前继节点是 ："+pred.getID());
    	System.out.println();
    }
    // 打印后继节点
    public static void printSuccessor() {
    	System.out.println("本节点的后继节点是 ："+finger[1].getSuccessor().getID());
    	System.out.println();
    }
    // 打印路由表信息



}