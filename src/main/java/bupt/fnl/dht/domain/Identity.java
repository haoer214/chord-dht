package bupt.fnl.dht.domain;

import java.io.Serializable;

/**
 * Identity：标识对应的实体类
 */
public class Identity implements Serializable {

    private int hash;
    private String identifier;
    private String mappingData;

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMappingData() {
        return mappingData;
    }

    public void setMappingData(String mappingData) {
        this.mappingData = mappingData;
    }

    @Override
    public String toString() {
        return "Identity{" +
                "hash=" + hash +
                ", identifier='" + identifier + '\'' +
                ", mappingData='" + mappingData + '\'' +
                '}';
    }
}
