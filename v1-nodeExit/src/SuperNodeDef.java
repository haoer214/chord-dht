import java.rmi.*;

//
// SuperNodeDef Interface
// RMI Interface
//
public interface SuperNodeDef extends Remote
{
	public String getRandomNode() throws RemoteException;

	public String getNodeList() throws RemoteException;

	// 节点退出时，列表删除退出节点信息，返回网络中现存的节点
	public Node[] setNodeList(int exitID) throws RemoteException;

	public void finishJoining(int id) throws RemoteException;

	public String getNodeInfo(String ipAddr, String port) throws RemoteException;

}
