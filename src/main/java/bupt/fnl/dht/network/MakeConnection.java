package bupt.fnl.dht.network;

import bupt.fnl.dht.node.Node;
import bupt.fnl.dht.utils.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 基于 TCP 的 Socket 通信
 */
public class MakeConnection {

    private String myIP;
    private String myPort;

    // 节点间通过序列化对象Object来传输数据
    public Message makeConnectionByObject(String ip, String port, Message message) throws Exception {
        if (myIP.equals(ip) && myPort.equals(port)) {
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

                return (Message)inFromControllerOrOtherNodes.readObject();
            }
        }
    }

    // 根据不同的输入信息执行相应的方法
    public static String considerInput(String received) throws Exception {
        String[] tokens = received.split("/");
        String outResponse = "";

        switch (tokens[0]) {
            case "setPred": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                setPredecessor(newNode);
                outResponse = "set it successfully";
                break;
            }
            case "getPred": {
                Node newNode = getPredecessor();
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "findSuc": {
                Node newNode = find_successor(Integer.parseInt(tokens[1]));
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "getSuc": {
                Node newNode = getSuccessor();
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "findPred": {
                int id = Integer.parseInt(tokens[1]);
                Node newNode = find_predecessor(id);
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            // 只有两个节点的退出
            case "quitOfTwoNodes":
                // 后继节点的列表中删除前继
                delete(pred);

                System.out.println("\n" + "【系统提示】- 节点 " + pred.getID() + " 已经退出DHT网络");
                setPredecessor(me);
                for (int i = 1; i <= m; i++) {
                    finger[i].setSuccessor(me);
                }
                printNum();
                break;
            // 多于两个节点时的退出
            case "quitOfManyNodes":
                // 后继节点的列表中删除前继
                delete(pred);
                // 通知剩余节点删除其前继
                noticeOthers("deleteNodeOfNodelist/" + pred.getID() + "/" + pred.getIP() + "/" + pred.getPort() + "/" + me.getID() + "/" + me.getIP() + "/" + me.getPort() + "/" + pred.getID());
                System.out.println("\n" + "【系统提示】- 节点 " + pred.getID() + " 已经退出DHT网络");
                // 将前继设为删除节点的前继
                setPredecessor(new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]));
                printNum();
                break;
            case "deleteNodeOfNodelist":
                Node deleteNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                Node updateNode = new Node(Integer.parseInt(tokens[4]), tokens[5], tokens[6]);
                delete(deleteNode);
                quit_update_finger_table(updateNode, Integer.parseInt(tokens[7]));
                System.out.println("\n" + "【系统提示】- 节点 " + tokens[7] + " 已经退出DHT网络");
                printNum();
                break;
            case "printNum":
                printNum();
                break;
            case "printNodeInfo":
                printNodeInfo();
                break;
            case "findSucOfPred":
                outResponse = Integer.toString(find_successor(find_predecessor(Integer.parseInt(tokens[1])).getID()).getID());
                break;
            case "load":
                outResponse = loadNode();
                break;
            case "updateList": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                updateList(newNode);
                break;
            }
            case "closetPred": {
                Node newNode = closet_preceding_finger(Integer.parseInt(tokens[1]));
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "updateFing": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                update_finger_table(newNode, Integer.parseInt(tokens[4]));
                outResponse = "update finger " + Integer.parseInt(tokens[4]) + " successfully";
                break;
            }
        }
        return outResponse;
    }
}
