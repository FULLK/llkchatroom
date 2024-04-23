package com.example.chatroom;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileThread implements  Runnable {

    // 文件传输端口18848
    private Socket fSocket;
    private BufferedReader mBufferedReader = null;
    private OutputStream mOutputStream = null;
    private Handler fHandler;
    private Context context;
    public Handler revfHandler;

    public FileThread(Handler handler,Context con) {
        fHandler = handler;
        this.context = con.getApplicationContext();
    }


    @Override
    public void run() {
        try {
            fSocket = new Socket("192.168.115.253", 18848);
            Socket socket = null;
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
                            Log.e("file name", " "+filename );
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
