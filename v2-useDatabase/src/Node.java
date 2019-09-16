public class Node {
	private int myID;
	private String myIP;
	private String myPort;
    @Override
	public int hashCode() {
		return 3*myID+4*myIP.hashCode()+5*myPort.hashCode();
	}
    @Override
    public boolean equals(Object obj) {
    	Node node=null;
    	if(obj instanceof Node)
    		node=(Node)obj;
    	if(node.getID()==myID&&node.getIP().equals(myIP)&&node.getPort().equals(myPort))
    		return true;
    	else return false;
    }
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