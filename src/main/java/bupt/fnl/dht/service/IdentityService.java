package bupt.fnl.dht.service;

import bupt.fnl.dht.domain.Message;

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
