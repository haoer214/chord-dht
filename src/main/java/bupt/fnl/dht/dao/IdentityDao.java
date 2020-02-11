package bupt.fnl.dht.dao;

import bupt.fnl.dht.domain.Identity;
import bupt.fnl.dht.domain.NodeVo;

public interface IdentityDao {

    /**
     * 节点加入时，创建数据表node[i]（i表示节点ID
     */
    void createTable(NodeVo vo);

    /**
     * 节点加入时，部分数据从后继迁移到新节点
     */
    void transferPart(NodeVo vo);

    /**
     * 后继节点删除迁移的部分数据
     */
    void deletePart(NodeVo vo);

    /**
     * 节点退出时，数据迁移到后继节点
     */
    void transferAll(NodeVo vo);

    /**
     * 节点退出时，删除数据表node[i]（i表示节点ID）
     */
    void deleteTable(NodeVo vo);

    /**
     * 添加数据 [Hash, identifier, mappingData]
     */
    void saveData(NodeVo vo);

    /**
     * 删除数据 [Hash, identifier, mappingData]
     */
    void deleteData(NodeVo vo);

    /**
     * 更新数据 [Hash, identifier, mappingData]
     */
    void updateData(NodeVo vo);

    /**
     * 查找数据 [Hash, identifier, mappingData]
     * @return
     */
    Identity findData(NodeVo vo);

}
