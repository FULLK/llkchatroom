import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by heshu on 2017/8/29.
 */
public class MyServer {
    public static ArrayList<Socket> mSocketList = new ArrayList<>() ;
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
            while (true){
                //监听链接
                Socket s = ss.accept();
                //打印信息
                System.out.println("ip:"+ s.getInetAddress().getHostAddress() +"加入聊天室");
                //将s加入到线程池中
                mSocketList.add(s);
                //启动子线程
                new Thread(new ServerThread(s)).start();
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("服务器已崩溃");
            e.printStackTrace();

        }
    }
}
