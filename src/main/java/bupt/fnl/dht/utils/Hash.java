package bupt.fnl.dht.utils;

import java.math.BigInteger;
import java.security.MessageDigest;

public class Hash {
    public static int HashFunc(String url, int numDHT) {
        int KID=-1;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(url.getBytes());
            byte[] hashBytes = md.digest();
            BigInteger hashNum = new BigInteger(1,hashBytes);

            KID = Math.abs(hashNum.intValue()) % numDHT;
        }catch (Exception e) {
            e.printStackTrace();
        }

        return KID;
    }
}
