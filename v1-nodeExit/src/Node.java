import java.io.Serializable;

// Node要实现Serializable接口，才能支持序列化
public class Node implements Serializable {
	private int myID;
	private String myIP;
	private String myPort;

	public void setID (int id) {
		this.myID = id;
	}
	public void setIP (String ip) {
		this.myIP = ip;
	}
	public void setPort (String port) {
		this.myPort = port;
	}
	public int getID() {
		return this.myID;
	}
	public String getIP() {
		return this.myIP;
	}
	public String getPort() {
		return this.myPort;
	}
	public Node(int id, String ip, String port){
		myID = id;
		myIP = ip;
		myPort = port;
	}
}

