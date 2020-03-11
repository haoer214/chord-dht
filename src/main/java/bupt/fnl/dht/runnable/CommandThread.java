package bupt.fnl.dht.runnable;

import bupt.fnl.dht.config.Config;
import bupt.fnl.dht.domain.NodeInfo;
import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * CommandThread - 用于监听后台键盘输入并响应
 * exit() 节点退出
 * printNodeList() 打印节点列表
 * printFinger() 打印路由表
 */
public class CommandThread implements Runnable {

    /* --- Spring不能在线程类中注入，可通过getBean获取 --- */

    NodeService nodeService;
    Print print;

    public CommandThread(NodeService nodeService, Print print) {
        this.nodeService = nodeService;
        this.print = print;
    }

    @Override
    public void run() {
        Scanner scan = new Scanner(System.in);
        label:
        while (scan.hasNext()) {
            String str1 = scan.nextLine();
            switch (str1) {
                case "exit":  // 节点退出
                    try {
                        nodeService.beforeExit();
                    } catch (Exception e) {
                        System.out.println("节点退出异常！");
                        e.printStackTrace();
                        System.exit(1);
                    }
                    break label;
                case "printNodeList":
                    try {
                        print.printNodeInfo();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "printFinger":
                    try {
                        print.printFingerInfo();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
}
