package bupt.fnl.dht.dao;

/**
 * 与MySQL数据库进行交互
 */
public interface DataBase {

    /**
     * 初始化，建立数据库连接
     */
    void initParam();

    // 节点加入时，创建数据表node[i]（i表示节点ID）
    void createTable(int nodeID);

    // 节点加入时，部分数据从后继迁移到新节点
    void transferPart(int newNode, int sucNode);

    // 节点退出时，数据迁移到后继节点
    void transferAll(int newNode, int sucNode);

    // 节点退出时，删除数据表node[i]（i表示节点ID）
    void deleteTable(int nodeID);

    // 判断标识是否已被注册
    boolean ifExist(int nodeID, String identifier);

    // 添加数据 [hash, identifier, mappingData]
    void registerData(int nodeID, int hash, String identifier, String mappingData);

    // 删除数据 [hash, identifier, mappingData]
    void deleteData(int nodeID, String identifier);

    // 更新数据 [hash, identifier, mappingData]
    void updateData(int nodeID, String identifier, String mappingData);

    // 通过标识解析数据 [hash, identifier, mappingData]
    String resolveData(int nodeID, String identifier);
}
