package bupt.fnl.dht.service;

/**
 * 用于打印各种信息
 */
public interface Print {
    /**
     * 打印路由表信息
     */
    void printFingerInfo();

    /**
     * 打印节点信息
     */
    void printNodeInfo();
}
