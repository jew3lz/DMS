package DMS.BO;

/**
 * LogData用于描述Unix系统日志文件WTMPX中的内容
 * 每一个实例代表一条数据
 * 0-32位是USER段(String类型) 68-72位是PID(int) 72-74位是TYPE(short)
 *  80-84位是TIME(int) 114-末位是HOST(String)
 * Created by JEWELZ on 14/5/15.
 */
public class LogData {
    /**
     * 一条日志信息在WTMPX文件中占用的字节量
     */
    public static final int LOG_LENGTH = 372;

    /**
     * 每条日志中记录的用户名
     * 及用户名在日志文件中的偏移量
     * 和用户名在日志文件中所占用的字节量
     */
    private String user;
    public static final int USER_OFFSET = 0;
    public static final int USER_LENGHT = 32;
    //读int不需要写长度.readInt()即可,short同理.
    private int pid;
    public static final int PID_OFFSET = 68;
    //登入的值是7,登出的值是8.此处设为常量
    private short type;
    public static final int TYPE_OFFSET = 72;
    public static final int TYPE_LOGIN = 7;
    public static final int TYPE_LOGOUT = 8;

    private int time;
    public static final int TIME_OFFSET = 80;

    private String host;
    public static final int HOST_OFFSET = 114;
    public static final int HOST_LENGTH = 258;

    public LogData(String user, int pid, short type, int time, String host) {
        this.user = user;
        this.pid = pid;
        this.type = type;
        this.time = time;
        this.host = host;
    }

    public LogData(String line) {
        String[] strs = line.split(",");
        this.user = strs[0];
        this.pid = Integer.parseInt(strs[1]);
        this.type = Short.parseShort(strs[2]);
        this.time = Integer.parseInt(strs[3]);
        this.host = strs[4];
    }

    @Override
    public String toString() {
        return user + "," + pid + "," + type + "," + time + "," + host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
