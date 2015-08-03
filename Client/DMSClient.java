package DMS.Client;

import DMS.BO.LogData;
import DMS.BO.LogRec;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * DMS的客户端,用来将wtmpx文件中的日志
 * 进行解析,配对,发送至服务端
 * Created by JEWELZ on 14/5/15.
 */
public class DMSClient {
    //该属性用于描述系统日志文件wtmpx(二进制文件)
    private File logFile;

    //该属性用于保存从wtmpx解析出来的日志(文本文件)
    private File textLogFile;

    //一次从wtmpx中解析的日志条目数
    private int batch;

    //该文件保存上次读取到的位置,以便继续读取
    private File lastPositionFile;

    //该文件用于保存为匹配成对的日志文件
    private File loginLogFile;

    //该文件用于保存所有配对成功的日志文件
    private File logRecFile;

    /**
     * 构造方法
     */
    public DMSClient() {
        try {
            //一次读10条
            batch = 10;

            logFile = new File("./src/DMS/wtmpx");

            //保存解析后的日志的文件
            textLogFile = new File("./src/DMS/logText.txt");

            //保存上次读取wtmpx文件的位置,以便下次继续读取
            lastPositionFile = new File("./src/DMS/last-position.dat");

            //初始化保存所有未配对成功的日志文件
            loginLogFile = new File("./src/DMS/logInList.txt");

            //初始化保存所有配对成功的日志文件
            logRecFile = new File("./src/DMS/logRec.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据上次读取的位置判断wtmpx文件中是否还有内容可读取,若有返回开始位置,若没有返回 -1
     */

    public long hasLogs() {
        /**
         * 1.判断记录上次读取到的位置的文件是否存在
         *  1.1 若不存在,这说明没有读取过文件,从头开始即可 (初始值为0)
         *  1.2 若存在,则读取上次的位置,并判断是否小于wtmpx文件的总长度,若是则说明还有数据可读
         */
        long position = 0;
        //1.2
        if (lastPositionFile.exists()) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(lastPositionFile, "r");
                position = raf.readLong();
            } catch (Exception e) {
            } finally {
                try {
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                }
            }
        }
        //判断是否还有数据可读
        if (position >= logFile.length()) {
            position = -1;
        }
        return position;
    }

    /**
     * 将给定的位置保存到last-position.dat文件中
     */
    public void saveLastPosition(long position) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(
                    lastPositionFile, "rw");
            raf.writeLong(position);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 批量从wtmpx文件中读取日志信息并解析位文本格式,写入到log.txt文件中
     * 解析完毕返回true
     */
    public boolean readNextLogs() {
        /**
         * 实现步骤:
         * 1.必要的判断
         *  1.1 wtmpx文件要存在,不存在返回false
         *  1.2 wtmpx文件中是否还有可读的数据
         * 2.解析
         *  2.1 创建RandomAccessFile(用它比较方便) 用于读取wtmpx文件
         *  2.2 创建缓冲字符输出流向log.txt中写文件
         *  2.3 循环batch次
         *  2.4 将RAF的游标移动到一条数据的起始位置
         *  2.5 按照LogData中定义的每隔数据的位置及长度将每一条数据中的5项内容读取出来
         *      并存入一个LogData实例中
         *  2.6 将每个实例传入到集合中
         *  2.7 将所有日志按行写入log.txt文件
         */

        //1.1
        if(!logFile.exists()){
            return false;
        }
        //1.2
        long position = hasLogs();
        if(position < 0){
            return false;
        }

        //2
        RandomAccessFile raf = null;
        PrintWriter pw = null;
        try {

            raf = new RandomAccessFile(logFile, "r");
            pw = new PrintWriter(textLogFile);
            //先设置起始位置
            raf.seek(position);
            System.out.println(position);
            List<LogData> list = new ArrayList<>();
            for (int i = 0; i < batch && raf.getFilePointer() < logFile.length(); i++) {
                System.out.println(i);
                //解析user,UNIX系统日志中都是以iso8859-1编码的
                byte[] userData = new byte[LogData.USER_LENGHT];
                raf.read(userData);
                String user = new String(userData, "iso8859-1").trim();

                //解析pid
                raf.seek(position + LogData.PID_OFFSET);
                int pid = raf.readInt();
                //解析type
                raf.seek(position + LogData.TYPE_OFFSET);
                short type = raf.readShort();

                //解析time
                raf.seek(position + LogData.TIME_OFFSET);
                int time = raf.readInt();

                //解析host
                byte[] hostData = new byte[LogData.HOST_LENGTH];
                raf.seek(position + LogData.HOST_OFFSET);
                raf.read(hostData);
                String host = new String(hostData, "iso8859-1").trim();

                //添加到list中
                LogData log
                        = new LogData(user, pid, type, time, host);
                list.add(log);

                //更新last-position.dat的值
                //输出每次解析的内容,便于调试
                System.out.println("---------->" + log);
                //先计算当前一条日志在wtmpx文件中的起始位置
                position = position + LogData.LOG_LENGTH;
                raf.seek(position);
            }
            //1:将解析后的日志写入textLogFile中
            //2:保存当前读取的位置,以便下次读取
            for (LogData log : list) {
                pw.println(log);
            }
            saveLastPosition(raf.getFilePointer());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return true;
    }

    /**
     * 从给定的文件中读取所有的日志,并存入集合后将
     * 该集合返回
     *
     * @param file 保存日志的文件
     * @return 返回存有所有日志的集合
     */
    public List<LogData> loadLogData(File file) {
        List<LogData> list
                = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = null;
            while ((line = br.readLine()) != null) {
                LogData log = new LogData(line);
                list.add(log);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    /**
     * 根据给定的登入日志与待配对的集合
     * 找出与之配对的登出日志,并返回配对对象
     * 若返回值为null则没有匹配的对象
     */

    public LogRec matchLogout(
            LogData login,List<LogData> list){
        //查看参数是否为登入日志
        if(login.getType()!=LogData.TYPE_LOGIN){
            return null;
        }
        for(LogData log : list){
            //查看log是否为登出日志
            if(log.getType()!=LogData.TYPE_LOGOUT){
                continue;
            }
            //查看用户名是否一致
            if(!login.getUser().equals(log.getUser())){
                continue;
            }
            //查看pid是否一致
            if(login.getPid()!=log.getPid()){
                continue;
            }
            //查看host是否一致
            if(!login.getHost().equals(log.getHost())){
                continue;
            }
            //都匹配,这创建LogRec对象并返回
            LogRec logRec = new LogRec(login,log);
            return logRec;
        }
        //for循环退出了,说明没有找到对应的登出(配对失败)
        return null;
    }

    /**
     * 将给定的集合中的元素顺序调用toString,并将返回的
     * 字符串按行写入给定文件中
     */
    public void saveList(File file, List list) {
        PrintWriter pw = null;

        try {
            pw = new PrintWriter(file);
            for (Object o : list) {
                pw.println(o);
            }
        } catch (Exception e) {
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    /**
     * 匹配日志
     */
    public boolean matchLogs(){
		/*
		 * 首先判断:
		 *   1:检查logs.txt文件是否存在(解析后的文件)
		 *     没有的话没法配对
		 *
		 * 将logs.txt文件中每行日志读取回来,并转换为
		 * 若干个LogData实例,并存入一个集合中等待配对
		 *
		 * 查看上次是否存在没有匹配成对的日志,有则将这些
		 * 日志转换为LogData对象,并也放入集合中等待配对
		 *
		 * 遍历集合,找出所有可以配对的日志(登入与登出)
		 * 配对条件:用户名,PID,HOST都要相同,然后
		 *         TYPE一个是7,一个是8的
		 * 将一组配对的日志出存入LogRec对象中,并将LogRec
		 * 对象存入一个集合中.
		 * 将所有没配对成功的日志存入另一个集合中
		 *
		 * 将配对成功的集合中的所有日志写入一个文件logrec.txt
		 * 将没有配对成功的日志写入另一个文件login.txt
		 *
		 * 将第一步生成的log.txt文件删除
		 *
		 */

        if(!textLogFile.exists()){
            System.out.println("log.txt文件不存在");
            return false;
        }

        //从文件中取出解析出的10条数据
        //创建用于保存待配对日志的集合
        List<LogData> list = new ArrayList<LogData>();

        //创建用于保存配对成功的日志的集合
        List<LogRec> matched = new ArrayList<LogRec>();

        //将log.txt文件中所有元素添加到list中
        list.addAll(loadLogData(textLogFile));

        //检查保存上次未匹配成功的日志文件是否存在
        if(loginLogFile.exists()){
            //将未匹配的也存入集合等待配对
            list.addAll(loadLogData(loginLogFile));
        }

        //再创建一个集合,用于保存所有没配对成功的日志
        List<LogData> loginList
                = new ArrayList<LogData>();
		/*
		 * 循环所有待配对日志,进行配对工作
		 */
        for(LogData log : list){
            //若这条日志不是登入,就忽略,因为我们是根据登入找登出
            if(log.getType()!=LogData.TYPE_LOGIN){
                continue;
            }
            //准备配对
            LogRec logRec = matchLogout(log, list);

            if(logRec == null){
                //没有配对成功
                loginList.add(log);
            }else{
                //配对成功
                matched.add(logRec);
            }
        }

        //分别将配对成功与失败的日志写入不同文件!!!!!!!!
        try {
            //写入没有配对成功的日志
            saveList(loginLogFile,loginList);

            //写入配对成功的日志
            saveList(logRecFile,matched);

            //将第一步解析出的保存10条日志的文件删除
            textLogFile.delete();

            //第二步操作成功
            return true;
        } catch (Exception e) {
        }
        return false;
    }


    /**
     * 将配对的数据发送至服务端保存
     */
    public boolean sendLogRecToServer() {
        /*
         * 1:必要的判断
		 *   1.1:检查保存配对成功的日志文件是否存在
		 * 2:将logrec.txt文件中每一行数据发送给服务器端
		 *   2.1:创建Socket与服务端连接
		 *   2.2:通过Socket获取输出流,并包装为缓冲字符
		 *       输出流,用于将数据发送给服务端
		 *   2.3:创建读取logrec.txt文件的缓冲字符输入流
		 *       用于按行读取每一对配对日志
		 *   2.4:顺序读取文件中的每一行日志,并发送给服务器
		 *       端
		 * 3:断开连接,释放资源
		 */

        if (!logRecFile.exists())
            return false;
        Socket socket = null;
        BufferedReader br = null;
        try {
            socket = new Socket("localhost", 8090);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            PrintWriter pw = new PrintWriter(osw);
            FileInputStream fis = new FileInputStream(logRecFile);
            InputStreamReader isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);

            String line = null;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
//            logRecFile.delete();
            pw.flush();//将缓冲区中的数据全部写出
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    /**
     * 客户端开始工作的方法
     */
    public void start() {
        /**
         * 完成收集数据的工作,分三步
         * 1.从wtmpx文件中取出一组数据(batch决定数量),并解析成字符串后保存
         * 2.将解析厚的日志按照登入登出配对
         * 3.将配对的数据发送至服务器
         */

        //1.
        if (readNextLogs()) {
            //2.
            matchLogs();
            //3.
            sendLogRecToServer();
        }
    }

    public static void main(String[] args) {
        DMSClient dmsClient = new DMSClient();

        dmsClient.start();
    }
}
