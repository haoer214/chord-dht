package bupt.fnl.dht.network;

import bupt.fnl.dht.node.Node;
import bupt.fnl.dht.utils.Message;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.app.chaincode.authority.QueryAuthority;
import org.app.chaincode.hash.InvokeHash;
import org.app.chaincode.hash.QueryHash;

import static bupt.fnl.dht.jdbc.DataBase.*;
import static bupt.fnl.dht.jdbc.DataBase.resolveData;
import static bupt.fnl.dht.utils.Decryption.decrypt;
import static bupt.fnl.dht.utils.Decryption.digest;
import static bupt.fnl.dht.network.NodeDHT.*;
import static bupt.fnl.dht.utils.Hash.HashFunc;

public class Communication {

    /* 收到Web消息后进行一系列权限校验 */
    public static Message authentication(Message received_message) throws Exception {
        Message response_message = received_message;
        // flag == 1，是节点初始化配置指令，来自其他dht节点
        if (received_message.getInitNode_flag() == 1) {
            String response_initInfo = considerInput(received_message.getInitInfo());
            response_message.setInitInfo(response_initInfo);
        }
        // 接收Web后台发送的message（包括 发送者前缀orgName 操作类型type 密文cipherText）
        else {
            // 查询区块链里是否存有发送者的前缀
            String orgName = received_message.getOrgName();
            JSONArray jsonArrayFromBlockchain = QueryAuthority.query(orgName);
            if (jsonArrayFromBlockchain == null || jsonArrayFromBlockchain.isEmpty()) {
                response_message.setFeedback("该企业未注册，没有操作权限！");
                System.out.println("该企业未注册，没有操作权限！");
            } else {
                System.out.println("成功读取企业"+orgName+"注册信息!");
                /*
                 * 控制模块，逻辑判断
                 */
                // 从区块链获取公钥
                String publicKey = jsonArrayFromBlockchain.getJSONObject(0).getJSONObject("Record").getString("public_key");

                // 公钥解密，获得明文
                String cipherText = received_message.getCipherText(); // 密文
                String plainText = decrypt(cipherText, publicKey); // 明文-String格式
                JSONObject plainTextJson = JSONObject.fromObject(plainText); // 明文-Json格式
                // 设置message的值
                response_message.setPlainText(plainText);
                response_message.setIdentity(plainTextJson.getString("identity"));
                response_message.setMappingData(plainTextJson.getString("mappingData"));

                // 遍历所有前缀并对照
                String[] tokens = response_message.getIdentity().split("/");
                String prefix = tokens[0];
                String authority = "";
                for (int i = 0; i < jsonArrayFromBlockchain.size(); i++) {
                    JSONObject tmpJson = jsonArrayFromBlockchain.getJSONObject(i).getJSONObject("Record");
                    if (tmpJson.getString("identity_prefix").equals(prefix)) {
                        authority = tmpJson.getString("authority");
                        break;
                    }
                }
                if (authority.isEmpty()) {
                    response_message.setFeedback("该标识前缀 "+prefix+" 未分配！");
                    System.out.println("该标识前缀 "+prefix+" 未分配！");
                } else {
                    // 比较操作权限
                    int type = received_message.getType();
                    if ((Integer.parseInt(authority) & type) == type) {
                        // 验证成功
                        // 可以进行下一步的增删改查操作
                        response_message = considerType(received_message);
                    } else {
                        response_message.setFeedback("对该前缀没有足够的操作权限！");
                        System.out.println("对该前缀没有足够的操作权限！");
                    }
                }
            }
        }
        return response_message;
    }

    /* 对于不同的操作类型，返回不同的 Message 对象 */
    public static Message considerType(Message message){
        Message result = message;
        switch (message.getType()) {
//            case "getNodeList":
//                result.setNodeList(getNodeList());
//                result.setFeedback("成功获取所有节点信息！");
//                break;
            case 8:
                try {
                    result = registerIdentity(message);
                } catch (Exception e) {
                    System.out.println("注册失败！");
                }
                break;
            case 4:
                try {
                    result = deleteIdentity(message);
                } catch (Exception e) {
                    System.out.println("删除失败！");
                }
                break;
            case 2:
                try {
                    result = updateIdentity(message);
                } catch (Exception e) {
                    System.out.println("更新失败！");
                }
                break;
            case 1:
                try {
                    result = resolveIdentity(message);
                } catch (Exception e) {
                    System.out.println("解析失败！");
                }
        }
        return result;
    }

    /* 增 */
    public static Message registerIdentity(Message message) throws Exception {
        String identity = message.getIdentity();
        String mappingData = message.getMappingData();
        System.out.println("【系统提示】- 有新标识 "+identity+" 请求注册...");
        int kid = HashFunc(identity);//标识的哈希
        Node targetNode = find_successor(kid);//应该存储的位置
        if (targetNode.getID() == me.getID()) {
            // 判断标识是否已被注册
            if (ifExist(me.getID(), identity)) {
                System.out.println("当前标识已被注册！");
                message.setFeedback("当前标识已被注册！");
            } else {
                // 添加到本地节点数据库列表
                registerData(me.getID(), kid, identity, mappingData);
                System.out.println("标识映射 " + identity + "->" + mappingData + " 已存入本地数据库");
                // 将标识、映射数据hash写入区块链
                String mappingDataHash = digest(mappingData);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("identifier",identity);
                jsonObject.put("mappingData_hash",mappingDataHash);
                new InvokeHash("写入").invoke(jsonObject);

                message.setFeedback("标识 " + identity + " 注册成功！");
            }
            return message;
        } else {
            System.out.println("注册请求已经转发至节点 " + targetNode.getID());
            return makeConnectionByObject(targetNode.getIP(), targetNode.getPort(), message);
        }
    }
    /* 删 */
    public static Message deleteIdentity(Message message) throws Exception {
        String identity = message.getIdentity();
        System.out.println("【系统提示】- 收到标识 "+identity+" 的删除请求...");
        int kid = HashFunc(identity);//标识的哈希
        Node targetNode = find_successor(kid);//应该存储的位置
        if (targetNode.getID() == me.getID()) {
            // 判断预删除标识是否存在
            if (!ifExist(me.getID(), identity)) {
                System.out.println("该标识还未注册，无法删除！");
                message.setFeedback("该标识还未注册，无法删除！");
            } else {
                // 删除本地数据库的标识及映射数据
                deleteData(me.getID(), identity);
                System.out.println("标识 " + identity + " 删除成功！");
                // 删除区块链状态数据库中标识的映射数据hash
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("identifier",identity);
                jsonObject.put("mappingData_hash",null); // 这里设为null会有异常
                new InvokeHash("删除").invoke(jsonObject);

                message.setFeedback("标识 " + identity + " 删除成功！");
            }
            return message;
        } else {
            System.out.println("删除请求已经转发至节点 " + targetNode.getID());
            return makeConnectionByObject(targetNode.getIP(), targetNode.getPort(), message);
        }
    }
    /* 改 */
    public static Message updateIdentity(Message message) throws Exception {
        String identity = message.getIdentity();
        String mappingData = message.getMappingData();
        System.out.println("【系统提示】- 标识 "+identity+" 请求更新映射数据...");
        int kid = HashFunc(identity);//标识的哈希
        Node targetNode = find_successor(kid);//应该存储的位置
        if (targetNode.getID() == me.getID()) {
            // 判断标识是否已被注册
            if (!ifExist(me.getID(), identity)) {
                System.out.println("该标识还未注册，无法更新！");
                message.setFeedback("该标识还未注册，无法更新！");
            } else {
                // 更新本地节点数据库列表
                updateData(me.getID(), identity, mappingData);
                System.out.println("标识映射 " + identity + "->" + mappingData + " 已更新至本地数据库");
                // 更新区块链状态数据库中标识的映射数据hash
                String mappingDataHash = digest(mappingData);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("identifier",identity);
                jsonObject.put("mappingData_hash",mappingDataHash);
                new InvokeHash("更新").invoke(jsonObject);

                message.setFeedback("标识 " + identity + " 更新映射数据成功！");
            }
            return message;
        } else {
            System.out.println("更新请求已经转发至节点 " + targetNode.getID());
            return makeConnectionByObject(targetNode.getIP(), targetNode.getPort(), message);
        }
    }
    /* 查 */
    public static Message resolveIdentity(Message message) throws Exception {
        String identity = message.getIdentity();
        System.out.println("【系统提示】- 收到标识 "+identity+" 的解析请求...");
        Node targetNode = find_successor(HashFunc(identity));
        if (targetNode.getID() == me.getID()) {
            // 从本地数据库获取内容
//            System.out.println((resolveData("select *" + " from node" + me.getID() + " where Identity='" + identity + "';"))
//                    .replaceAll("#", "\n"));
            String result = resolveData(me.getID(), identity);
            if (result.equals("")) {
                System.out.println("该标识不存在！");
                message.setFeedback("该标识不存在！");
            } else {
                System.out.println("标识解析成功！正在进行防篡改校验...");
                String mappingDataHash = QueryHash.query(identity);
                if (digest(result).equals(mappingDataHash)) {
                    System.out.println("经验证，解析结果未被篡改");
                    // message写入解析结果
                    message.setMappingData(result);
                    message.setFeedback("标识解析成功！经验证未被篡改");
                } else {
                    System.out.println("【注意！！】解析结果已被篡改！");
                    message.setFeedback("【注意！！】解析结果已被篡改！");
                }
            }
            return message;
        } else {
            System.out.println("解析请求已经转发至节点 " + targetNode.getID());
            // 从其他节点数据库获取内容
//            System.out.println(makeConnection(targetNode.getIP(), targetNode.getPort(), "geturl/" + identity)
//                    .replaceAll("#", "\n"));
            return makeConnectionByObject(targetNode.getIP(), targetNode.getPort(),message);
        }
    }

    /* 获取所有节点信息 */
    public static Node[] getNodeList() {
        // 【注意】必须把List转换为数组才能序列化传输
        Node[] nodes = new Node[nodeList.size()];
        nodeList.toArray(nodes);
        return nodes;
    }
}
