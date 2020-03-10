package bupt.fnl.dht.service;

import bupt.fnl.dht.domain.Message;
import bupt.fnl.dht.domain.Node;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.app.chaincode.authority.QueryAuthority;
import org.app.chaincode.hash.InvokeHash;
import org.app.chaincode.hash.QueryHash;

import static bupt.fnl.dht.jdbc.DataBase.*;
import static bupt.fnl.dht.jdbc.DataBase.resolveData;
import static bupt.fnl.dht.utils.Decryption.decrypt;
import static bupt.fnl.dht.utils.Decryption.digest;
import static bupt.fnl.dht.utils.Hash.HashFunc;

/**
 * 对收到的Message进行鉴权
 *
 */
public interface IdentityService {

    /* 收到Web消息后进行一系列权限校验 */
    Message authentication(Message received_message);

    /* 对于不同的操作类型，返回不同的 Message 对象 */
    Message considerType(Message message);

    /* 增 */
    Message registerIdentity(Message message);
    /* 删 */
    Message deleteIdentity(Message message);
    /* 改 */
    Message updateIdentity(Message message);
    /* 查 */
    Message resolveIdentity(Message message);
}
