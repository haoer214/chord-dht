package bupt.fnl.dht.runnable;

import bupt.fnl.dht.domain.Message;
import bupt.fnl.dht.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketThread implements Runnable {

    private Socket connection;
    private IdentityService identityService;

    public SocketThread(Socket connection, IdentityService identityService) {
        this.connection = connection;
        this.identityService = identityService;
    }

    @Override
    public void run() {
        try (
                // 【注意】对于Object IO流，要先创建输出流对象，再创建输入流对象，不然程序会死锁
                ObjectOutputStream outToOtherNodes = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream inFromOtherNodes = new ObjectInputStream(connection.getInputStream()))
        {

            Message received_message = (Message)inFromOtherNodes.readObject();

            /* 收到Web消息后进行一系列权限校验 */
            outToOtherNodes.writeObject(identityService.authentication(received_message));

        } catch (Exception e) {
            System.out.println("【系统提示】- "+"线程无法服务连接");
            e.printStackTrace();
        }
    }
}
