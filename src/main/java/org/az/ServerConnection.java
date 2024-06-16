package org.az;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.maven.plugin.logging.Log;

import java.util.Properties;

/**
 * created by yangtong on 2024/6/15 11:48:31
 * 连接服务器
 */
public final class ServerConnection {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final Log log;
    private Session session;

    public ServerConnection(String host, int port, String user, String password, Log log) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.log = log;
    }

    /**
     * 登录服务器
     */
    private void before() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            // 避免要求密钥确认
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            log.info(host + "登录成功， 即将开始部署...");
            this.session = session;
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    public void doConnect(SessionTask... tasks) {
        if (tasks != null) {
            before();
            for (SessionTask task : tasks) {
                try {
                    task.doTask(session);
                } catch (ExecFailException e) {
                    System.out.println("\u001b[31m执行命令失败，即将退出登录...\u001b[0m");
                    break;
                }
            }
            after();
        }
    }

    private void after() {
        //关闭会话
        session.disconnect();
        log.info("退出登录成功!");
    }

}
