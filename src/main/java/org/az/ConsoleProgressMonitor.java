package org.az;

import com.jcraft.jsch.SftpProgressMonitor;
import org.apache.maven.plugin.logging.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * created by yangtong on 2024/6/14 上午10:49
 * <p>
 * 控制台进度条
 */
public class ConsoleProgressMonitor implements SftpProgressMonitor {
    private long transferred;
    private long total = 0;
    private final Log log;

    public ConsoleProgressMonitor(Log log) {
        this.log = log;
    }

    /**
     * 进行sftp操作的初始化方法
     *
     * @param op   sftp类型，0上传，1下载
     * @param src  源文件
     * @param dest 目标文件
     * @param max  文件字节数
     */
    @Override
    public void init(int op, String src, String dest, long max) {
        this.total = max;
        System.out.println("\u001b[35m开始上传程序包...\u001b[0m");
        System.out.println("程序包路径: " + src);
        System.out.println("上传路径: " + dest);

        BigDecimal _max = BigDecimal.valueOf(max);
        //将max字节数变成mb单位
        _max = _max.divide(BigDecimal.valueOf(1024 * 1024), 4, RoundingMode.HALF_UP);
        System.out.println("上传文件大小: \u001b[34m" + _max + " MB\u001b[0m");
    }

    @Override
    public boolean count(long count) {
        //计算已上传的字节数
        transferred += count;
        //计算上传进度
        int progress = (int) (transferred * 100 / total);
        //在控制台打印进度条
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            //每上传2%打印一个"#"
            if (i < progress / 2) {
                bar.append("\u001b[36m#\u001b[0m");
            }
            //未上传的部分打印"."
            else {
                bar.append(".");
            }
        }
        bar.append("]   ").append(progress).append("%  ").append(transferred).append("/").append(total);
        //覆盖之前的进度条，让进度条始终在一行显示
        System.out.print("\r" + bar);
        return true;
    }

    @Override
    public void end() {
        System.out.println();
        System.out.println("\u001b[33m上传成功!!!\u001b[0m");
        System.out.println();
    }
}
