import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

/* 
  新节点加入时需要知道一个已经在网络中的节点的IP和监听端口
*/
public class NodeDHT implements Runnable {

    private int ID;
    private Socket connection;
    private static ServerSocket serverSocket = null; 
    private static Node me, pred;
    private static int m;
    private static int numDHT;
    private static int busy;
    private static Object object = new Object();
    private static FingerTable[] finger;    
    private static String knownhostIP;
    private static String knownhostport;
    private static String myIP;
    private static String myport;
    private static List<Node> nodeList = new LinkedList<>();
    // v2增：连接mysql所需要的成员变量
    private static String driver;
    private static String url;
    private static String user;
    private static String pass;
    private static Connection conn;

    public NodeDHT(Socket s, int i) {
        this.connection = s;
        this.ID = i;
    }
    
    public static void main(String args[]) throws Exception
    {
        //第一个节点和之后其他节点的加入，通过参数的个数区分（修改）
        //args参数为2位:[当前节点监听端口] [numNodes]=>说明是第一个加入的节点 args[0] args[1]
        //args参数为4位:[已知节点IP] [已知节点监听端口] [当前节点监听端口] [numNodes]=>说明不是第一个加入的节点 args[0]  args[1]  args[2]  args[3]
        if (args.length==2){
        	System.out.println(" *********************************启动DHT网络*************************************");
        	myport=args[0];

            int maxNumNodes = Integer.parseInt(args[1]);
            m = (int) Math.ceil(Math.log(maxNumNodes) / Math.log(2));
            finger = new FingerTable[m+1];
            numDHT = (int)Math.pow(2,m);
         
            InetAddress mIP = InetAddress.getLocalHost();
            myIP = mIP.getHostAddress();
            System.out.println("本节点IP地址: " + myIP );

            int initInfo = getFirstNodeInfo(myIP,myport);
            me = new Node(initInfo,myIP,myport);
            pred = me;
            System.out.println("节点ID:" + me.getID() + "   前继节点ID:" + pred.getID());

            // '第一个节点加入'的线程
            Runnable runnable = new NodeDHT(null,0);
            Thread thread = new Thread(runnable);
            thread.start();
            
            // v2改：监听键盘输入的线程
            Runnable inputRunnable = new NodeDHT(null,-2);
            Thread inputThread = new Thread(inputRunnable);
            inputThread.start();
            
            int count = 1;
            int port = Integer.parseInt(args[0]);
            try {
                   serverSocket = new ServerSocket( port );
            } catch (IOException e) {
                   System.out.println("[系统提示]:"+"无法监听端口 - " + port);
                   System.exit(-1);
            }
            
            while (true) {
                   Socket newCon = serverSocket.accept();
                   Runnable runnable2 = new NodeDHT(newCon,count++);
                   Thread t = new Thread(runnable2);
                   t.start();
            }
        }     
        else if(args.length==4){
        	System.out.println(" *********************************启动DHT网络*************************************");
        	knownhostIP=args[0];
        	knownhostport=args[1];
        	myport=args[2];
        	int maxNumNodes = Integer.parseInt(args[3]);
            m = (int) Math.ceil(Math.log(maxNumNodes) / Math.log(2));
            finger = new FingerTable[m+1];
            numDHT = (int)Math.pow(2,m);
             
            InetAddress mIP = InetAddress.getLocalHost();
            myIP=mIP.getHostAddress(); 
            System.out.println("本节点IP地址: " + myIP);

            int initInfo = getNodeInfo(myIP,myport);
            me = new Node(initInfo,myIP,myport);
            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("findPred/"+initInfo);
            Message result = makeConnectionByObject(knownhostIP, knownhostport, request);
            String[] tokens = result.getInitInfo().split("/");
            pred = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
            System.out.println("本节点 ID:"+me.getID() + "   前继节点 ID:" +pred.getID());

            // '非第一个节点加入'的线程
            Runnable runnable = new NodeDHT(null,-1);
            Thread thread = new Thread(runnable);
            thread.start();
            
            // v2改：监听键盘输入的线程
            Runnable inputRunnable = new NodeDHT(null,-2);
            Thread inputThread = new Thread(inputRunnable);
            inputThread.start();
            
            int count = 1;
            int port = Integer.parseInt(myport);
            try {
                   serverSocket = new ServerSocket( port );
                } catch (IOException e) {
                   System.out.println("无法监听端口 - " + port);
                   System.exit(-1);
                }
            
            while (true) {
                   Socket newCon = serverSocket.accept();
                   Runnable runnable2 = new NodeDHT(newCon,count++);
                   Thread t = new Thread(runnable2);
                   t.start();
            }
        }
        else
        {
            System.out.println("Syntax one - NodeDHT-First [LocalPortnumber] [numNodes]");
            System.out.println("Syntax two - NodeDHT-other [Known-HostIP]  [Known-HostPortnumber] [LocalPortnumber] [numNodes]");
            System.out.println("         *** [LocaPortNumber] = is the port number which the Node will be listening waiting for connections.");
            System.out.println("         *** [Known-HostName] = is the hostIP of one DHTNode already in the net.");
            System.out.println("         *** [Known-HostPortnumber] = is the port which the Known-Host listening waiting for connections.");
            System.exit(1);
        }	
      
    }

    public static void finishJoiningFirst(int id) throws Exception{
    	System.out.println();
        System.out.println("[系统提示]:"+"节点 " +id + "已经在DHT网络中！.");
        printNodeInfo();
        //printNum();
        synchronized (object) {
            busy = 0;
        }
    }
    
    public static void finishJoining(int id) throws Exception{
    	System.out.println();
        System.out.println("[系统提示]:"+"节点 " +id + "已经在DHT网络中！.");
        printNodeInfo();
        //printNum();
        synchronized (object) {
            busy = 0;
        }
    }

    //新增：获取第一个节点的信息
    public static int getFirstNodeInfo(String nodeIP, String nodePort) throws Exception{
        if (busy == 0) { 
            synchronized (object) {
                 busy = 1; 
            }
            int nodeID;
            int initInfo = 0;
            try{ 
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.reset();
                String hashString = nodeIP+ nodePort;
                md.update(hashString.getBytes());
                byte[] hashBytes = md.digest();
                BigInteger hashNum = new BigInteger(1,hashBytes);
                
                nodeID = Math.abs(hashNum.intValue()) % numDHT;
                
                initInfo = nodeID;
            } catch (NoSuchAlgorithmException nsae){
                nsae.printStackTrace();
            }
            
            return initInfo; 
        }
        else {
            return -1; 
        } 
    } 

    //改动：修改了确定冲突的方式
    /*
     * 可否将新节点IP、端口直接发送给已知节点进行hash计算，并比较判断是否冲突
     * 这样的话，即便产生冲突，只需要已知节点重新计算即可，新节点仅需makeConnection一次
     */
    public static int getNodeInfo(String nodeIP, String nodePort) throws Exception{
        if (busy == 0) {
            synchronized (object) {
                busy = 1;
            }
            int nodeID;
            int initInfo =0;
    
            try{ 
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.reset();
                String hashString = nodeIP+ nodePort;
                md.update(hashString.getBytes());
                byte[] hashBytes = md.digest();
                BigInteger hashNum = new BigInteger(1,hashBytes);
    
                nodeID = Math.abs(hashNum.intValue()) % numDHT;

                Message request = new Message();
                request.setInitNode_flag(1);
                request.setInitInfo("findSucOfPred/"+nodeID);
                while(Integer.parseInt(makeConnectionByObject(knownhostIP, knownhostport, request).getInitInfo())==nodeID) {
                    md.reset();
                    md.update(hashBytes);
                    hashBytes = md.digest();
                    hashNum = new BigInteger(1,hashBytes);
                    nodeID = Math.abs(hashNum.intValue()) % numDHT;
                    request.setInitInfo("findSucOfPred/"+nodeID);
                }

                System.out.println("新节点加入... ");
    
                initInfo = nodeID;

            } catch (NoSuchAlgorithmException nsae){
                nsae.printStackTrace();
            }

            return initInfo;
        }
        else {
            return -1; 
        } 
    } 
    
    public static int HashFunc(String url) {
    	int KID=-1;
    	try {
    		MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(url.getBytes());
            byte[] hashBytes = md.digest();
            BigInteger hashNum = new BigInteger(1,hashBytes);
            
            KID = Math.abs(hashNum.intValue()) % numDHT;
    	}catch (Exception e) {
            e.printStackTrace();
        }
           
        return KID;
    }

    // v2增：初始化jdbc连接信息，加载mysql驱动，建立Connection
    public static void initParam(String paramFile) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(paramFile)));
        System.out.println("配置文件加载成功！");
        // 通过.ini配置文件进行初始化
        driver = props.getProperty("driver");
        url = props.getProperty("url");
        user = props.getProperty("user");
        pass = props.getProperty("pass");
        System.out.println("配置信息初始化成功！");
//        System.out.println("driver = " + driver);
//        System.out.println("url = " + url);
//        System.out.println("user = " + user);
//        System.out.println("pass = " + pass);
        // 加载驱动
        Class.forName(driver);
        System.out.println("驱动加载成功！");
        // 建立连接
        conn = DriverManager.getConnection(url,user,pass);
        System.out.println("数据库连接建立成功！");

    }
    // v2增：节点加入时，创建数据表node[i]（i表示节点ID）
    public static void createTable(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(sql);
        }
    }
    // v2增：节点加入时，部分数据从后继迁移到新节点
    public static void transferPart(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(sql);
        }
    }
    // v2增：节点退出时，数据迁移到后继节点
    public static void transferAll(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(sql);
        }
    }
    // v2增：节点退出时，删除数据表node[i]（i表示节点ID）
    public static void deleteTable(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(sql);
        }
    }
    // v2增：判断标识是否已被注册
    public static boolean ifExist(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
        {
            return rs.next();
        }
    }
    // v2增：添加数据 [Hash, Identity, Content]
    public static int registerData(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement())
        {
            return stmt.executeUpdate(sql);
        }
    }
    // v2增：通过标识解析数据 [Hash, Identity, Content]
    public static String resolveData(String sql) throws Exception {
        try(
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
        {
            String result = "";
            if (rs.next()) {
//                result  = "<解析结果>: " + "#"
//                        + "【哈希】 " + rs.getInt(1) + "#"
//                        + "【标识】 " + rs.getString(2) + "#"
//                        + "【内容】 " + rs.getString(3) + "#";
                result = rs.getString(3);
            }
//            else {
//                result = "解析失败! 请确认标识是否输入正确 " + "#";
//            }
            return result;
        }
    }

    @Override
    public void run() {
        if (this.ID == 0) { //ID==0时，第一个加入的节点的入口

            // v2增：初始化数据库连接信息
            System.out.println("正在连接数据库 ... ");
            try {
                initParam("./mysql.ini");
            } catch (Exception e) {
                System.out.println("数据库连接失败！");
            }
            // v2增：创建数据表
            System.out.println("正在创建数据表 ... ");
            try {
                createTable("create table node" + me.getID()
                        + "(Hash int, "
                        + "Identity varchar(255) primary key, "
                        + "Content text);");
                System.out.println("成功创建数据表【node" + me.getID() + "】");
            } catch (Exception e) {
                System.out.println("数据表创建失败！");
            }

            System.out.println("正在创建路由表 ... ");
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
            System.out.println("路由表创建完成，此节点是网络中唯一节点！");
            printFingerInfo();
            //System.out.println();
           
            try {
            	System.out.println("开始创建节点列表...");
            	add(me);
				System.out.println("节点列表创建完成");
			} catch (Exception e) {
                e.printStackTrace();
            }
            
            try { 
                finishJoiningFirst(me.getID());//释放对象锁
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if (this.ID == -1) {//ID==-1时，非第一个加入的节点的入口

            // v2增：初始化数据库连接信息
            System.out.println("正在连接数据库 ... ");
            try {
                initParam("./mysql.ini");
            } catch (Exception e) {
                System.out.println("数据库连接失败！");
            }
            // v2增：创建数据表
            System.out.println("正在创建数据表 ... ");
            try {
                createTable("create table node" + me.getID()
                        + "(Hash int, "
                        + "Identity varchar(255) primary key, "
                        + "Content text);");
                System.out.println("成功创建数据表【node" + me.getID() + "】");
            } catch (Exception e) {
                System.out.println("数据表创建失败！");
            }

            System.out.println("正在创建路由表 ...");
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
            System.out.println("空表创建完成....");
            //System.out.println();
            try{    
            	    System.out.println("开始初始化路由表.....");
                    init_finger_table(pred);
                    System.out.println("路由表初始化完成.....");
                    printFingerInfo();
                    update_others();
                    System.out.println("其它节点路由表已更新");
                    System.out.println();
                    System.out.println("开始构建节点列表...");
                    buildNodeList();
                    System.out.println("节点列表创建完成");
                    updateOthersList();
                    System.out.println("其它节点的列表已更新");
            } catch (Exception e) {
            	e.printStackTrace();
            }

            // v2增：将部分数据从后继迁移到当前节点
            if (me.getID() > finger[1].getSuccessor().getID()) { // 新加入节点ID大于后继
                try {
                    transferPart("insert into node" + me.getID()
                            + " select * from node" + finger[1].getSuccessor().getID()
                            + " where Hash<=" + me.getID() + "&&Hash>" + finger[1].getSuccessor().getID() + ";");
                    transferPart("delete from node" + finger[1].getSuccessor().getID()
                            + " where Hash<=" + me.getID() + "&&Hash>" + finger[1].getSuccessor().getID() + ";");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else { // 新加入节点ID小于后继
                try {
                    transferPart("insert into node" + me.getID()
                            + " select * from node" + finger[1].getSuccessor().getID()
                            + " where Hash<=" + me.getID() + "||Hash>" + finger[1].getSuccessor().getID() + ";");
                    transferPart("delete from node" + finger[1].getSuccessor().getID()
                            + " where Hash<=" + me.getID() + "||Hash>" + finger[1].getSuccessor().getID() + ";");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try { 
                finishJoining(me.getID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if (this.ID == -2) { // 指令集
            Scanner scan = new Scanner(System.in);
            while (scan.hasNext()) {
                String str1 = scan.nextLine();
                if(str1.equals("exit")){ // 节点退出
                    try {
						beforeExit();	   
					} catch (Exception e) {
						System.out.println("节点退出异常！");
					}
                    break;
                }
                else if(str1.equals("printnodelist")) {
                	try {
						printNodeInfo();
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
                else if(str1.equals("printfinger")) {
                	printFingerInfo();
                }
                else if(str1.equals("printnum")) {
                	printNum();
                }
                else if(str1.equals("printpred")) {
                	printPred();
                }
                else if(str1.equals("printsuc")) {
                	printSuccessor();
                }
                else {
                	System.out.print("命令格式不正确！请重新输入");
                }
            }
            scan.close();
            System.exit(0);
        }
        else {
            // ID等于其他值时，进行的是信息交互的部分
            /* 对象序列化 */
            try (
                    // 【注意】对于Object IO流，要先创建输出流对象，再创建输入流对象，不然程序会死锁
                    ObjectOutputStream outToControllerOrOtherNodes = new ObjectOutputStream(connection.getOutputStream());
                    ObjectInputStream inFromControllerOrOtherNodes = new ObjectInputStream(connection.getInputStream()))
            {
                System.out.println("尝试读取对象...");
                Message received_message = (Message)inFromControllerOrOtherNodes.readObject();
                System.out.println(received_message.getInitNode_flag());
                System.out.println(received_message.getInitInfo());
                // 节点初始化配置指令
                if (received_message.getInitNode_flag() == 1) {
                    String response_initInfo = considerInput(received_message.getInitInfo());
                    received_message.setInitInfo(response_initInfo);
                    System.out.println(received_message.getInitInfo());
                    outToControllerOrOtherNodes.writeObject(received_message);

                } else {
                    outToControllerOrOtherNodes.writeObject(considerType(received_message));
                }
            } catch (Exception e) {
                System.out.println("[系统提示]:"+"线程无法服务连接");
            	e.printStackTrace();
            }
        }
    }
    // 对于不同的操作类型，返回不同的 Message 对象
    public static Message considerType(Message message){
        System.out.println(message.getType());
        Message result = message;
        switch (message.getType()) {
            case "getNodeList":
                result.setNodeList(getNodeList());
                result.setFeedback("成功获取所有节点信息！");
                break;
            case "register":
                try {
                    result = registerIdentity(message);
                } catch (Exception e) {
                    System.out.println("添加数据失败！");
                }
                break;
//            case "delete":
//                break;
//            case "modify":
//                break;
            case "resolve":
                try {
                    result = resolveIdentity(message);
                } catch (Exception e) {
                    System.out.println("解析数据失败！");
                }
        }
        return result;
    }

    // 获取所有节点信息
    private static Node[] getNodeList() {
        // 【注意】必须把List转换为数组才能序列化传输
        Node[] nodes = new Node[nodeList.size()];
        nodeList.toArray(nodes);
        return nodes;
    }

    private static Message registerIdentity(Message message) throws Exception {
        int kid = HashFunc(message.getIdentity());//标识的哈希
        Node targetNode = find_successor(kid);//应该存储的位置
        if (targetNode.getID() == me.getID()) {
            // v2增：判断标识是否已被注册
            if (ifExist("select * from node" + me.getID()
                    + " where Identity='" + message.getIdentity() + "';")) {
                System.out.println("[系统提示]: 当前标识已被注册！");
                message.setFeedback("该标识已被注册过！");
            } else {
                // v2增：添加到本地节点数据库列表
                registerData("insert into node" + me.getID() + "(Hash, Identity, Content)"
                        + " values(" + kid + ", '" + message.getIdentity() + "', '" + message.getMappingData() + "');");
                System.out.println("[系统提示]: 标识映射 " + message.getIdentity() + "->" + message.getMappingData() + " 已存入本地数据库");
                message.setFeedback("标识 " + message.getIdentity() + " 注册成功！");
            }
            return message;
        } else {
            Message receive_message = makeConnectionByObject(targetNode.getIP(), targetNode.getPort(), message);
            System.out.println("[系统提示]: 标识映射已存入节点" + targetNode.getID());
            return receive_message;
        }
    }
    private static Message resolveIdentity(Message message) throws Exception {
        Node targetNode = find_successor(HashFunc(message.getIdentity()));
        if (targetNode.getID() == me.getID()) {
            // v2增：从本地数据库获取内容，并将所有"#"替换为换行符
//            System.out.println((resolveData("select *" + " from node" + me.getID() + " where Identity='" + identity + "';"))
//                    .replaceAll("#", "\n"));
            String result = resolveData("select *" + " from node" + me.getID() + " where Identity='" + message.getIdentity() + "';");
            if (!result.equals("")) {
                message.setMappingData(result);
                message.setFeedback("标识解析成功！");
            } else {
                message.setFeedback("该标识不存在！");
            }
            return message;
        } else {
            // v2增：从其他节点数据库获取内容，并将所有"#"替换为换行符
//            System.out.println(makeConnection(targetNode.getIP(), targetNode.getPort(), "geturl/" + identity)
//                    .replaceAll("#", "\n"));
            return makeConnectionByObject(targetNode.getIP(), targetNode.getPort(),message);
        }
    }

    public static Message makeConnectionByObject(String ip, String port, Message message) throws Exception {
        System.out.println(message.getInitNode_flag());
        System.out.println(message.getInitInfo());
        if (myIP.equals(ip) && myport.equals(port)) {
            String response = considerInput(message.getInitInfo());
            message.setInitInfo(response);
            return message;
        } else {
            try(
                    Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
                    // 【注意】对于Object IO流，要先创建输出流对象，再创建输入流对象，不然程序会死锁
                    ObjectOutputStream outToControllerOrOtherNodes = new ObjectOutputStream(sendingSocket.getOutputStream());
                    ObjectInputStream inFromControllerOrOtherNodes = new ObjectInputStream(sendingSocket.getInputStream()))
            {
                outToControllerOrOtherNodes.writeObject(message);

                Message result = (Message)inFromControllerOrOtherNodes.readObject();

                System.out.println(result.getInitInfo());
                return result;
            }
        }
    }
    
    public void beforeExit() throws Exception{
    	if(nodeList.size()==1) {
            // v2增：节点退出时，删除数据表
            deleteTable("drop table node" + me.getID() + ";");
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("[系统提示]: 节点 "+me.getID()+"已经退出DHT网络");
			System.out.println("[系统提示]: 网络已关闭");
    	}
    	else if(nodeList.size()==2) {
    	    // v2增：数据迁移到后继节点
            transferAll("insert into node" + finger[1].getSuccessor().getID()
                    + " select * from node" + me.getID() + ";");
            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("quitOfTwoNodes/");
    		makeConnectionByObject(finger[1].getSuccessor().getIP(), finger[1].getSuccessor().getPort(), request);
            // v2增：删除数据表
            deleteTable("drop table node" + me.getID() + ";");
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("[系统提示]: 节点 "+me.getID()+"已经退出DHT网络");
    	}
    	else {
            // v2增：数据迁移到后继节点
            transferAll("insert into node" + finger[1].getSuccessor().getID()
                    + " select * from node" + me.getID() + ";");
            Message request = new Message();
            request.setInitNode_flag(1);
            request.setInitInfo("quitOfManyNodes/"+pred.getID()+"/"+pred.getIP()+"/"+pred.getPort());
    		makeConnectionByObject(finger[1].getSuccessor().getIP(), finger[1].getSuccessor().getPort(), request);
            // v2增：删除数据表
            deleteTable("drop table node" + me.getID() + ";");
            System.out.println("\n" + "已删除数据表【node" + me.getID() + "】");
            System.out.println("[系统提示]: 节点 "+me.getID()+"已经退出DHT网络");
    	}
    }

    public static String considerInput(String received) throws Exception {
        String[] tokens = received.split("/");
        String outResponse = "";

        if (tokens[0].equals("setPred")) {
            Node newNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
            setPredecessor(newNode);
            outResponse = "set it successfully";	
        }
        else if (tokens[0].equals("getPred")) {
            Node newNode = getPredecessor();
            outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
        }
        else if (tokens[0].equals("findSuc")) {
            Node newNode = find_successor(Integer.parseInt(tokens[1]));
            outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
        }
        else if (tokens[0].equals("getSuc")) {
            Node newNode = getSuccessor();
            outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
        }
        //新添加
        else if (tokens[0].equals("findPred")) {
            int id=Integer.parseInt(tokens[1]);
            Node newNode = find_predecessor(id);
            outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
        }
        //新添加
        else if (tokens[0].equals("quitOfTwoNodes")) {//只有两个节点的退出
        	delete(pred);//后继节点的列表中删除前继
        	System.out.println("\n"+"[系统提示]: 节点 "+pred.getID()+"已经退出DHT网络");
        	setPredecessor(me);
        	for(int i=1;i<=m;i++) {
        		finger[i].setSuccessor(me);
        	}       	
        	printNum();
        }
        //新添加
        else if (tokens[0].equals("quitOfManyNodes")) {//多于两个节点时的退出
        	delete(pred);//后继节点的列表中删除前继
        	noticeOthers("deleteNodeOfNodelist/"+pred.getID()+"/"+pred.getIP()+"/"+pred.getPort()+"/"+me.getID()+"/"+me.getIP()+"/"+me.getPort()+"/"+pred.getID());//通知剩余节点删除其前继
        	System.out.println("\n"+"[系统提示]: 节点 "+pred.getID()+"已经退出DHT网络");
        	setPredecessor(new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]));//将前继设为删除节点的前继
        	printNum();
        }
        //新添加
        else if (tokens[0].equals("deleteNodeOfNodelist")) {
        	Node deleteNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
        	Node updateNode = new Node(Integer.parseInt(tokens[4]),tokens[5],tokens[6]);
        	delete(deleteNode);
        	quit_update_finger_table(updateNode,Integer.parseInt(tokens[7]));
        	System.out.println("\n"+"[系统提示]: 节点 "+tokens[7]+"已经退出DHT网络");
            printNum();
        }
        //新添加
        else if (tokens[0].equals("printNum")) {
        	printNum();
        }
        //新添加
        else if (tokens[0].equals("printNodeInfo")) {
        	printNodeInfo();
        }
        //新添加
        else if (tokens[0].equals("findSucOfPred")) {
            outResponse = Integer.toString(find_successor(find_predecessor(Integer.parseInt(tokens[1])).getID()).getID());
        }
        //新添加
        else if (tokens[0].equals("load")) {
            outResponse = loadNode();
        }
        //新添加
        else if (tokens[0].equals("updateList")) {
        	Node newNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
        	updateList(newNode);
        }
        else if (tokens[0].equals("closetPred")) {
            Node newNode = closet_preceding_finger(Integer.parseInt(tokens[1]));
            outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
        }
        else if (tokens[0].equals("updateFing")) {
            Node newNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
            update_finger_table(newNode,Integer.parseInt(tokens[4]));
            outResponse = "update finger " + Integer.parseInt(tokens[4]) + " successfully";	
        }
        return outResponse;
    }

    //初始化路由表
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
    //更新影响到的节点的路由表（波及到的节点范围：向前1～2^m-1）
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
    //更新路由表
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

    //设置当前节点的前继节点
    public static void setPredecessor(Node n) // throws RemoteException
    {
        pred = n;
    }
    //获取当前节点的前继节点
    public static Node getPredecessor() //throws RemoteException 
    {
        return pred;
    }
    //通过当前节点的路由表查询某个NID的后继节点
    public static Node find_successor(int id) throws Exception {
        Node n = find_predecessor(id);

        Message request = new Message();
        request.setInitNode_flag(1);
        request.setInitInfo("getSuc/");
        Message result = makeConnectionByObject(n.getIP(),n.getPort(),request);
        String[] tokens = result.getInitInfo().split("/");
        return new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
    }
    //通过当前节点的路由表查询某个NID的前继节点
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
    //获取当前节点的后继节点
    public static Node getSuccessor() 
    {
        return finger[1].getSuccessor();
    }
    //获取当前节点路由表中距离目标id最近的节点
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
    //新增：更新nodeList
    public synchronized static void updateList(Node node) throws Exception {
    	add(node);
    	System.out.println();
    	System.out.println("[系统提示]： "+"新节点 "+node.getID()+"加入DHT网络");
    	printNodeInfo();
    	printNum();
    }
    //新增：更新其它节点的nodeList
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
    public synchronized static void buildNodeList() throws Exception{
    	add(me);
    	Message request = new Message();
    	request.setInitNode_flag(1);
    	request.setInitInfo("load/");
    	Message result = makeConnectionByObject(knownhostIP, knownhostport, request);
    	getNode(result.getInitInfo());
    }

    //新增：处理返回的m个node信息并生成list(路由表中最多只有m个node)
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
    //新增：广播消息
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
    //新增：打印节点个数
    public static void printNum(){
    	System.out.println("当前节点个数 ："+nodeList.size()+"个");
    	System.out.println();
    }
    //新增：打印前继
    public static void printPred() {
    	System.out.println("本节点的前继节点是 ："+pred.getID());
    	System.out.println();
    }
    //新增：打印后继
    public static void printSuccessor() {
    	System.out.println("本节点的后继节点是 ："+finger[1].getSuccessor().getID());
    	System.out.println();
    }
    //新增：打印路由表信息
    public static void printFingerInfo(){
    	String results="";
    	System.out.println("*****路由表信息*****");
    	for(int i=1;i<=m;i++) {
		      System.out.println(results+"Index["+finger[i].getStart()+"]       "+"后继节点ID: "+finger[i].getSuccessor().getID());
	     }
    }
    //新增：打印节点信息
    public synchronized static void printNodeInfo() throws Exception{
    	Iterator<Node> iterator = nodeList.iterator();
    	String string;
    	System.out.println("*****节点列表*****");
    	if(nodeList.size()==0)
    		System.out.println("列表为空！");
    	while(iterator.hasNext()) {
    		Node node = iterator.next();
    		string="节点ID:"+node.getID()+"  IP地址："+node.getIP()+"  端口号： "+node.getPort()+" ";
    		System.out.println(string);
    	}
    	System.out.println();
    }
    //新增：删除节点
    public synchronized static void delete(Node node){
    	nodeList.remove(node);
    }
    //新增：增加节点
    public synchronized static void add(Node node){
    	nodeList.add(node);
    }
}