package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender extends Sender{
    private Path source;
    public FileSender(Connection connection, String queueName, Path source) {
        super(connection, queueName);
        this.source = source;
    }

    @Override
    public void run() {
        try {
            this.setChannel(this.getConnection().createChannel());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            String type = Files.probeContentType(this.source);
            byte[] data = Files.readAllBytes(this.source);
            String receiver = (this.getGroupName().length() > 0) ? ("#" + this.getGroupName()) : ("@" + this.getQueueName());
            System.out.printf("\nEnviando %s para %s .\n", source.toAbsolutePath(), receiver);
            this.send(data, String.valueOf(source.getFileName()), type);
            System.out.printf("\nArquivo %s foi enviado para %s !\n", source.toAbsolutePath(), receiver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
