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
    //第一个命令，一般是进入指定目录
    private final String firstCommand;
    //真正要执行的命令，该命令在指定目录下执行
    private final String command;
    //root账号
    private final boolean IS_ROOT;

    private static final String Exec = "exec";
    //命令间分隔符
    private final static String SPLIT = " && ";
    //加载环境变量
    private static final String INIT = "source /etc/profile;source ~/.bashrc;source ~/.profile";
    //以sudo模式执行
    private final static String SUDO = "sudo -S -p '' ";


    /**
     * 构造函数
     *
     * @param session      一次linux会话
     * @param firstCommand 第一个命令，一般是进入指定目录
     * @param command      后续命令
     */
    public ExecCommand(Session session, String firstCommand, String command) {
        this.command = command;
        this.firstCommand = firstCommand;
        this.session = session;
        IS_ROOT = session.getUserName().equals("root");
    }

    /**
     * 以非sudo模式执行命令
     *
     * @return 命令执行结果，0为成功，其他为失败
     */
    public int doCommand() {
        return doCommand(null);
    }

    /**
     * 执行命令
     *
     * @param sudo sudo密码，为null表示不以sudo模式执行
     * @return 命令执行结果，0为成功，其他为失败
     */
    public int doCommand(String sudo) {
        ChannelExec execChannel = null;
        int lineCount = 0;
        try {
            execChannel = (ChannelExec) session.openChannel(Exec);
            execChannel.setCommand(getCommand(sudo));
            execChannel.setErrStream(new OutputStream() {
                @Override
                public void write(int b) {
                    System.out.print("\u001b[31m" + (char) b + "\u001b[0m");
                }
            });
            execChannel.connect();

            //如果不是root账号，且命令不是cd命令，则需要输入sudo密码
            if (sudo != null && !command.startsWith("cd ") && !IS_ROOT) {
                OutputStream out = execChannel.getOutputStream();
                out.write((sudo + "\n").getBytes());
                out.flush();
                out.close();
            }

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

            //确保execChannel被关闭了
            while (!execChannel.isClosed()) {
                sleep10();
            }
            int exitCode = execChannel.getExitStatus();
            if (lineCount == 0 && exitCode == 0) {
                System.out.println("该命令无任何输出");
            }
            return exitCode;
        } catch (JSchException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (execChannel != null) {
                execChannel.disconnect();
            }
        }
    }

    /**
     * 线程睡眠10ms
     */
    private void sleep10() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取最终执行的命令
     *
     * @param sudo sudo密码，为null表示不以sudo模式执行
     * @return 最终执行的命令
     */
    private String getCommand(String sudo) {
        boolean isSudo = false;
        StringBuilder finalCommand = new StringBuilder().append(INIT).append(SPLIT).append(firstCommand).append(SPLIT);
        //如果sudo为空，则以普通模式执行命令
        if (sudo == null) {
            finalCommand.append(command);
        }
        //如果command以"sudo "开头，则以sudo模式执行
        else if (command.startsWith("sudo ")) {
            isSudo = true;
            finalCommand.append(SUDO).append(command.replace("sudo ", ""));
        }
        //进入这个分支，说明sudo不为空，并且command不以"sudo "开头，则以sudo模式执行
        else {
            //cd命令不能结合sudo使用
            if (!command.startsWith("cd ")) {
                finalCommand.append(SUDO);
                isSudo = true;
            }
            finalCommand.append(command);
        }

        System.out.println("\u001b[32m执行命令" + (isSudo ? "(sudo模式)" : "") + "：" + command + "\u001b[0m");
        return finalCommand.toString();
    }

}
