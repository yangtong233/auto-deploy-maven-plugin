package org.az;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by yangtong on 2024/6/15 11:30:39
 */
@Mojo(name = "auto-deploy", defaultPhase = LifecyclePhase.PACKAGE)
public class AutoDeployMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project}")
    private MavenProject project;
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
                session -> {
                    if (beforeCommands != null) {
                        //执行命令
                        for (String command : beforeCommands) {
                            if (new ExecCommand(session, "cd " + remotePath, command).doCommand() != 0) {
                                throw new ExecFailException();
                            }
                            System.out.println();
                        }
                    }
                },
                //上传
                session -> {
                    //上传程序包
                    try {
                        //sftp channel用于传输文件
                        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                        sftpChannel.connect();

                        sftpChannel.put(jarPackagePath, remoteFile, new ConsoleProgressMonitor(log));
                        //上传完成后关闭sftp channel
                        sftpChannel.disconnect();
                    } catch (JSchException | SftpException e) {
                        throw new RuntimeException(e);
                    }
                },
                //后置命令
                session -> {
                    if (afterCommands != null) {
                        for (String command : afterCommands) {
                            if (new ExecCommand(session, "cd " + remotePath, command).doCommand() != 0) {
                                throw new ExecFailException();
                            }
                            System.out.println();
                        }
                    }
                },
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
}