package bupt.fnl.dht.domain;

import java.io.Serializable;

/**
 * Message：传输信息对应的实体类
 *【注意】控制组件和DHT节点中的Message类所在的目录名必须相同
 */
public class Message implements Serializable {

    // 节点配置标志位（设置为 1 则是传输节点的初始化信息）
    private int initNode_flag;
    // 节点配置信息
    private String initInfo;

    // 发送者前缀
    private String orgName;
    // 私钥加密后的信息
    private String cipherText;
    // 公钥解密后的信息
    private String plainText;

    // 五种类型：getNodeList, register, delete, update, resolve
    private int type;
    // 标识
    private String identity;
    // 映射数据
    private String mappingData;
    // dht节点信息
    private Node[] nodeList;
    // 反馈信息
    private String feedback;


    public void setInitNode_flag(int initNode_flag){
        this.initNode_flag = initNode_flag;
    }
    public void setInitInfo(String initInfo){
        this.initInfo = initInfo;
    }
    public void setType(int type) {
        this.type = type;
    }
    public void setIdentity(String identity) {
        this.identity = identity;
    }
    public void setMappingData(String mappingData) {
        this.mappingData = mappingData;
    }
    public void setNodeList(Node[] nodeList) {
        this.nodeList = nodeList;
    }
    public void setFeedback(String feedback){
        this.feedback = feedback;
    }

    public int getInitNode_flag(){
        return initNode_flag;
    }
    public String getInitInfo(){
        return initInfo;
    }
    public int getType(){
        return type;
    }
    public String getIdentity(){
        return identity;
    }
    public String getMappingData(){
        return mappingData;
    }
    public Node[] getNodeList(){
        return nodeList;
    }
    public String getFeedback(){
        return feedback;
    }

    public String getCipherText() {
        return cipherText;
    }
    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    }
    public String getPlainText() {
        return plainText;
    }
    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }
    public String getOrgName() {
        return orgName;
    }
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
}
