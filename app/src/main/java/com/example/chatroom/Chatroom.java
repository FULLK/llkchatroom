package com.example.chatroom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Chatroom extends AppCompatActivity {
    private List<Msg> msgList = new ArrayList<>();
    private TextView inputTest;
    private Button send;
    private Button file;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private ClientThread mClientThread;
    private FileThread fClientThread;
    private Handler mHandler;
    private Handler fHandler;
    private static final int PICK_FILE_REQUEST_CODE = 100; // 自定义请求码
    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        //各种赋值初始化
        inputTest = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        file = (Button) findViewById(R.id.file);
        msgRecyclerView = (RecyclerView) findViewById(R.id.msg_recycler_view);
        //创建一个LinearLayoutManager（线性布局）对象将它设置到RecyclerView
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutmanager);
        //调用构造方法创造实例,参数消息集合
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);

        //重写Handler类handleMessage方法，并将对象赋给mHandler
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

        //发送按钮监听器
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
        //创建实例，将mHandler作为参数传递给mClientThread实例
        mClientThread = new ClientThread(mHandler);
        fClientThread = new FileThread(fHandler,this);
        //启动子线程
        new Thread(mClientThread).start();
        new Thread(fClientThread).start();
    }

    @SuppressLint("Range")
    @Override
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

                //将msg传递给发送子线程
                fClientThread.revfHandler.sendMessage(handleMsg);
                //输入框变空
                inputTest.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
