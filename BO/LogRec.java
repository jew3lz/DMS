package DMS.BO;

/**
 * 该类用于表示一个配对的日志
 * 配对的日志含有两条日志,一条登入,一条登出
 * Created by JEWELZ on 14/5/15.
 */
public class LogRec {
    private LogData logIn;
    private LogData logOut;

    public LogRec(LogData logIn, LogData logOut) {
        this.logIn = logIn;
        this.logOut = logOut;
    }

    @Override
    public String toString() {
        return logIn + "|" + logOut;
    }
}
