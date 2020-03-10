package bupt.fnl.dht.runnable;


import bupt.fnl.dht.service.NodeService;
import bupt.fnl.dht.service.Print;

import java.util.Scanner;

/**
 * 用于监听后台键盘输入并响应
 * exit：节点退出
 * printNodeList：打印节点列表
 * printFinger：打印路由表
 */
public class CommandThread implements Runnable {

    NodeService nodeService;
    Print print;
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
                    print.printFingerInfo();
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
