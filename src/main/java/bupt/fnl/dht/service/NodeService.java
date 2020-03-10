package bupt.fnl.dht.service;

import bupt.fnl.dht.domain.Node;

/**
 * dht节点相关业务功能
 */
public interface NodeService {
    /**
     * initNode - 初始化节点信息
     *
     * 通过输入参数的个数区分第一个节点和之后其他节点的加入
     *
     *      args参数为2位: [当前节点监听端口] [numNodes]
     *      说明是第一个加入的节点 args[0] args[1]
     *
     *      args参数为4位: [已知节点IP] [已知节点监听端口] [当前节点监听端口] [numNodes]
     *      说明不是第一个加入的节点 args[0]  args[1]  args[2]  args[3]
     */
    void initNode(String... args);

    /**
     * getCurrentNode - 获取当前节点
     * @param maxNumNodes 用户输入的最大节点数
     * @return 返回当前节点
     * @throws Exception
     */
    Node getCurrentNode(int maxNumNodes);

    /**
     * 获取新加入网络的节点ID
     * @param nodeIP
     * @param nodePort
     * @return
     */
    int getNodeID(String nodeIP, String nodePort);

    /**
     * 处理返回的m个node信息并生成list(路由表中最多只有m个node)
     * @param str
     */
    void getNode(String str);

    /**
     * 创建节点列表nodeList
     */
    void buildNodeList();

    // 更新nodeList
    void updateList(Node node);

    /**
     * 更新其它节点的nodeList
     */
    void updateOthersList();

    /**
     * 通过当前节点的路由表查询某个NID的前继节点
     * @param id
     * @return
     */
    Node find_predecessor(int id);

    /**
     * 通过当前节点的路由表查询某个NID的后继节点
     * @param id
     * @return
     */
    Node find_successor(int id);

    /**
     * 设置当前节点的前继节点
     * @param n
     */
    void setPredecessor(Node n);

    /**获取当前节点的前继节点
     *
     * @return
     */
    Node getPredecessor();

    /**
     * 获取当前节点的后继节点
     * @return
     */
    Node getSuccessor();

    // 获取当前节点路由表中距离目标id最近的节点
    Node closet_preceding_finger(int id);

    // 广播消息
    void noticeOthers(String message);

    String loadNode();

    /**
     * 节点退出网络
     * 分 单个节点、两个节点、多个节点 退出3种情况
     */
    void beforeExit();
}
