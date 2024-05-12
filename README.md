
# 如何跑通
- 将手机和电脑都连自己的热点
- 先运行服务器得到可监听的地址
- 更新客户端安卓消息线程和文件线程的socker目标地址为可监听地址
- 然后数据线连接手机运行，此时手机便多了个app，然后可以不需要数据线单独运行了
# 代码仓库地址
[https://github.com/FULLK/llkchatroom/](https://github.com/FULLK/llkchatroom/)
# 客户端
## 登录
输入用户名。获取输入的用户名和通信IP

```java

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        input_name = (TextView) findViewById(R.id.input_name);
        Button confirm = (Button) findViewById(R.id.confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Name.name = input_name.getText().toString();
                //得到输入的字符串
                Name.IP = getLocalIpAddress();
                Log.e("Register", Name.IP + Name.name);
                if (!Name.name.equals("")) {
                    //输入内容不为空那么点击就跳转到chatromm界面
                    Intent intent = new Intent(MainActivity.this, Chatroom.class);
                    startActivity(intent);
                }
            }
        });
    }
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddre", ex.toString());
        }

        return null;
    }
```
首先是输入用户名存到Name结构体中

```java
public class Name {
    public static String name ;
    public static  String IP;
}

```

这段Java代码遍历了本机的所有网络接口（NetworkInterface），然后对于每个网络接口，进一步遍历其绑定的所有IP地址（InetAddress）。其核心目的是找到并返回一个符合条件的IPv4或IPv6地址，该地址既不是回环地址（loopback address，如127.0.0.1），也不是链路本地地址（link-local address，这类地址仅用于同一链路上的通信，如IPv6的fe80::/10范围内的地址）。具体步骤如下：

1. **获取网络接口枚举**：首先通过`NetworkInterface.getNetworkInterfaces()`方法获取到本机所有网络接口的枚举（Enumeration）对象。网络接口可以理解为计算机上的物理或虚拟网卡。

2. **遍历网络接口**：使用`hasMoreElements()`和`nextElement()`方法遍历所有的网络接口。对于每个网络接口`intf`：

3. **获取IP地址枚举**：通过`intf.getInetAddresses()`方法获取该网络接口上绑定的所有IP地址的枚举。

4. **遍历IP地址**：再次使用`hasMoreElements()`和`nextElement()`遍历这些IP地址。对于每个IP地址`inetAddress`：

5. **检查地址类型**：使用`isLoopbackAddress()`方法检查这个IP地址是否是回环地址，使用`isLinkLocalAddress()`方法检查是否是链路本地地址。这两个条件都不满足，意味着这个IP地址是可外部访问的地址。

6. **返回符合条件的IP地址**：一旦找到一个既不是回环地址也不是链路本地地址的IP地址，就立即通过`getHostAddress().toString()`获取其字符串表示形式并返回。这意味着该方法最终返回的是本机的第一个非回环、非链路本地的IP地址。
## 发送消息
定义消息类
```java
public class Msg {
    public static final int TYPE_RECEIVED = 0;//收到的消息
    public static final int TYPE_SENT = 1;//发出去的消息
    private String name;
    private String content;
    private int type;
    //content表示消息内容，type表示类型
    public Msg(String name,String content ,int type){
        this.name = name;
        this.content = content;
        this.type = type;
    }
    public String getContent(){
        return content;
    }
    public int getType(){
        return type;
    }
    public String getName() {return name;}
}
```

点击按钮后发送消息
```java
send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String content = inputTest.getText().toString();
                Log.e("get from input", content);
                if (!"".equals(content)) {
                    try {
                        //将输入框的信息传递给msg，并标记
                        Message handleMsg = new Message();
                        handleMsg.what = 1;
                        handleMsg.obj = inputTest.getText().toString();
                        //将msg传递给发送子线程
                        mClientThread.revHandler.sendMessage(handleMsg);
                        //输入框变空
                        inputTest.setText("");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
```
子线程不断循环运行实现发送消息

```java
  Looper.prepare();
            //绑定发送线程的Handler
            //由chatroom点击事件跳转到这里发送消息
            revHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        try {
                            //发送消息
                            String content;
                            content =Name.IP+"#$#" + Name.name+"#$#" + msg.obj.toString() + "\r\n";
                            mOutputStream.write(content.getBytes("utf-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            //Looper.loop(); 让Looper开始工作，从消息队列里取消息，处理消息。
            Looper.loop();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("error","");
        }
```

## 接受消息
子线程循环接受服务端的消息

```java
      new Thread(){
                @Override
                public void run() {
                    super.run();
                    try {
                        String content = null;
                        //一个新线程持续循环的接受从服务器的消息，再发送给chatroom
                        while ((content = mBufferedReader.readLine()) != null) {

                            Log.d("get from server",content);
                            //将接受到的数据传递给msg对象，并标记
                            Message handleMsg = new Message();
                            handleMsg.what = 0;
                            handleMsg.obj = content;
                            mHandler.sendMessage(handleMsg);
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }.start();//启动
```
然后发送给主线程，主线程根据接受到的消息来更新聊天界面

```java
  mHandler = new Handler() {
            @Override
            public void handleMessage(Message handleMsg) {
                if (handleMsg.what == 0) {
                    //接受到消息后的操作
                    String content = handleMsg.obj.toString();
                    Log.d("recive", content);
                    String[] arr = content.split("#\\$#");
                    String ip = arr[0];
                    String name = arr[1];
                    String str = arr[2];
                    Log.d("get ", ip + name + str);
                    Msg msg;

                    if (ip.equals(Name.IP)) {
                        Log.e("recive from server", "it is me ");
                        msg = new Msg(name, str, Msg.TYPE_SENT);
                    } else {
                        msg = new Msg(name, str, Msg.TYPE_RECEIVED);
                    }
                    msgList.add(msg);
                    Log.e("TAG", "msg " + msgList.size());
                    adapter.notifyItemInserted(msgList.size() - 1);//当有新消息时，刷新RecyclView中的显示
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);//将RecyclerView定位到最后一行
                    inputTest.setText("");//清空输入框*/
                }
            }
        };
```

## 发送文件
首先选择文件
```java
file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建意图用于选择文件
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("*/*");
                /*if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), PICK_FILE_REQUEST_CODE);
                } else {
                    Toast.makeText(context, "无法找到文件选择器", Toast.LENGTH_SHORT).show();
                }*/
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
            }
        });
```
然后对选择到的文件的返回结果进行处理


```java

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
            if (data != null) {
                Uri uri = data.getData();
                Log.e("uri", ":" + uri);
                String filePath = "";
                // 根据Android版本的不同，获取文件路径的方式也有所不同
                // 在API 19（KitKat）及以上版本，需要通过ContentResolver查询文件的真实路径
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(this, uri)) {
                    // 处理DocumentsProvider的情况
                    ContentResolver resolver = getContentResolver();
                    InputStream inputStream = null;
                    inputStream = resolver.openInputStream(uri);

                    String filename = getFileNameFromUri(this, uri);
                    Log.e("filename: ", ":" + filename);
                    Log.e("inputstream: ", ":" + inputStream);
                    Log.e("uri: ", ":" + uri);

                    if (uri != null) {
                        handleSelectedFilePath(filename,uri);
                    }
                }
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
```

其中处理返回结果调用下列函数得到了文件名，并且将选择的文件写到了一个新建的可知道文件路径的文件（因为不能根据返回结果得到文件路径）

```java

        public String getFileNameFromUri (Context context, Uri uri){
            String fileName = null;
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    fileName = cursor.getString(columnIndex);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return fileName;
        }


        private void handleSelectedFilePath (String filename, Uri file){
            // 在这里处理获取到的文件路径
            Log.e("TAG", "Selected file name" + filename);
            // 可以进一步上传文件、读取文件内容等操作
            try {
                // 假设你已经有了一个Uri对象
                Uri sourceUri = file;

// 获取源文件的输入流
                InputStream inputStream = getContentResolver().openInputStream(sourceUri);

// 定义目标文件路径，这里以应用程序的cache目录为例
                String destFilePath = getCacheDir().getPath() + "/"+filename;
                File destFile = new File(destFilePath);

// 创建并获取目标文件的输出流
                FileOutputStream outputStream = new FileOutputStream(destFile);

// 将源文件内容复制到新文件
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

// 关闭输入输出流
                outputStream.flush();
                outputStream.close();
                inputStream.close();

// 现在你可以得到新建文件的文件地址
                String newFileAddress = destFile.getAbsolutePath();
                Log.e("getAbsolutePath", ": "+newFileAddress );
                //将输入框的信息传递给msg，并标记
                Message handleMsg = new Message();
                handleMsg.what = 1;
                handleMsg.obj =newFileAddress ;
                //"/data/data/llk/files/"
                //将msg传递给发送子线程
                fClientThread.revfHandler.sendMessage(handleMsg);
                //输入框变空
                inputTest.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
```
最后`handleSelectedFilePath函数`将包含新建文件地址发送到子线程，子线程将文件名字和文件长度和文件字节发送到服务端

```java
  Looper.prepare();
            //绑定发送线程的Handler
            //由chatroom点击事件跳转到这里发送消息
            revfHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        try {
                            mOutputStream = fSocket.getOutputStream();//输出流，客户端到管道
                            //发送消息
                            String content;
                            File file = new File(msg.obj.toString());
                            Log.e("msg.obj.toString()",": "+msg.obj.toString());
                            content = Name.IP + "#$#" + Name.name + "#$#" + file.getName() + "\r\n";
                            Log.e("content", ": "+content );
                            mOutputStream.write(content.getBytes("utf-8"));
                            //先发送了name ip 消息再发送文件内容
                            DataOutputStream fout = new DataOutputStream(fSocket.getOutputStream());
                            DataInputStream fin = new DataInputStream(new FileInputStream(file));
                            //将文件发送出去
                            // 传送文件名字
                            fout.writeUTF(file.getName());
                            Log.e("file.getname()", ": "+file.getName() );
                            fout.flush();
                            // 传送长度
                            fout.writeLong(file.length());
                            Log.e("file.length()", ": "+file.length() );
                            fout.flush();

                            System.out.println("开始传送文件...(大小：" + file.getTotalSpace() + ")");
                            // 传送文件
                            int lengthout = -1;// 读取到的文件长度
                            byte[] buffout = new byte[1024];
                            double curLength = 0;
                            // 循环读取文件，直到结束
                            while ((lengthout = fin.read(buffout)) > 0) {
                                Thread.sleep(10);
                                Log.e(" ", "lengthout: "+lengthout );
                                curLength+=lengthout;
                                Log.e("curlength / length", ": "+curLength+"/"+file.length());
                                fout.write(buffout, 0, lengthout);
                                fout.flush();
                            }
                            System.out.println("传送文件完成");
                            Thread.sleep(5000);
                            byte[] bytes = "EOF".getBytes(Charset.forName("UTF-8"));
                            fout.write(bytes);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();

                        }
                    }

                }
            };
            //Looper.loop(); 让Looper开始工作，从消息队列里取消息，处理消息。
            Looper.loop();
        }
```

## 接受文件
子线程不断接受从服务端发送过来的文件，也是接受文件名和文件长度和文件字节内容，但会在指定路径新建一个文件来接受传输过来的内容。中途会更新消息列表再去接受


```java
 new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        while (true){
                        String filename = null;
                        String content = null;
                        DataInputStream dis = new DataInputStream(fSocket.getInputStream());
                        // 从服务器传过来的东西
                        System.out.println("客户端已经链接文件服务");
                        //先传输过来名字和ip和提示文件到达消息
                        mBufferedReader = new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
                        content = mBufferedReader.readLine();
                        Log.e("content", ": "+content );
                        Message handleMsg = new Message();
                        handleMsg.what = 0;
                        handleMsg.obj = content+" position at "+" /storage/emulated/0/Download/";
                        fHandler.sendMessage(handleMsg);
                            filename = dis.readUTF();
                            Log.e("file name", "/storage/emulated/0/Download/"+filename );
                            //根据服务器发送过来的UTF格式的文件名字
                            String destFilePath ="/storage/emulated/0/Download/"+filename;
                            File file = new File(destFilePath);
                            file.createNewFile();
                            // 保存到本地的文件
                            //获取服务器传过来的文件大小
                            Log.e("new position", " "+file.getAbsolutePath() );
                            //显示完整路径
                            double totleLength = dis.readLong();
                            Log.e("length", " "+totleLength );
                            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
                            //通过dos往文件里写入内容
                            System.out.println("开始接收：" + totleLength);
                            int length = -1;
                            long recvlength = -1;
                            byte[] buff = new byte[1024];
                            double curLength = 0;
                            try {
                                while((length=dis.read(buff))>0){
                                    String str = new String(buff, StandardCharsets.UTF_8);

                                    if (str.charAt(0)=='E'&&str.charAt(1)=='O'&&str.charAt(2)=='F')
                                    {
                                        break;
                                    }
                                    dos.write(buff, 0, length);
                                    Arrays.fill(buff, (byte) 0);
                                    //往文件里写入buff
                                    Log.e("写入文件的长度: ", " "+length );
                                    curLength+=length;
                                    //System.out.println("传输进度："+(curLength/totleLength*100)+"%");
                                    System.out.println("传输进度："+(curLength/totleLength*100)+"%");
                                }
                                System.out.println("传输完成");
                            } catch (Exception ste) {
                                System.out.println("接收文件出错");
                            }

                    }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
```
中途发送到主线程根据文件消息更新消息列表
```java
 fHandler = new Handler() {
            @Override
            public void handleMessage(Message handleMsg) {
                if (handleMsg.what == 0) {
                    //接受到消息后的操作
                    String content = handleMsg.obj.toString();
                    Log.e("recive content", content);
                    String[] arr = content.split("#\\$#");
                    String ip = arr[0];
                    String name = arr[1];
                    String file = arr[2];
                    Log.e("get ", ip+file + name);
                    Msg msg;

                    if (ip.equals(Name.IP)) {
                        Log.e("recive from server", "it is me ");
                        msg = new Msg(name, file, Msg.TYPE_SENT);
                    } else {
                        msg = new Msg(name, file, Msg.TYPE_RECEIVED);
                    }
                    msgList.add(msg);
                    Log.e("TAG", "msg " + msgList.size());
                    adapter.notifyItemInserted(msgList.size() - 1);//当有新消息时，刷新RecyclView中的显示
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);//将RecyclerView定位到最后一行
                    inputTest.setText("");//清空输入框*/
                }
            }
        };
```

# 服务端
先列出各个可以监听的ip地址，然后得到运行两个子线程，分别用处理接受消息和文件并再发送给各个客户端
```java

public class MyServer {
    public static ArrayList<Socket> mSocketList = new ArrayList<>() ;
    public static ArrayList<Socket> fSocketList = new ArrayList<>() ;
    public static void main(String[] args) throws SocketException{
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress inetAddress = ia.getAddress();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    System.out.println("ServerSocket可能监听的IP地址: " + inetAddress.getHostAddress());
                }
            }
        }
        try {
            //创建服务器Socket
            ServerSocket ss = new ServerSocket(8848);
            ServerSocket fs = new ServerSocket(18848);
            while (true){
                //监听链接
                Socket s = ss.accept();
                Socket f = fs.accept();
                //打印信息
                System.out.println("ip:"+ s.getInetAddress().getHostAddress() +"加入聊天室");
                System.out.println("ip:"+ f.getInetAddress().getHostAddress()+" 客户端已经链接文件服务");
                //将s加入到线程池中
                mSocketList.add(s);
                fSocketList.add(f);
                //启动子线程
                new Thread(new ServerThread(s)).start();
                new Thread(new FileThread(f) ).start();
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("服务器已崩溃");
            e.printStackTrace();
        }
    }
}

```

## 接受消息并发送给各个客户端
接受消息，然后发给各个客户端
```java

public class ServerThread implements Runnable {
    private Socket mSocket = null;
    private BufferedReader mBufferedReader = null;
    //构造方法
    public ServerThread(Socket s)throws IOException{
        mSocket = s;
        //输入管道到服务器
        mBufferedReader = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf-8"));
    }
    public void run(){
        try {
            String content = null;
            //循环接受服务器消息，如果没有接收到，说明该客户端下线，将其从线程池中删除
            while ((content = mBufferedReader.readLine())!=null){
                System.out.println("ip:"+ mSocket.getInetAddress().getHostAddress()+":"+content);

                //循环向其他线程发送消息
                for (Iterator<Socket> it = MyServer.mSocketList.iterator();
                    it.hasNext();) {
                Socket s = it.next();
                try {
                    OutputStream os = s.getOutputStream();
                    os.write((content + "\n").getBytes("utf-8"));
                } catch (SocketException e) {
                    e.printStackTrace();
                    it.remove();
                }
            }
        }
        }catch (IOException e){
            System.out.println("接收出错");
            try {
                mSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            MyServer.mSocketList.remove(mSocket);
            System.out.println("ip:"+ mSocket.getInetAddress().getHostAddress() +"退出聊天室");
        }
    }
}
```

## 接受文件并发送给各个客户端
接受文件相关信息，在本地新建一个文件，并将接受到的字节流写入文件，然后再将文件相关信息和字节内容发送给各个客户端
```java
public class FileThread implements Runnable{
    
        private Socket fSocket = null;
        private BufferedReader fBufferedReader = null;
        //构造方法

        public FileThread(Socket f)throws IOException{
            fSocket = f;
            //输入管道到服务器
        }
 
		@Override
		public void run() {
	
			try {
                while (true) {
                System.out.println("new");   
                
              
                String filename = null;
                String content = null;
                DataInputStream dis = new DataInputStream(fSocket.getInputStream());
                // 从服务器传过来的东西
                //先传输过来名字和ip和提示文件到达消息
                BufferedReader mBufferedReader = new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
                content = mBufferedReader.readLine()+"\r\n";
                System.out.println("content"+content);
                
                 filename=dis.readUTF();
    
                    //根据客户端发送过来的UTF格式的文件名字
                    File file = new File("D:\\androidstudio\\chatroom\\server\\savefile\\"+filename);
                    System.out.println("filename"+filename);
                    if (!file.exists()) {
                        try {
                            // 新建文件
                            boolean created = file.createNewFile();
                            if (created) {
                                
                                System.out.println("成功创建文件");
                                // 文件成功创建
                            } else {
                                // 文件创建失败，可能是因为权限问题或其他原因
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // 保存到本地的文件
                    //获取服务器传过来的文件大小
                    double totleLength = dis.readLong();
                    System.out.println("file length "+totleLength);
                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
                    //通过dos往文件里写入内容
                    System.out.println("开始接收："+totleLength);
                    int length=-1;
                    byte[] buff= new byte[1024];
                    double curLength = 0;
                    
                    try {
                        while((length=dis.read(buff))>0){
                    
                            
                            String str = new String(buff, StandardCharsets.UTF_8);
                            System.err.println(str);
                            if (str.charAt(0)=='E'&&str.charAt(1)=='O'&&str.charAt(2)=='F')
                            {
                                break;
                            }
                            dos.write(buff, 0, length);
                            Arrays.fill(buff, (byte) 0); 
                            //往文件里写入buff
        
                            curLength+=length;
                            //System.out.println("传输进度："+(curLength/totleLength*100)+"%");
                            System.out.println("传输进度："+(curLength/totleLength*100)+"%");
                        }
                        System.out.println("传输完成");
                    } catch (Exception ste) {
                        System.out.println("接收文件出错"); 
                    }
                   
                    

                    for (Iterator<Socket> it = MyServer.fSocketList.iterator();it.hasNext();)
                    {  
                        Socket f= it.next();
                        try {
                            DataOutputStream fout = new DataOutputStream(f.getOutputStream());
                            DataInputStream fin = new DataInputStream(new FileInputStream(file));
                            fout.write(content.getBytes("utf-8"));
                            System.out.println("content: "+content);
                            //将文件发送出去
                            // 传送文件名字
                            fout.writeUTF(file.getName());
                            System.out.println("file.getName() "+file.getName());
                            fout.flush();
                                // 传送长度
                            fout.writeLong(file.length());
                            System.out.println("file.length() "+file.length());
                            fout.flush();
                            System.out.println("开始传送文件...(大小：" + file.getTotalSpace() + ")");
                            // 传送文件
                            int lengthout = -1;// 读取到的文件长度
                            byte[] buffout = new byte[1024];
                             curLength = 0;
                            // 循环读取文件，直到结束
                            while ((lengthout = fin.read(buffout)) > 0) {
                                Thread.sleep(4);
                                //System.out.println(" lengthout: "+lengthout );
                                curLength+=lengthout;
                                System.out.println("curlength / length: "+curLength+"/"+file.length());
                                fout.write(buffout, 0, lengthout);
                                fout.flush();
                            }
                            System.out.println("传送文件完成");
                            Thread.sleep(1000);
                            byte[] bytes = "EOF".getBytes(Charset.forName("UTF-8"));
                            fout.write(bytes);
                         }
                        catch (Exception e) {
                            System.out.println("传输意外");
                        }
                    }  
            }
        }
            catch (IOException e) {
                System.out.println("接收出错");
                try {
                  
                    fSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                MyServer.fSocketList.remove(fSocket);
                System.out.println("ip:"+ fSocket.getInetAddress().getHostAddress() +"文件传输结束");
            }

        
		} 
}
```
