package bupt.fnl.dht.service.impl;

import bupt.fnl.dht.domain.FingerTable;
import bupt.fnl.dht.domain.Node;
import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.service.Print;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

@Service("print")
public class PrintImpl implements Print {

    @Autowired
    NodeInfo nodeInfo;

    // 打印路由表信息
    public void printFingerInfo(){
        FingerTable[] finger = nodeInfo.getFinger();
        int m = nodeInfo.getM();
        System.out.println("*****路由表信息*****");
        for (int i = 1; i <= m; i++) {
            System.out.println("Index[" + finger[i].getStart() + "]       " + "后继节点ID: " + finger[i].getSuccessor().getID());
        }
        System.out.println();
    }

    // 打印节点信息
    public void printNodeInfo(){
        List<Node> nodeList = nodeInfo.getNodeList();
        if (nodeList == null || nodeList.size() == 0) {
            System.out.println("列表为空！");
            return;
        }
        Iterator<Node> iterator = nodeList.iterator();
        System.out.println("*****节点列表*****");
        while(iterator.hasNext()) {
            Node node = iterator.next();
            String string = "节点ID: " + node.getID() + " IP地址: " + node.getIP() + " 端口号: " + node.getPort();
            System.out.println(string);
        }
        System.out.println();
    }
}
