package org.az;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * created by yangtong on 2024/6/15 11:30:39
 */
@Mojo(name = "auto-deploy", defaultPhase = LifecyclePhase.PACKAGE)
public class AutoDeployMojo extends AbstractMojo {

    /**
     * 命令分隔符
     */
    private final static String SEPARATOR = ";";

    /**
     * 对应maven项目的pom.xml文件
     */
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * 用于获取mvn原始命令
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * 服务器地址
     */
    @Parameter(required = true)
    private String host;
    /**
     * 服务器端口，默认22
     */
    @Parameter(defaultValue = "22")
    private int port;
    /**
     * 服务器登录账号
     */
    @Parameter(required = true)
    private String user;
    /**
     * 服务器登录密码
     */
    @Parameter(required = true)
    private String password;
    /**
     * 上传导服务器的地址文件夹
     */
    @Parameter(required = true)
    private String remotePath;

    /**
     * 后置命令，上传jar包完毕后执行的命令
     */
    @Parameter
    private String[] beforeCommands;

    /**
     * 后置命令，上传jar包完毕后执行的命令
     */
    @Parameter
    private String[] afterCommands;

    @Override
    public void execute() {
        //只有在auto-deploy环境下打包，才能使用该插件
        List<String> activeProfiles = session.getRequest().getActiveProfiles();
        if (!activeProfiles.contains("auto-deploy")) {
            return;
        }

        //日志
        Log log = getLog();
        //本地程序包
        String jarPackagePath = parseJarPackagePath();
        log.info("\u001b[36m本地程序包路径：\u001b[0m" + jarPackagePath);
        //远程服务器的文件路径
        String remoteFile = parseRemotePath();
        log.info("\u001b[36m将被上传到：\u001b[0m" + host + " -> " + remoteFile);
        //创建服务器连接
        ServerConnection server = new ServerConnection(host, port, user, password, log);
        log.info("所有命令都将在\u001b[1m[" + remotePath + "]\u001b[0m目录下执行");
        //执行具体操作
        server.doConnect(
                //前置命令
                parseCommand(beforeCommands),
                //上传
                session -> {
                    //上传程序包
                    try {
                        //sftp channel用于传输文件
                        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                        sftpChannel.connect();

                        sftpChannel.put(jarPackagePath, remoteFile, new ConsoleProgressMonitor());
                        //上传完成后关闭sftp channel
                        sftpChannel.disconnect();
                    } catch (JSchException | SftpException e) {
                        throw new RuntimeException(e);
                    }
                },
                //后置命令
                parseCommand(afterCommands),
                //收尾命令
                session -> log.info("自动部署成功，即将退出登录...")
        );
    }

    /**
     * 解析获取本地要上传的jar包的绝对路径
     */
    private String parseJarPackagePath() {
        Build build = project.getBuild();
        String packaging = project.getPackaging();
        String directory = build.getDirectory();
        String finalName = build.getFinalName();
        return directory + File.separator + finalName + "." + packaging;
    }

    /**
     * 解析得到远程服务器的目录
     */
    private String parseRemotePath() {
        //远程服务器上的文件路径
        if (!remotePath.endsWith("/")) {
            remotePath = remotePath + "/";
        }

        Build build = project.getBuild();
        String packaging = project.getPackaging();
        String finalName = build.getFinalName();

        return remotePath + finalName + "." + packaging;
    }

    /**
     * 解析命令数组，并返回SessionTask对象
     * 对于每条命令，可能包含多个子命令，子命令之间用分号分隔
     *
     * @param commandArr 命令数组
     * @return SessionTask对象
     */
    private SessionTask parseCommand(String[] commandArr) {
        return session -> {
            if (commandArr != null) {
                for (String commands : commandArr) {
                    //可能commands是以分号分隔的多个命令，需要把所有命令拆分出来依次执行
                    if (commands != null) {
                        for (String command : commands.split(SEPARATOR)) {
                            if (command != null) {
                                command = command.trim();
                                int exitCode = new ExecCommand(session, "cd " + remotePath, command).doCommand();
                                if (exitCode != 0) {
                                    throw new ExecFailException();
                                }
                                System.out.println("命令结束状态码：" + exitCode);
                                System.out.println();
                            }
                        }
                    }
                }
            }
        };
    }
}
