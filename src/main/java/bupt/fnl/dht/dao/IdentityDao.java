package bupt.fnl.dht.dao;

import bupt.fnl.dht.domain.Identity;
import bupt.fnl.dht.domain.Vo;

public interface IdentityDao {

    /**
     * 节点加入时，创建数据表node[i]（i表示节点ID
     */
    void createTable(Vo vo);

    /**
     * 节点加入时，部分数据从后继迁移到新节点
     */
    void transferPart(Vo vo);

    /**
     * 后继节点删除迁移的部分数据
     */
    void deletePart(Vo vo);

    /**
     * 节点退出时，数据迁移到后继节点
     */
    void transferAll(Vo vo);

    /**
     * 节点退出时，删除数据表node[i]（i表示节点ID）
     */
    void deleteTable(Vo vo);

    /**
     * 添加数据 [Hash, identifier, mappingData]
     */
    void saveData(Vo vo);

    /**
     * 删除数据 [Hash, identifier, mappingData]
     */
    void deleteData(Vo vo);

    /**
     * 更新数据 [Hash, identifier, mappingData]
     */
    void updateData(Vo vo);

    /**
     * 查找数据 [Hash, identifier, mappingData]
     * @return
     */
    Identity findData(Vo vo);

}
