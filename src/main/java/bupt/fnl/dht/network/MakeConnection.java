package bupt.fnl.dht.network;

import bupt.fnl.dht.domain.FingerTable;
import bupt.fnl.dht.domain.Node;
import bupt.fnl.dht.domain.Message;
import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.service.FingerService;
import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 基于 TCP 的 Socket 通信
 */
public class MakeConnection {

    NodeInfo nodeInfo;
    NodeService nodeService;
    FingerService fingerService;

    // 节点间通过序列化对象Object来传输数据
    public Message makeConnectionByObject(String ip, String port, Message message) {
        if (nodeInfo.getMyIP().equals(ip) && nodeInfo.getMyPort().equals(port)) {
            String response = considerInput(message.getInitInfo());
            message.setInitInfo(response);
            return message;
        } else {
            try(
                    Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
                    // 【注意】对于Object IO流，要先创建输出流对象，再创建输入流对象，不然程序会死锁
                    ObjectOutputStream outToOtherNodes = new ObjectOutputStream(sendingSocket.getOutputStream());
                    ObjectInputStream inFromOtherNodes = new ObjectInputStream(sendingSocket.getInputStream()))
            {
                outToOtherNodes.writeObject(message);

                return (Message)inFromOtherNodes.readObject();

            } catch (IOException | ClassNotFoundException e){
                System.out.println("Socket建立连接失败！");
                return null;
            }
        }
    }

    // 根据不同的输入信息执行相应的方法
    public String considerInput(String received) {
        String[] tokens = received.split("/");
        String outResponse = "";

        Node pred = nodeInfo.getPred();
        Node me = nodeInfo.getMe();

        switch (tokens[0]) {
            case "setPred": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                nodeService.setPredecessor(newNode);
                outResponse = "set it successfully";
                break;
            }
            case "getPred": {
                Node newNode = nodeService.getPredecessor();
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "findSuc": {
                Node newNode = nodeService.find_successor(Integer.parseInt(tokens[1]));
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "getSuc": {
                Node newNode = nodeService.getSuccessor();
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            case "findPred": {
                int id = Integer.parseInt(tokens[1]);
                Node newNode = nodeService.find_predecessor(id);
                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
                break;
            }
            // 只有两个节点的退出
            case "quitOfTwoNodes":
                // 后继节点的列表中删除前继
                nodeInfo.getNodeList().remove(pred);

                int m = nodeInfo.getM();
                FingerTable[] finger = nodeInfo.getFinger();

                System.out.println("\n" + "【系统提示】- 节点 " + pred.getID() + " 已经退出DHT网络");
                nodeService.setPredecessor(me);
                for (int i = 1; i <= m; i++) {
                    finger[i].setSuccessor(me);
                }
                break;
            // 多于两个节点时的退出
            case "quitOfManyNodes":
                // 后继节点的列表中删除前继
                nodeInfo.getNodeList().remove(pred);
                // 通知剩余节点删除其前继
                nodeService.noticeOthers("deleteNodeOfNodelist/" + pred.getID() + "/" + pred.getIP() + "/" + pred.getPort() + "/" + me.getID() + "/" + me.getIP() + "/" + me.getPort() + "/" + pred.getID());
                System.out.println("\n" + "【系统提示】- 节点 " + pred.getID() + " 已经退出DHT网络");
                // 将前继设为删除节点的前继
                nodeService.setPredecessor(new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]));
                break;
            case "deleteNodeOfNodelist":
                Node deleteNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                Node updateNode = new Node(Integer.parseInt(tokens[4]), tokens[5], tokens[6]);
                nodeInfo.getNodeList().remove(deleteNode);
                fingerService.quit_update_finger_table(updateNode, Integer.parseInt(tokens[7]));
                System.out.println("\n" + "【系统提示】- 节点 " + tokens[7] + " 已经退出DHT网络");
                break;

            case "load":
                outResponse = nodeService.loadNode();
                break;
            case "updateList": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                nodeService.updateList(newNode);
                break;
            }
//            case "closetPred": {
//                Node newNode = closet_preceding_finger(Integer.parseInt(tokens[1]));
//                outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort();
//                break;
//            }
            case "updateFing": {
                Node newNode = new Node(Integer.parseInt(tokens[1]), tokens[2], tokens[3]);
                fingerService.update_finger_table(newNode, Integer.parseInt(tokens[4]));
                outResponse = "update finger " + Integer.parseInt(tokens[4]) + " successfully";
                break;
            }
        }
        return outResponse;
    }
}
