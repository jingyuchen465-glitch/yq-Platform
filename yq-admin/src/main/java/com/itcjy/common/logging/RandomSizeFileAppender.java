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

public class RandomSizeFileAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Encoder<ILoggingEvent> encoder;
    private String logPath = "logs";
    private String maxFileSize = "2MB";
    private long maxFileSizeBytes = FileSize.valueOf("2MB").getSize();
    private OutputStream outputStream;
    private long currentSize;

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for RandomSizeFileAppender");
            return;
        }
        try {
            this.maxFileSizeBytes = FileSize.valueOf(maxFileSize).getSize();
            openNewFile();
            super.start();
        } catch (Exception ex) {
            addError("Failed to start RandomSizeFileAppender", ex);
        }
    }

    @Override
    protected synchronized void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }
        try {
            byte[] bytes = encoder.encode(eventObject);
            if (currentSize > 0 && currentSize + bytes.length > maxFileSizeBytes) {
                rollover();
            }
            outputStream.write(bytes);
            outputStream.flush();
            currentSize += bytes.length;
        } catch (IOException ex) {
            addError("Failed to write log event", ex);
        }
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        closeCurrentFile();
        super.stop();
    }

    private void rollover() throws IOException {
        closeCurrentFile();
        openNewFile();
    }

    private void openNewFile() throws IOException {
        Path directory = Path.of(logPath);
        Files.createDirectories(directory);
        Path file = createUniqueFilePath(directory);
        outputStream = new BufferedOutputStream(Files.newOutputStream(
                file,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        ));
        currentSize = 0;

        byte[] header = encoder.headerBytes();
        if (header != null && header.length > 0) {
            outputStream.write(header);
            currentSize += header.length;
        }
    }

    private Path createUniqueFilePath(Path directory) throws IOException {
        for (int i = 0; i < 100; i++) {
            String date = LocalDate.now().format(DATE_FORMATTER);
            int random = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
            Path file = directory.resolve(date + "_" + random + ".log");
            if (Files.notExists(file)) {
                return file;
            }
        }
        throw new IOException("Failed to create unique log file name");
    }

    private void closeCurrentFile() {
        if (outputStream == null) {
            return;
        }
        try {
            byte[] footer = encoder.footerBytes();
            if (footer != null && footer.length > 0) {
                outputStream.write(footer);
            }
            outputStream.close();
        } catch (IOException ex) {
            addError("Failed to close log file", ex);
        } finally {
            outputStream = null;
            currentSize = 0;
        }
    }
}
