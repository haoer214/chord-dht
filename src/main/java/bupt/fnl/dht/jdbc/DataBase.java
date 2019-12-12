package bupt.fnl.dht.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * 使用jdbc实现与mysql数据库的交互
 */

public class DataBase {
    // 数据库连接对象Connection
    public static Connection conn;

    // 初始化jdbc连接信息，加载mysql驱动，建立Connection
    public static void initParam(String paramFile) throws Exception {
        Properties props = new Properties();
        // 通过当前路径下的.ini配置文件进行初始化
        props.load(new FileInputStream(new File(paramFile)));
        // 连接到mysql所需要的配置信息
        String driver = props.getProperty("driver");
        String url = props.getProperty("url");
        String user = props.getProperty("user");
        String pass = props.getProperty("pass");
        System.out.println("配置信息初始化成功！");
        // 加载驱动
        Class.forName(driver);
        System.out.println("驱动加载成功！");
        // 建立连接
        conn = DriverManager.getConnection(url, user, pass);
        System.out.println("数据库连接建立成功！");
    }

    // 节点加入时，创建数据表node[i]（i表示节点ID）
    public static void createTable(int nodeID) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "create table node?(Hash int, Identity varchar(255) primary key, Content text);")) {
            pstmt.setInt(1, nodeID);
            pstmt.executeUpdate();
        }
    }

    // 节点加入时，部分数据从后继迁移到新节点
    public static void transferPart(int newNode, int sucNode) throws Exception {
        try (
                PreparedStatement pstmt1 = conn.prepareStatement(
                        "insert into node? select * from node? where Hash<=?&&Hash>?;")) {
            pstmt1.setInt(1, newNode);
            pstmt1.setInt(2, sucNode);
            pstmt1.setInt(3, newNode);
            pstmt1.setInt(4, sucNode);
            pstmt1.executeUpdate();
        }
        try (
                PreparedStatement pstmt2 = conn.prepareStatement(
                        "delete from node? where Hash<=?&&Hash>?;")) {
            pstmt2.setInt(1, sucNode);
            pstmt2.setInt(2, newNode);
            pstmt2.setInt(3, sucNode);
            pstmt2.executeUpdate();
        }
    }

    // 节点退出时，数据迁移到后继节点
    public static void transferAll(int newNode, int sucNode) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "insert into node? select * from node?;")) {
            pstmt.setInt(1, sucNode);
            pstmt.setInt(2, newNode);
            pstmt.executeUpdate();
        }
    }

    // 节点退出时，删除数据表node[i]（i表示节点ID）
    public static void deleteTable(int nodeID) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "drop table node?;")) {
            pstmt.setInt(1, nodeID);
            pstmt.executeUpdate();
        }
    }

    // 判断标识是否已被注册
    public static boolean ifExist(int nodeID, String identity) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "select * from node? where Identity=?;")) {
            pstmt.setInt(1, nodeID);
            pstmt.setString(2, identity);
            try (
                    ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return true;
            }
        }
        return false;
    }

    // 添加数据 [Hash, Identity, Content]
    public static void registerData(int nodeID, int hash, String identity, String content) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "insert into node?(Hash, Identity, Content) values(?, ?, ?);")) {
            pstmt.setInt(1, nodeID);
            pstmt.setInt(2, hash);
            pstmt.setString(3, identity);
            pstmt.setString(4, content);
            pstmt.executeUpdate();
        }
    }

    // 删除数据 [Hash, Identity, Content]
    public static void deleteData(int nodeID, String identity) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "delete from node? where Identity=?;")) {
            pstmt.setInt(1, nodeID);
            pstmt.setString(2, identity);
            pstmt.executeUpdate();
        }
    }

    // 更新数据 [Hash, Identity, Content]
    public static void updateData(int nodeID, String identity, String content) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "update node? set Content=? where Identity=?;")) {
            pstmt.setInt(1, nodeID);
            pstmt.setString(2, content);
            pstmt.setString(3, identity);
            pstmt.executeUpdate();
        }
    }

    // 通过标识解析数据 [Hash, Identity, Content]
    public static String resolveData(int nodeID, String identity) throws Exception {
        try (
                PreparedStatement pstmt = conn.prepareStatement(
                        "select * from node? where Identity=?;")) {
            pstmt.setInt(1, nodeID);
            pstmt.setString(2, identity);
            try (
                    ResultSet rs = pstmt.executeQuery()) {
                String result = "";
                if (rs.next()) {
//                result  = "<解析结果>: " + "#"
//                        + "【哈希】 " + rs.getInt(1) + "#"
//                        + "【标识】 " + rs.getString(2) + "#"
//                        + "【内容】 " + rs.getString(3) + "#";
                    result = rs.getString(3);
                }
                return result;
            }
        }
    }
}
