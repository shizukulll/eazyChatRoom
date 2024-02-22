package network;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

//import java.util.Arrays;
//import javax.swing.text.html.HTMLEditorKit;  
//import javax.swing.text.AttributedString;

public class Client {
	// UI组件
	JButton ConnectServer;// 连接
	JButton SendMessageButton;// 发送
	JTextField NickNameText;// 昵称
	JTextField ServerIPAddressText;// 服务器ip
	JTextField ServerPortText;// 服务器端口
	JTextField InputContentText;
	JList OnlineClientList;// 在线列表
	JLabel ChatContentLabel;
	JLabel Tell;
	String[] state = { "在线", "隐身", "忙碌","自动回复","关闭自动回复"};
	JComboBox<String> lib = new JComboBox<String>(state);
	String str = "LOGIN#";
	JTextArea chatContentArea;
	JTextPane textPane = new JTextPane(); 
	JScrollBar jscrollBar ;
	
	JButton bl = new JButton("登录");
	JButton bl1 = new JButton("确定");
	JButton cleanButton;

	JTextField loginname = new JTextField(20);
	JTextField autoReplyFiled = new JTextField(20);

	// socket相关
	Socket socket;// input和output是通过socket定义的，如果socket关闭了，其他两个也失效
	BufferedReader input;// input为服务器传来的数据
	PrintStream output;// output为向服务器输出的数据

	// 用户昵称
	DefaultListModel<String> OnlineClientNickName;// 在线用户昵称列表
	String ToTargetName = "ALL";// 目标用户昵称 OnlineClientList的监听器对其修改
	ClientThread cliendThread;// 客户端线程
	DefaultListModel<String> banList = new DefaultListModel<>();
	
	
	Boolean isBusy = false;
	boolean autoReply = false;
	String autoReplyStr;

	public Client() {
		LoginFrame();
		CreateFrame();

	}

	public void LoginFrame() {
		final JFrame LoginFrame = new JFrame("登录窗口");
		LoginFrame.setSize(400, 300);
		LoginFrame.setLayout(new  FlowLayout(FlowLayout.CENTER , 15,15));
		LoginFrame.setVisible(true);
		LoginFrame.setLocationRelativeTo(null);
		LoginFrame.setAlwaysOnTop(true);
		JLabel Tell = new JLabel("请输入登录用户名：");
		Tell.setFont(new java.awt.Font("Dialog", 1, 20));
		loginname.setFont(new java.awt.Font("Dialog", 1, 20));
		
		LoginFrame.add(Tell);
		LoginFrame.add(loginname);
		LoginFrame.add(bl);
		
		LoginFrame.setResizable(false);
		bl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				NickNameText.setText(loginname.getText());
				LoginFrame.dispose();
			}
		});
	}

	public void CreateFrame() {
		
		JFrame ClientFrame = new JFrame("客户端");
		ClientFrame.setSize(800, 600);
		ClientFrame.setLocationRelativeTo(null);
//		ClientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ClientFrame.addWindowListener(new WindowAdapter() {  
	            public void windowClosing(WindowEvent e) {  
	                try {
						disConnected();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}  
	                System.exit(0);
	            }  
	        });  
        
		JPanel ClientIdPanel = new JPanel();
		ClientIdPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		ClientIdPanel.setSize(800, 100);
		// 昵称栏
		JLabel NickNameLabel = new JLabel("昵称");
		NickNameText = new JTextField(10);

		ClientIdPanel.add(NickNameLabel);
		ClientIdPanel.add(NickNameText);
		// 服务器IP地址
		JLabel ServerIPAddressLabel = new JLabel("IP地址");
		ServerIPAddressText = new JTextField(10);
		ServerIPAddressText.setText("127.0.0.1");
		ClientIdPanel.add(ServerIPAddressLabel);
		ClientIdPanel.add(ServerIPAddressText);
		// 端口号
		JLabel ServerPortLabel = new JLabel("端口");
		ServerPortText = new JTextField(10);
		ServerPortText.setText("8080");
		ClientIdPanel.add(ServerPortLabel);
		ClientIdPanel.add(ServerPortText);
		// 连接服务器,状态下拉列表
		ConnectServer = new JButton("连接");
		ClientIdPanel.add(ConnectServer);

		JComboBox jcombo = new JComboBox(state);
		ClientIdPanel.add(lib);
		
		ClientIdPanel.setBorder(new TitledBorder("用户信息栏"));

		// 好友列表
		JPanel FriendListPanel = new JPanel();
		FriendListPanel.setPreferredSize(new Dimension(200, 400));
		FriendListPanel.setBorder(new TitledBorder("好友列表"));
		// 好友列表内容
		OnlineClientNickName = new DefaultListModel<String>();
		OnlineClientList = new JList(OnlineClientNickName);
		OnlineClientList.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        if(e.getButton() == MouseEvent.BUTTON3) {
		            // 显示右键菜单
		            JPopupMenu popupMenu = new JPopupMenu();
		            JMenuItem menuItem1 = new JMenuItem("拉黑");
		            JMenuItem menuItem2 = new JMenuItem("取消拉黑");

		            menuItem1.addActionListener(new ActionListener() {
		                @Override
		                public void actionPerformed(ActionEvent e) {
		                    String selectedName = (String) OnlineClientList.getSelectedValue();
		                    Log("已拉黑" + selectedName);
		                    SendMessage("MESSAGE#" + selectedName + "#" + NickNameText.getText() + "#你已经被" + NickNameText.getText() + "拉黑");
		                    banList.addElement(selectedName);
		                }
		            });
		            
		            menuItem2.addActionListener(new ActionListener() {
		                @Override
		                public void actionPerformed(ActionEvent e) {
		                    String selectedName = (String) OnlineClientList.getSelectedValue();
		                    Log("已取消拉黑" + selectedName);
		                    SendMessage("MESSAGE#" + selectedName + "#" + NickNameText.getText() + "#你已经被" + NickNameText.getText() + "取消拉黑");
		                    banList.removeElement(selectedName);
		                }
		            });
		            
		            popupMenu.add(menuItem1); 
		            popupMenu.add(menuItem2); 
		            popupMenu.show(OnlineClientList, e.getX(), e.getY()); 
		        }
		    }
		});
		
		
		FriendListPanel.add(OnlineClientList);

		// 聊天内容
		JPanel ChatContentPanel = new JPanel();
//		ChatContentPanel.setPreferredSize(new Dimension(490, 400));
//		ChatContentPanel.setBorder(new TitledBorder("聊天内容"));
//		ChatContentLabel = new JLabel("<html>");
//		ChatContentLabel.setFont(new java.awt.Font("Dialog", 1, 20));
//		ChatContentLabel.setPreferredSize(new Dimension(490, 400));
//		ChatContentPanel.add(ChatContentLabel);
		chatContentArea = new JTextArea();
		chatContentArea.setFont(new Font("Dialog", Font.BOLD, 20));
		chatContentArea.setEditable(false);
//		chatContentArea.setSize(new Dimension(490, 400));
		JScrollPane scrollPane = new JScrollPane(chatContentArea);
		scrollPane.setPreferredSize(new Dimension(490, 400));
		scrollPane.doLayout();
//		ChatContentPanel.add(chatContentArea);
		jscrollBar = scrollPane.getVerticalScrollBar();
	    if (jscrollBar != null)
	        jscrollBar.setValue(jscrollBar.getMaximum());
		// 输入内容
		JPanel InputContentPanel = new JPanel();
		InputContentPanel.setPreferredSize(new Dimension(600, 100));
		InputContentText = new JTextField();
		InputContentText.setPreferredSize(new Dimension(600, 60));
		SendMessageButton = new JButton("发送");
		cleanButton = new JButton("clean");
		InputContentPanel.add(InputContentText);
		InputContentPanel.add(SendMessageButton);
		InputContentPanel.add(cleanButton);
		InputContentPanel.setBorder(new TitledBorder("输入内容"));
//		InputContentText.addKeyListener(new KeyAdapter(){
//			public void keyTyped(KeyEvent e) {
//			if((char)e.getKeyChar()==KeyEvent.VK_ENTER) {
//				String message = InputContentText.getText().trim();
//				if(message == "") {
//					message = "发送空消息";
//				}
//				SendMessage("MESSAGE#" + ToTargetName + "#" + NickNameText.getText() + "#" + message);
//				InputContentText.setText("");
//			}}
//		});

		// 客户端整体布局
		ClientFrame.add(ClientIdPanel, BorderLayout.NORTH);
		ClientFrame.add(FriendListPanel, BorderLayout.WEST);
//		ClientFrame.add(ChatContentPanel, BorderLayout.CENTER);
		ClientFrame.add(scrollPane, BorderLayout.CENTER);
		ClientFrame.add(InputContentPanel, BorderLayout.SOUTH);


		ClientFrame.setVisible(true);
		AddActionListener();
	}

	// 连接服务器
	public void ConnectServer() {
		// 获取基本信息
		String ServerIPAddress = ServerIPAddressText.getText().trim();// 删除头尾空白
		int ServerPort = Integer.parseInt(ServerPortText.getText().trim());
		String NickName = NickNameText.getText();

		try {
			// socket相关
			socket = new Socket(ServerIPAddress, ServerPort);
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintStream(socket.getOutputStream());
			OnlineClientNickName.addElement("所有人");
			OnlineClientNickName.addElement(NickName);
			SendMessage(str + NickName);// 向服务器发送本帐号登陆消息
			// 为客户端建立线程:
			cliendThread = new ClientThread();

		} catch (UnknownHostException e) {
			Error("Client：主机地址异常" + e.getMessage());
			return;
		} catch (IOException e) {
			Error("Client：连接服务器异常" + e.getMessage());
			return;
		}
	}

	private void AddActionListener() {	// 点击连接
		ConnectServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ConnectServer();
				ConnectServer.setEnabled(false);
			}
		});
		lib.addItemListener(new ItemListener() {	//点击状态
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				int index = lib.getSelectedIndex(); 
				switch(index) {
				case 0:
					str = "RELOGIN#";
					SendMessage(str + "ALL#" + NickNameText.getText() + "#online");
					isBusy = false;
					break;
				case 1:
					str = "LOGIN1#";
					SendMessage(str + "ALL#" + NickNameText.getText() + "#invisible");
					break;
				case 2:
					str = "LOGIN2#";
					SendMessage(str + "ALL#" + NickNameText.getText() + "#busy");
					Log("用户"+NickNameText.getText()+"忙碌中！");
					isBusy = true;
					break;
				case 3:
					AutoReplyFrame();
					autoReply = true;
					break;
				case 4: 
					autoReply = false;
					break;
				}
			}
		});
		SendMessageButton.addActionListener(new ActionListener() {		// 点击发送
			public void actionPerformed(ActionEvent e) {
				String message = InputContentText.getText().trim();
				SendMessage("MESSAGE#" + ToTargetName + "#" + NickNameText.getText() + "#" + message);
				InputContentText.setText("");
			}
		});
		cleanButton.addActionListener(new ActionListener() {		// 点击发送
			public void actionPerformed(ActionEvent e) {
				chatContentArea.setText("");
				 
			}
		});

		// 检验目标发送者是谁
		OnlineClientList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = OnlineClientList.getSelectedIndex();//返回列表中被选的索引号
				if (index == 0) {
					ToTargetName = "ALL";
				} else {
					String ToClientNickName = (String) OnlineClientNickName.getElementAt(index);
					ToTargetName = ToClientNickName;
				}
			}
		});
	}

	//输出错误
	private void Error(String message) {
		// JLabel不支持\n换行，故添加html标签进行换行
//		ChatContentLabel.setText(ChatContentLabel.getText() + "<span color='red' >" + message + "</span>" + "<br />");
		chatContentArea.append(message + "\n");  
		 if (jscrollBar != null)
		        jscrollBar.setValue(jscrollBar.getMaximum());
	}

	//输出上线下线内容
	private void Log(String message) {
//		ChatContentLabel.setText(ChatContentLabel.getText() + "<span  color='blue'>" + message + "</span>" + "<br />");
		chatContentArea.append(message + "\n");  
		 if (jscrollBar != null)
		        jscrollBar.setValue(jscrollBar.getMaximum());
	}

	//输出私聊内容
	private void Message(String message) {
//		ChatContentLabel.setText(ChatContentLabel.getText() + "<span color='black'>" + message + "</span>" + "<br />");
		chatContentArea.append(message + "\n");  
		 if (jscrollBar != null)
		        jscrollBar.setValue(jscrollBar.getMaximum());
	}

	//输出广播内容
	private void MessageTotal(String message) {
//		ChatContentLabel.setText(ChatContentLabel.getText() + "<span color='green'>" + message + "</span>" + "<br />");
		chatContentArea.append(message + "\n");  
		 if (jscrollBar != null)
		        jscrollBar.setValue(jscrollBar.getMaximum());
	}

	//客户端线程
	public class ClientThread implements Runnable {// 与服务器建立连接时，新建客户端线程，接收信息
		// 客户端调用readline时会产生死锁
		public ClientThread() {
			new Thread(this).start();
		}
		//run函数会在线程开始时自动调用
		public void run() {
			while (true) {//客户端断开连接之前不停止			
				String message;
				try {				
					message = input.readLine();// 在服务器传来的消息中读取下一行
					Tokenizer tokens = new Tokenizer(message, "#");// 对原有消息进行分割
					String MessageType = tokens.nextToken();
					
					//根据登录协议对消息进行显示
					switch (MessageType) {
					case "LOGIN": {// 上线信息
						String LoginClientNickName = tokens.nextToken();
						Log("上线通知：用户" + LoginClientNickName + "已上线");
						OnlineClientNickName.addElement(LoginClientNickName);
						break;
					}
//					case"LOGIN1":{//隐身
//						String LoginClientNickName1 = tokens.nextToken();
//						OnlineClientNickName.addElement(LoginClientNickName1);
//						break;
//					}
					case "LOGIN2":{
						tokens.nextToken();
						String LoginClientNickName = tokens.nextToken();
						Log("用户" + LoginClientNickName + "状态为忙碌,请勿打扰");
//						OnlineClientNickName.addElement(LoginClientNickName);
						break;
					}
					case "MESSAGE": {// 聊天消息
						String ToClientNickName = tokens.nextToken();
						String FromClientNickName = tokens.nextToken();
						String content = tokens.nextToken();
						
						if ("ALL".equals(ToClientNickName)) {
							MessageTotal("来自" + FromClientNickName + "对全体的消息：" + content);
						} else {
							if(!banList.contains(FromClientNickName)) {
								Message("来自" + FromClientNickName + "对您的私聊消息：" + content);
							if(isBusy) {
								SendMessage("MESSAGE#" + FromClientNickName + "#" + ToClientNickName + "#状态为忙碌,请勿打扰，谢谢！");
							}
							if(autoReply) {
								SendMessage("MESSAGE#" + FromClientNickName + "#" + ToClientNickName + "#[自动回复]:" + autoReplyStr);
							}
							}
						}
						break;
					}
					case "DISCONNECTED":{//断开连接
						tokens.nextToken();
						String FromClientNickName = tokens.nextToken();
						MessageTotal("来自" + FromClientNickName + "下线");
						OnlineClientNickName.removeElement(FromClientNickName);
						break;
					}
					case "RELOGIN":{
						tokens.nextToken();
						String LoginClientNickName = tokens.nextToken();
						if(!OnlineClientNickName.contains(LoginClientNickName)){
							  OnlineClientNickName.addElement(LoginClientNickName);
							}
						Log("上线通知：用户" + LoginClientNickName + "已上线");
						break;
					}
					default: {
						Error("客户端接收消息格式错误");
						break;
					}
					}
					System.out.println("客户端接收到" + message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Error("Client：客户端接收消息失败" + e.getMessage());
				}
			}
		}
	}
	
	public void disConnected() throws IOException {
		if (socket != null && !socket.isClosed()) {  
		SendMessage("DISCONNECTED#ALL#" + NickNameText.getText() + "#disConnected");
        socket.close();
        }
	}

	public void SendMessage(String message) {
		output.println(message);
		output.flush();
	}

	//消息分割
	public class Tokenizer {
		String Tokens[];
		int TokenIndex = 0;
		//把Message，按#进行分割
		public Tokenizer(String Message, String Delimiter) {
			Tokens = Message.split(Delimiter);
			//System.out.println(Arrays.toString(Tokens));
		}
		//获取下一项
		public String nextToken() {
			TokenIndex++; 
			return Tokens[TokenIndex-1];
			//return Tokens[TokenIndex];
		}
	}
	
	public void AutoReplyFrame() {
		final JFrame autoReplyFrame = new JFrame("自动回复窗口");
		autoReplyFrame.setSize(400, 300);
		autoReplyFrame.setLayout(new  FlowLayout(FlowLayout.CENTER , 15,15));
		autoReplyFrame.setVisible(true);
		autoReplyFrame.setLocationRelativeTo(null);
		autoReplyFrame.setAlwaysOnTop(true);
		JLabel Tell = new JLabel("请输入自动回复语句：");
		Tell.setFont(new java.awt.Font("Dialog", 1, 20));
		autoReplyFiled.setFont(new java.awt.Font("Dialog", 1, 20));
		
		autoReplyFrame.add(Tell);
		autoReplyFrame.add(autoReplyFiled);
		autoReplyFrame.add(bl1);
		
		autoReplyFrame.setResizable(false);
		bl1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				autoReplyStr = autoReplyFiled.getText();
//				NickNameText.setText(loginname.getText());
				autoReplyFrame.dispose();
			}
		});
	}

	public static void main(String srgs[]) {
		Client client = new Client();
	}
}
