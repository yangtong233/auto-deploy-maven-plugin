package org.az;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * created by yangtong on 2024/6/15 11:32:00
 * 执行linux命令，每个ExecCommand对应一个命令
 */
public class ExecCommand {

    private final Session session;
    private final String firstCommand;
    private final String command;
    private static final String Exec = "exec";
    //加载环境变量
    private static final String initCommand = "source /etc/environment;source /etc/profile;source ~/.bashrc;source ~/.profile";

    /**
     * 构造函数
     *
     * @param session 一次linux会话
     * @param firstCommand 第一个命令，一般是进入指定目录
     * @param command 后续命令
     */
    public ExecCommand(Session session, String firstCommand, String command) {
        this.command = command;
        this.firstCommand = firstCommand;
        this.session = session;
    }

    /**
     * 执行命令
     *
     * @return 命令执行结果，0为成功，其他为失败
     */
    public int doCommand() {
        ChannelExec execChannel = null;
        int lineCount = 0;
        System.out.println("\u001b[32m执行命令：" + command + "\u001b[0m");
        try {
            execChannel = (ChannelExec) session.openChannel(Exec);
            execChannel.setCommand(initCommand + " && " + firstCommand + " && " + command);
            execChannel.setErrStream(new OutputStream() {
                @Override
                public void write(int b) {
                    System.out.print("\u001b[31m" + (char) b + "\u001b[0m");
                }
            });
            execChannel.connect();

            //执行完毕后得到正确情况下的执行结果
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(execChannel.getInputStream()));
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lineCount++;
                }
                System.out.println(line);
            }
            stdoutReader.close();
        } catch (JSchException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (execChannel != null) {
                execChannel.disconnect();
            }
        }
        int exitCode = execChannel.getExitStatus();
        if (lineCount == 0 && exitCode == 0) {
            System.out.println("该命令无任何输出");
        }
        return exitCode;
    }

}
