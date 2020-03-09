package bupt.fnl.dht.node.print;

import bupt.fnl.dht.node.FingerTable;
import bupt.fnl.dht.node.Node;

import java.util.Iterator;
import java.util.List;

/**
 * 打印各种信息
 */
public class Print {

    private Node me;                // 当前节点
    private List<Node> nodeList;    // 节点列表
    private FingerTable[] finger;   // 路由表
    private int m;                  // 路由表条数
    private int numDHT;             // 网络最大节点数

    // 打印路由表信息
    public void printFingerInfo(){
        System.out.println("*****路由表信息*****");
        for (int i = 1; i <= m; i++) {
            System.out.println("Index[" + finger[i].getStart() + "]       " + "后继节点ID: " + finger[i].getSuccessor().getID());
        }
        System.out.println();
    }

    // 打印节点信息
    public void printNodeInfo(){
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
}
