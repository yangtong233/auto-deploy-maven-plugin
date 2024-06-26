package org.az;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * created by yangtong on 2024/6/15 12:17:12
 */
public class AutoDeployMojoTest {

    private static final String host = "192.168.1.220";
    private static final Integer port = 22;
    private static final String user = "httech";
    private static final String password = "Dev62628816";
    private static final String remotePath = "/home/httech";

    public static void main(String[] args) {
        Log log = new SystemStreamLog();
        String jarPackagePath = "D:\\test.jar";
        log.info("程序包路径" + jarPackagePath);

        String remoteFile = remotePath + "/test.jar";
        log.info("将被上传到" + remoteFile);

        ServerConnection server = new ServerConnection(host, port, user, password, log);
        log.info("所有命令都将在\u001b[1m[" + remotePath + "]\u001b[0m目录下执行" + (!user.equals("root") ? "" : ", 并且以sudo模式执行"));

        String[] beforeCommands = new String[]{"cd /home", "touch 223.md"};
        String[] afterCommands = new String[]{"ls -l", "sudo ps -ef|grep redis"};

        server.doConnect(
                //前置命令
                session -> {
                    //执行命令
                    for (String command : beforeCommands) {
                        if (new ExecCommand(session, "cd " + remotePath, command).doCommand() != 0) {
                            throw new ExecFailException();
                        }
                        System.out.println();
                    }
                },
                //上传文件
                session -> {
                    //上传文件
                    try {
                        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                        sftpChannel.connect();

                        sftpChannel.put(jarPackagePath, remoteFile, new ConsoleProgressMonitor());
                        sftpChannel.disconnect();
                    } catch (JSchException | SftpException e) {
                        throw new RuntimeException(e);
                    }
                },
                //执行后置命令
                session -> {
                    for (String command : afterCommands) {
                        if (new ExecCommand(session, "cd " + remotePath, command).doCommand() != 0) {
                            throw new ExecFailException();
                        }
                        System.out.println();
                    }
                },
                //收尾
                session -> {
                    log.info("自动部署成功，即将退出登录");
                });
    }
}
