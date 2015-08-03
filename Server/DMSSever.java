package DMS.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * DMS服务端
 * Created by JEWELZ on 15/5/15.
 */
public class DMSSever {
    //用于与客户端连接的SeverSocket
    private ServerSocket server;

    //线程池,用于控制服务端线程数量并重用线程
    private ExecutorService threadPool;

    //用于保存所有客户端发来的配对日志
    private File serverLogRecFile;

    //双向缓冲队列,用于保存所有客户端发来的配对日志,这里缓冲是为了提高效率
    private BlockingDeque<String> logQueue;

    public DMSSever() {
        try {
            //初始化ServerSocket
            /*
             * 初始化时要求我们传入一个整数,这个整数
			 * 表示端口号,客户端就是通过这个端口号
			 * 连接到服务端的
			 */
            server = new ServerSocket(8090);

            //初始化用于保存所有客户端发送的配对日志的文件
            serverLogRecFile = new File("./src/DMS/severLogRec.txt");

            //!!!初始化线程池
            threadPool = Executors.newFixedThreadPool(50);

            //!!!初始化双缓冲队列
            logQueue = new LinkedBlockingDeque<>();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器开始工作的方法
     */
    public void start() {
        try {
            /*
             * 启动用于向文件中写入日志的线程
			 */
            WriteLogHandler writeLogHandler = new WriteLogHandler();
            Thread t = new Thread(writeLogHandler);
            t.start();
                /*
                 * Socket accept()
				 * 该方法是一个阻塞方法,用于等待客户端的连接
				 * 一旦一个客户端连接上,该方法会返回与该客户
				 * 端通信的Socket
				 */
            //死循环的目的是一直监听不同客户端的连接
            while (true) {
                System.out.println("Waiting for Client");
                Socket socket = server.accept();
                System.out.println("Connected to Client Succesfully");
                /*
                 * 当一个客户端连接后,启动一个线程,将该客户端
				 * 的Socket传入,使该线程与该客户端通信
				 */
                Runnable clientHandler = new ClientHandler(socket);
//				Thread t = new Thread(clientHandler);
//				t.start();
                //将任务指派给线程池
                threadPool.execute(clientHandler);
            }

        } catch (Exception e) {
        } finally {

        }
    }

    public static void main(String[] args) {
        DMSSever server = new DMSSever();
        server.start();
    }

    /**
     * 该线程的作用是与给定的客户端Socket进行通信
     *
     * @author Administrator
     */
    class ClientHandler implements Runnable {
        //当前线程用于交流的指定客户端的Socket
        private Socket socket;

        /**
         * 创建线程体时将交互的Socket传入
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            PrintWriter pw = null;
            try {
                /*
                 * 通过Socket获取输出流,用于将信息发送给
				 * 客户端
				 */
                OutputStream out = socket.getOutputStream();
                OutputStreamWriter osw
                        = new OutputStreamWriter(out, "UTF-8");
                pw = new PrintWriter(osw, true);

				/*
                 * 通过连接上的客户端的Socket获取输入流
				 * 来读取客户端发送过来的信息
				 */
                InputStream in = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(in, "UTF-8");
                //包装为缓冲字符输入流,可以按行读取字符串
                BufferedReader br = new BufferedReader(isr);
                String message = null;

                while ((message = br.readLine()) != null) {
                    /*
                     * 将客户端发送过来的每一条配对信息
					 * 保存起来.(放入缓冲队列,等待写入文件)
					 */
                    logQueue.offer(message, 5, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
            } finally {
                /*
                 * linux的客户端若断开连接,服务端会读取到null
				 * windows的客户端断开连接,服务端会抛出异常
				 * 所以finally是我们最后做处理的最佳地点
				 */
                System.out.println("Client LogOut");
                    /*
                     * 不同分别关闭输入流与输出流
					 * 关闭Socket即可,因为这两个流都是从Socket
					 * 获取的.就好比打电话,我们最终挂电话就自然
					 * 断开了麦克风和听筒一样
					 */
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 该线程用于周期性从缓冲队列中取出日志信息,并
     * 写入文件中
     *
     * @author Administrator
     */
    class WriteLogHandler implements Runnable {
        public void run() {
            PrintWriter pw = null;
            try {
                //创建文件输出流是为追加写操作.
                //创建用于向server-logrec.txt文件中写数据的流
                pw = new PrintWriter(new FileOutputStream(serverLogRecFile, true));
                /*
                 * 循环从队列中取出每一个日志,并写入文件
				 * 若队列中没有元素了,可以休息一会.
				 */
                while (true) {
                    if (logQueue.size() == 0) {
                        pw.flush();
//                        Thread.sleep(1000);
                    } else {
                        //若队列中还有日志,就拿来写
                        pw.println(logQueue.poll());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }
}
