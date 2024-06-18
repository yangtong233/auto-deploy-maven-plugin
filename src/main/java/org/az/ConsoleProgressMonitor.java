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
    //总字节数
    private long total = 0;
    //累计传输总字节数
    private long transferred;
    //每次上传开始时间
    private long startTime;
    //累计时间，累计达到1秒则计算上传速率，然后置为0
    private long elapsedTime;
    //过去一秒累计上传的字节数，方便计算上传速率
    private long bytesPerSecond;

    private StringBuilder rate;

    /**
     * 进行sftp操作的初始化方法，并打印初始化信息
     *
     * @param op   sftp类型，0上传，1下载
     * @param src  源文件
     * @param dest 目标文件
     * @param max  文件字节数
     */
    @Override
    public void init(int op, String src, String dest, long max) {
        this.total = max;
        this.startTime = System.currentTimeMillis();
        System.out.println("\u001b[35m开始上传程序包...\u001b[0m");
        System.out.println("程序包路径: " + src);
        System.out.println("上传路径: " + dest);
        System.out.println("上传文件大小: \u001b[34m" + formatSize(max) + "\u001b[0m");
    }

    @Override
    public boolean count(long count) {
        //累加速率计算的3个属性
        elapsedTime += (System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        bytesPerSecond += count;
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
        //计算上传速率
        if (elapsedTime >= 1000 || transferred == total) {
            rate = new StringBuilder()
                    .append("  速率: ")
                    .append(formatSize(bytesPerSecond / elapsedTime * 1000))
                    .append("/s");
            elapsedTime = 0;
            bytesPerSecond = 0;
        }

        //覆盖之前的进度条，让进度条始终在一行显示
        bar.append("]   ").append(progress).append("%  ")
                .append(formatSize(transferred)).append("/").append(formatSize(total))
                .append(rate == null ? "" : rate);
        System.out.print("\r" + bar);

        return true;
    }

    @Override
    public void end() {
        System.out.println();
        System.out.println("\u001b[33m上传成功!!!\u001b[0m");
        System.out.println();
    }

    /**
     * 将字节数count转为kb或者mb单位，如果count小于1mb则转为kb，否则转为mb<br>
     * eg:<br>
     * 1536byte -> 1.5KB<br>
     * 1572864byte -> 1.5MB<br>
     */
    private String formatSize(long count) {
        BigDecimal _count = BigDecimal.valueOf(count);
        if (_count.compareTo(BigDecimal.valueOf(1024 * 1024)) < 0) {
            _count = _count.divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_UP);
            return _count + " KB";
        } else {
            _count = _count.divide(BigDecimal.valueOf(1024 * 1024), 2, RoundingMode.HALF_UP);
            return _count + " MB";
        }
    }
}
