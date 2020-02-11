package bupt.fnl.dht.dao;

import bupt.fnl.dht.domain.Identity;
import bupt.fnl.dht.domain.NodeVo;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class IdentityDaoTest {

    private InputStream in;
    private SqlSession sqlSession;
    private IdentityDao identityDao;
    private NodeVo vo = new NodeVo();
    private Identity identity = new Identity();

    @Before
    public void initParam() throws IOException {
        // 1、读取配置文件
        in = Resources.getResourceAsStream("SqlMapConfig.xml");
        // 2、创建SqlSessionFactory工厂
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(in);
        // 3、使用工厂生产SqlSession对象
        sqlSession = factory.openSession();
        // 4、使用SqlSession创建Dao代理对象
        identityDao = sqlSession.getMapper(IdentityDao.class);

        vo.setIdentity(identity);
    }
    @After
    public void destroy() throws IOException {
        // 提交事务
        sqlSession.commit();
        // 6、释放方法
        sqlSession.close();
        in.close();
    }

    @Test
    public void testCreateTable() {
        vo.setCurNode("node2");
        identityDao.createTable(vo);
    }
    @Test
    public void testSaveData(){
        vo.setCurNode("node1");
        identity.setHash(225);
        identity.setIdentifier("hr_zhao");
        identity.setMappingData("hr_zhao@bupt.edu.cn");
        identityDao.saveData(vo);
    }
    @Test
    public void testFindData(){
        vo.setCurNode("node1");
        identity.setIdentifier("hr_zhao");
        System.out.println(identityDao.findData(vo).getMappingData());
    }
}