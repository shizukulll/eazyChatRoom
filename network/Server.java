package network;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
 
public class Server {
	JButton StartServer;//开始服务器
	JButton StopServer;//停止服务器
	JButton SendMessageButton;//发送消息
	JTextField MaxNumberText;//最大连接人数
	JTextField ServerPortText;//监听端口
	JTextField InputContentText;//消息输入框
	JList OnlineClientList;//在线列表
	JLabel LogsLabel;//日志栏
	
	ServerSocket serverSocket;//服务器的socket类型为 ServerSocket
	ServerThread1 serverThread;//服务器线程
	ConcurrentHashMap<String, ClientThread> clientThreads;//key=clientNickName, value=clientThread

	DefaultListModel<String> OnlineClientNickName;
	String ToTargetName  = "ALL";//信息发送的目标用户昵称
	
	public Server() {
		CreateFrame();
	}
	//开始运行服务器
	public void StartServer() {
		int ServerPort = Integer.parseInt(ServerPortText.getText().trim());//删除字符串头尾空白符
		try {
			//对相应端口建立socket
			serverSocket = new ServerSocket(ServerPort);
			//为服务器建立新的线程，在线程里面对socket进行监听
			serverThread = new ServerThread1();
			
			clientThreads = new ConcurrentHashMap<String,ClientThread>();
			
			OnlineClientNickName.addElement("ALL");
		} catch (BindException e) {
			// TODO Auto-generated catch block
			Error("Server：端口异常"+e.getMessage());
		} catch(Exception e) {
			Error("Server：服务器启动失败");
		}
		Success("成功运行服务器");
	}

	public void CreateFrame() {
		JFrame ServerFrame = new JFrame("服务器");
		ServerFrame.setSize(800,600);
		ServerFrame.setLocationRelativeTo(null);
		ServerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//声明服务端信息栏
		JPanel ServerIdPanel = new JPanel();
		ServerIdPanel.setBorder(new TitledBorder("服务器信息栏"));
		ServerIdPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		ServerIdPanel.setSize(800, 100);
		JLabel MaxNumberLabel = new JLabel("    最大连接人数");
		MaxNumberText = new JTextField(10);
		MaxNumberText.setText("10");
		ServerIdPanel.add(MaxNumberLabel);
		ServerIdPanel.add(MaxNumberText);
	
		JLabel ServerPortLabel = new JLabel("    端口");
		ServerPortText = new JTextField(10);
		ServerPortText.setText("8080");
		ServerIdPanel.add(ServerPortLabel);
		ServerIdPanel.add(ServerPortText);
		
		StartServer = new JButton("启动");
		StopServer = new JButton("停止");
		ServerIdPanel.add(StartServer);
		ServerIdPanel.add(StopServer);

		
		//在线用户列表
		JPanel FriendListPanel = new JPanel();
		FriendListPanel.setPreferredSize(new Dimension(200,400));
		FriendListPanel.setBorder(new TitledBorder("好友列表"));		

		OnlineClientNickName = new DefaultListModel<String>();
		OnlineClientList = new JList(OnlineClientNickName);
		FriendListPanel.add(OnlineClientList);

		//日志面板
		JPanel LogsPanel = new JPanel();
		LogsPanel.setPreferredSize(new Dimension(590,400));
		LogsPanel.setBorder(new TitledBorder("日志内容"));
		LogsLabel = new JLabel("<html>");
		LogsLabel.setFont(new java.awt.Font("Dialog", 1, 20));
		LogsLabel.setPreferredSize(new Dimension(590,400));
		LogsPanel.add(LogsLabel);
		
		//输入内容面板
		JPanel InputContentPanel = new JPanel();
		InputContentPanel.setPreferredSize(new Dimension(600,100));
		//聊天输入框
		InputContentText = new JTextField();
		InputContentText.setPreferredSize(new Dimension(600,60));
		//按钮
		SendMessageButton = new JButton("发送");
		InputContentPanel.add(InputContentText);
		InputContentPanel.add(SendMessageButton);
		InputContentPanel.setBorder(new TitledBorder("输入内容"));
		
		ServerFrame.add(ServerIdPanel, BorderLayout.NORTH);
		ServerFrame.add(FriendListPanel, BorderLayout.WEST);
		ServerFrame.add(LogsPanel, BorderLayout.CENTER);
		ServerFrame.add(InputContentPanel,BorderLayout.SOUTH);

		ServerFrame.setVisible(true);	

		AddActionListener();
	}
	//监听
	private void AddActionListener() {
		StartServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				StartServer();
			}
		});
		StopServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		SendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String message = InputContentText.getText();
				if("ALL".equals(ToTargetName)) {
					for(ConcurrentHashMap.Entry<String, ClientThread> entry: clientThreads.entrySet()) {
						entry.getValue().SendMessage("MESSAGE#"+ToTargetName+"#SERVER#"+message);
					}
				}else {
					clientThreads.get(ToTargetName).SendMessage("MESSAGE#"+ToTargetName+"#SERVER#"+message);
				}
			}
		});
		OnlineClientList.addListSelectionListener(new ListSelectionListener() { //检验目标发送者是谁
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				int index = OnlineClientList.getSelectedIndex();
				if(index == 0) {
					ToTargetName = "ALL";
				}
				else {
					String ToClientNickName = (String)OnlineClientNickName.getElementAt(index);
					ToTargetName = ToClientNickName;
				}
				Success("成功修改消息目标用户为："+ToTargetName);
			}
 
		});
	}
	// 输出消息
	private void Log(String message){
		//JLabel不支持\n换行，故添加html标签进行换行，没有</html>结束标签不影响显示
		LogsLabel.setText(LogsLabel.getText()+message+"<br />");
	}
	//输出错误信息
	private void Error(String message){
		LogsLabel.setText(LogsLabel.getText()+"<span color='red'>Error："+message+"</span>"+"<br />");
	}
	//输出成功信息
	private void Success(String message){
		LogsLabel.setText(LogsLabel.getText()+"<span color='green'>Success："+message+"</span>"+"<br />");
	}
	
	//服务器线程
	private class ServerThread1 implements Runnable{
		public ServerThread1() {
			new Thread(this).start();
		}
		@Override
		public void run() {//循环accept下一个socket连接
			
			while(true) {
				if(!serverSocket.isClosed()) {
					try {
						Socket socket = serverSocket.accept();//为每个即将连接的客户端新建一个socket
						ClientThread clientThread = new ClientThread(socket);//每接收到一个服务器请求，就为其新建一个客户线程
						String ClientNickName = clientThread.getClientNickName();						
						clientThreads.put(ClientNickName, clientThread);
						Success("用户"+ClientNickName+"登录成功！");
					} catch (IOException e) {
						Error("Server：用户登录失败！"+e.getMessage());
					}
				}else {
					Error("Server:未知错误！");
				}
			}
		}
	}
	//客户端线程
	public class ClientThread implements Runnable{
		private Socket socket;
		private BufferedReader input;
		private PrintStream output;
		private String ClientNickName;
		
		boolean isRuning = false;//区分第一次连接和之后接收的消息

		public ClientThread(Socket clientSocket) {
			this.socket = clientSocket;
			
			isRuning = Initialize();
			new Thread(this).start();
		}
		// 第一次生成线程时调用
		public synchronized boolean Initialize() {
			try {
				input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				output = new PrintStream(socket.getOutputStream());
				// 接收用户的输入数据
				String clientInputStr;
				clientInputStr = input.readLine();//readline运行时阻塞，故须建立客户端线程
				Log("Client："+clientInputStr);
				//检验信息头是否为LOGIN，是则向所有其他用户转发
				Tokenizer tokens = new Tokenizer(clientInputStr,"#");
				String MessageType = tokens.nextToken();
				if("LOGIN".equals(MessageType)) {
					ClientNickName = tokens.nextToken();
					OnlineClientNickName.addElement(ClientNickName);//服务端在线用户列表显示该用户昵称
					Broadcast(clientInputStr);//向所有已登陆用户广播该用户登陆的信息
					for(ConcurrentHashMap.Entry<String, ClientThread> entry: clientThreads.entrySet()) {
						SendMessage("LOGIN#"+entry.getKey());
					}
				}else if("LOGIN1".equals(MessageType)) {
					ClientNickName = tokens.nextToken();
					OnlineClientNickName.addElement(ClientNickName);//服务端在线用户列表显示该用户昵称
					for(ConcurrentHashMap.Entry<String, ClientThread> entry: clientThreads.entrySet()) {
						SendMessage("LOGIN#"+entry.getKey());
					}	
				}else if("LOGIN2".equals(MessageType)) {
					ClientNickName = tokens.nextToken();
					OnlineClientNickName.addElement(ClientNickName);//服务端在线用户列表显示该用户昵称
					Broadcast(clientInputStr);//向所有已登陆用户广播该用户登陆的信息
					for(ConcurrentHashMap.Entry<String, ClientThread> entry: clientThreads.entrySet()) {
						SendMessage("LOGIN#"+entry.getKey());
					}	
				}
				else {
					Error("Server:该用户不是在线状态登录！");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Error("Server：未知错误！ "+e.getMessage());
			}
			return true;
		}

		public void run() {
			while(isRuning) {
				try {
					//接收用户的输入数据
					String clientInputStr;
					clientInputStr = input.readLine();
					Log("Server："+clientInputStr);

					//按信息头部分类处理
					Tokenizer tokens = new Tokenizer(clientInputStr,"#");
					String MessageType = tokens.nextToken();
					switch(MessageType) {
					case "MESSAGE":{
						String ToClientNickName = tokens.nextToken();
						if(ToClientNickName.equals("ALL")) {
							//广播
							Broadcast(clientInputStr);
							tokens.nextToken();
							Log("Server：已将消息广播转发，消息内容为"+tokens.nextToken());
						}else {
							//对消息进行一对一的转发
							clientThreads.get(ToClientNickName).SendMessage(clientInputStr);
							Log("Server: 已将来自"+tokens.nextToken()+"的消息"+tokens.nextToken()+"转发给"+ToClientNickName);
						}
						break;
					}
					case "DISCONNECTED":{
						Broadcast(clientInputStr);
						tokens.nextToken();//对象肯定是all，不管
						String name = tokens.nextToken();//得到用户名
						clientThreads.remove(name);
						OnlineClientNickName.removeElement(name);
						Log("Server：已将下线消息广播转发，消息内容为"+clientInputStr);
						break;
					}
					case "LOGIN1":{//隐身
						tokens.nextToken();
						String name = tokens.nextToken();
						Log("Server：已将下线消息广播转发，消息内容为"+clientInputStr);
						Broadcast("DISCONNECTED#ALL#" + name + "#disConnected");
						break;
					}
					case "RELOGIN":{//上线
						tokens.nextToken();
						String name = tokens.nextToken();
						Broadcast("RELOGIN#ALL#" + name + "#online");
						Log("Server：已将上线消息广播转发，消息内容为"+clientInputStr);
						break;
					}
					case "LOGIN2":{
						Broadcast(clientInputStr);
						break;
					}
					default : {
						Error("Server: 服务器收到的消息格式错误");
						break;
					}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
//					Error(" ");
				} 
			}
 
		}
		// 返回该用户昵称
		public String getClientNickName() {
			return ClientNickName;
		}
		//发送消息
		public void SendMessage(String Message) {
			output.println(Message);
			output.flush();
		}
		//广播
		public void Broadcast(String Message) {
			for(ConcurrentHashMap.Entry<String, ClientThread> entry: clientThreads.entrySet()) {
				entry.getValue().SendMessage(Message);
			}
		}
		
	}
	//消息分割
	public class Tokenizer{
		String Tokens[];
		int TokenIndex = 0;
		public Tokenizer(String Message, String Delimiter) {
			Tokens = Message.split(Delimiter);
		}
		//返回下一个内容
		public String nextToken() {
			TokenIndex++;
			if (TokenIndex <= Tokens.length) {  
			    return Tokens[TokenIndex - 1];  
			} else {  
			    return null; // or throw an exception, depending on your requirements  
			}
		}
	}
	
	public static void main(String arg[]) {
		Server server = new Server();
}
}