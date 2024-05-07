import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

