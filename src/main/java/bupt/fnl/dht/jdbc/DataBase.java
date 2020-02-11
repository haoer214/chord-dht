package bupt.fnl.dht.jdbc;

import bupt.fnl.dht.dao.IdentityDao;
import bupt.fnl.dht.domain.Identity;
import bupt.fnl.dht.domain.NodeVo;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * 使用jdbc实现与mysql数据库的交互
 */

public class DataBase {

    private static IdentityDao identityDao;
    private static SqlSession sqlSession;
    private static NodeVo vo = new NodeVo();
    private static Identity identity = new Identity();

    public static void initParam() throws IOException {
        // 1、读取配置文件
        InputStream in = Resources.getResourceAsStream("SqlMapConfig.xml");
        // 2、创建SqlSessionFactory工厂
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(in);
        // 3、使用工厂生产SqlSession对象
        sqlSession = factory.openSession();
        // 4、使用SqlSession创建Dao代理对象
        identityDao = sqlSession.getMapper(IdentityDao.class);

        vo.setIdentity(identity);
    }

    // 节点加入时，创建数据表node[i]（i表示节点ID）
    public static void createTable(int nodeID) {
        vo.setCurNode("node"+nodeID);
        identityDao.createTable(vo);
    }

    // 节点加入时，部分数据从后继迁移到新节点
    public static void transferPart(int newNode, int sucNode) {
        vo.setCurNode("node"+newNode);
        vo.setSucNode("node"+sucNode);
        vo.setCurHash(newNode);
        vo.setSucHash(sucNode);
        identityDao.transferPart(vo);
        identityDao.deletePart(vo);
        sqlSession.commit();
    }

    // 节点退出时，数据迁移到后继节点
    public static void transferAll(int newNode, int sucNode) {
        vo.setCurNode("node"+newNode);
        vo.setSucNode("node"+sucNode);
        identityDao.transferAll(vo);
        sqlSession.commit();
    }

    // 节点退出时，删除数据表node[i]（i表示节点ID）
    public static void deleteTable(int nodeID) {
        vo.setCurNode("node"+nodeID);
        identityDao.deleteTable(vo);
    }

    // 判断标识是否已被注册
    public static boolean ifExist(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        return identityDao.findData(vo)!=null;
    }

    // 添加数据 [hash, identifier, mappingData]
    public static void registerData(int nodeID, int hash, String identifier, String mappingData) {
        vo.setCurNode("node"+nodeID);
        identity.setHash(hash);
        identity.setIdentifier(identifier);
        identity.setMappingData(mappingData);
        identityDao.saveData(vo);
        sqlSession.commit();
    }

    // 删除数据 [hash, identifier, mappingData]
    public static void deleteData(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        identityDao.deleteData(vo);
        sqlSession.commit();
    }

    // 更新数据 [hash, identifier, mappingData]
    public static void updateData(int nodeID, String identifier, String mappingData) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        identity.setMappingData(mappingData);
        identityDao.updateData(vo);
        sqlSession.commit();
    }

    // 通过标识解析数据 [hash, identifier, mappingData]
    public static String resolveData(int nodeID, String identifier) {
        vo.setCurNode("node"+nodeID);
        identity.setIdentifier(identifier);
        return identityDao.findData(vo).getMappingData();
    }
}
