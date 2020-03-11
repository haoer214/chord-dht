package bupt.fnl.dht.dao.impl;

import bupt.fnl.dht.dao.IdentityDao;
import bupt.fnl.dht.domain.Identity;
import bupt.fnl.dht.domain.Vo;
import bupt.fnl.dht.dao.DataBase;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;

@Repository("dataBase")
public class DataBaseImpl implements DataBase {

    private IdentityDao identityDao;
    private SqlSession sqlSession;
    private Vo vo = new Vo();
    private Identity identity = new Identity();

    public void initParam() {
        // 1、读取配置文件
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("SqlMapConfig.xml");
        } catch (IOException e) {
            System.out.println("配置文件读取失败！");
            System.exit(1);
        }
        // 2、创建SqlSessionFactory工厂
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(in);
        // 3、使用工厂生产SqlSession对象
        sqlSession = factory.openSession();
        // 4、使用SqlSession创建Dao代理对象
        identityDao = sqlSession.getMapper(IdentityDao.class);

        vo.setIdentity(identity);
    }

    // 节点加入时，创建数据表node[i]（i表示节点ID）
    public void createTable(int nodeID) {
        vo.setCurNode("node"+nodeID);
        identityDao.createTable(vo);
    }

    // 节点加入时，部分数据从后继迁移到新节点
    public void transferPart(int newNode, int sucNode) {
        vo.setCurNode("node"+newNode);
        vo.setSucNode("node"+sucNode);
        vo.setCurHash(newNode);
        vo.setSucHash(sucNode);
        identityDao.transferPart(vo);
        identityDao.deletePart(vo);
        sqlSession.commit();
    }

    // 节点退出时，数据迁移到后继节点
    public void transferAll(int newNode, int sucNode) {
        vo.setCurNode("node"+newNode);
        vo.setSucNode("node"+sucNode);
        identityDao.transferAll(vo);
        sqlSession.commit();
    }

    // 节点退出时，删除数据表node[i]（i表示节点ID）
    public void deleteTable(int nodeID) {
        vo.setCurNode("node"+nodeID);
        identityDao.deleteTable(vo);
    }

    // 判断标识是否已被注册
    public boolean ifExist(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        return identityDao.findData(vo)!=null;
    }

    // 添加数据 [hash, identifier, mappingData]
    public void registerData(int nodeID, int hash, String identifier, String mappingData) {
        vo.setCurNode("node"+nodeID);
        identity.setHash(hash);
        identity.setIdentifier(identifier);
        identity.setMappingData(mappingData);
        identityDao.saveData(vo);
        sqlSession.commit();
    }

    // 删除数据 [hash, identifier, mappingData]
    public void deleteData(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        identityDao.deleteData(vo);
        sqlSession.commit();
    }

    // 更新数据 [hash, identifier, mappingData]
    public void updateData(int nodeID, String identifier, String mappingData) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        identity.setMappingData(mappingData);
        identityDao.updateData(vo);
        sqlSession.commit();
    }

    // 通过标识解析数据 [hash, identifier, mappingData]
    public String resolveData(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        return identityDao.findData(vo).getMappingData();
    }
}
