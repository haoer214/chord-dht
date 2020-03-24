package bupt.fnl.dht.service;

import bupt.fnl.dht.domain.Node;

/**
 * 路由表相关业务功能
 */
public interface FingerService {
    /**
     * initTable - 初始化数据库、路由表
     * @param args
     */
    void initTable(String... args);
    /**
     * 初始化路由表
     */
    void init_finger_table();

    /**
     * 更新路由表
     * @param s
     * @param i
     */
    void update_finger_table(Node s, int i);

    /**
     * 更新影响到节点的路由表（波及到的节点范围：向前1～2^m-1
     */
    void update_others();

    void quit_update_finger_table(Node s, int exitID);


}
