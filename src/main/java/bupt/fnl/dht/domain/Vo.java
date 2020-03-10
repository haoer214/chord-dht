package bupt.fnl.dht.domain;

/**
 * dao方法参数传入的实体类
 */
public class Vo {

    private int curHash;
    private int sucHash;
    private String curNode;
    private String sucNode;
    private Identity identity;

    public int getCurHash() {
        return curHash;
    }

    public void setCurHash(int curHash) {
        this.curHash = curHash;
    }

    public int getSucHash() {
        return sucHash;
    }

    public void setSucHash(int sucHash) {
        this.sucHash = sucHash;
    }

    public String getCurNode() {
        return curNode;
    }

    public void setCurNode(String curNode) {
        this.curNode = curNode;
    }

    public String getSucNode() {
        return sucNode;
    }

    public void setSucNode(String sucNode) {
        this.sucNode = sucNode;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }
}
