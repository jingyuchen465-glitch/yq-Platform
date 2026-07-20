package com.itcjy.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.util.FileSize;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自定义Logback文件Appender：按随机大小滚动日志文件
 * 当日志文件达到配置的最大大小时，自动关闭当前文件并创建新文件
 * 新文件名格式：yyyy-MM-dd_随机6位数.log，避免文件名冲突
 */
public class RandomSizeFileAppender extends AppenderBase<ILoggingEvent> {

    /** 日期格式化器，用于生成日志文件名中的日期部分 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 日志事件编码器，负责将日志事件转为字节数组 */
    private Encoder<ILoggingEvent> encoder;
    /** 日志文件存放目录，默认为 "logs" */
    private String logPath = "logs";
    /** 单个日志文件最大大小（字符串配置，如 "2MB"） */
    private String maxFileSize = "2MB";
    /** 单个日志文件最大大小（字节数，由 maxFileSize 解析而来） */
    private long maxFileSizeBytes = FileSize.valueOf("2MB").getSize();
    /** 当前日志文件的输出流 */
    private OutputStream outputStream;
    /** 当前日志文件已写入的字节数 */
    private long currentSize;

    /** 设置日志编码器（由logback配置文件注入） */
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    /** 设置日志文件存放路径（由logback配置文件注入） */
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    /** 设置单个日志文件最大大小，如 "2MB"、"500KB"（由logback配置文件注入） */
    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /** Appender启动：解析文件大小配置并打开第一个日志文件 */
    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for RandomSizeFileAppender");
            return;
        }
        try {
            // 将字符串大小配置解析为字节数
            this.maxFileSizeBytes = FileSize.valueOf(maxFileSize).getSize();
            // 创建并打开第一个日志文件
            openNewFile();
            super.start();
        } catch (Exception ex) {
            addError("Failed to start RandomSizeFileAppender", ex);
        }
    }

    /** 核心写入逻辑：将日志事件编码后写入文件，超过大小限制时触发滚动 */
    @Override
    protected synchronized void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }
        try {
            // 将日志事件编码为字节数组
            byte[] bytes = encoder.encode(eventObject);
            // 判断写入后是否超过文件大小限制，超过则滚动到新文件
            if (currentSize > 0 && currentSize + bytes.length > maxFileSizeBytes) {
                rollover();
            }
            // 写入日志数据并刷新缓冲区
            outputStream.write(bytes);
            outputStream.flush();
            // 累加当前文件已写入字节数
            currentSize += bytes.length;
        } catch (IOException ex) {
            addError("Failed to write log event", ex);
        }
    }

    /** Appender停止：关闭当前日志文件，释放资源 */
    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        closeCurrentFile();
        super.stop();
    }

    /** 日志滚动：关闭当前文件并打开新文件 */
    private void rollover() throws IOException {
        closeCurrentFile();
        openNewFile();
    }

    /** 打开新的日志文件：创建目录、生成唯一文件名、写入文件头 */
    private void openNewFile() throws IOException {
        // 确保日志目录存在
        Path directory = Path.of(logPath);
        Files.createDirectories(directory);
        // 生成不重复的文件路径
        Path file = createUniqueFilePath(directory);
        // 以 CREATE_NEW 模式打开，确保不会覆盖已有文件
        outputStream = new BufferedOutputStream(Files.newOutputStream(
                file,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        ));
        currentSize = 0;

        // 写入编码器定义的日志文件头（如有）
        byte[] header = encoder.headerBytes();
        if (header != null && header.length > 0) {
            outputStream.write(header);
            currentSize += header.length;
        }
    }

    /** 生成唯一文件路径：日期_随机6位数.log，最多尝试100次避免冲突 */
    private Path createUniqueFilePath(Path directory) throws IOException {
        for (int i = 0; i < 100; i++) {
            String date = LocalDate.now().format(DATE_FORMATTER);
            // 生成 100000~999999 的随机数作为文件名后缀
            int random = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
            Path file = directory.resolve(date + "_" + random + ".log");
            // 文件不存在则可用
            if (Files.notExists(file)) {
                return file;
            }
        }
        throw new IOException("Failed to create unique log file name");
    }

    /** 关闭当前日志文件：写入文件尾（如有），关闭流并重置状态 */
    private void closeCurrentFile() {
        if (outputStream == null) {
            return;
        }
        try {
            // 写入编码器定义的日志文件尾（如有）
            byte[] footer = encoder.footerBytes();
            if (footer != null && footer.length > 0) {
                outputStream.write(footer);
            }
            outputStream.close();
        } catch (IOException ex) {
            addError("Failed to close log file", ex);
        } finally {
            // 无论成功与否都重置流和大小计数
            outputStream = null;
            currentSize = 0;
        }
    }
}
