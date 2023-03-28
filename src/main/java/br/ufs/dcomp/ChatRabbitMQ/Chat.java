package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;

import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


/**
 * compile: mvn clean compile assembly:single
 * execute: java -jar <jarfile> <host> <username> <password>
 **/
public class Chat extends Thread {
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String queueName = "";

    public Chat(String connHost, String connUser, String connPass) throws Exception {
        this.factory = new ConnectionFactory();
        this.factory.setHost(connHost);
        this.factory.setUsername(connUser);
        this.factory.setPassword(connPass);
        this.factory.setVirtualHost("/");

        System.out.println("> connecting...");
        this.connection = this.factory.newConnection();
        System.out.println("> creating channel...");
        this.channel = this.connection.createChannel();
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("User: ");
        try {
            String queueName = scanner.nextLine();
            this.setQueueName(queueName);
            //(queue-name, durable, exclusive, auto-delete, params);
            this.getChannel().queueDeclare(this.getQueueName(),false,false,false,null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Sender sender = new Sender(this.getConnection(), this.getQueueName());
        Receiver receiver = new Receiver(this.getConnection(), this.getQueueName());
        FilesReceiver filesReceiver = new FilesReceiver(this.getConnection(), this.getQueueName());

        sender.start();
        receiver.start();
        filesReceiver.start();
    }

    public static void main(String[] arg) throws Exception {
        if(arg.length != 3) {
            System.out.println("java -jar <jarfile> <host> <username> <password>");
            return;
        }

        Chat chat = new Chat(arg[0], arg[1], arg[2]);
        chat.run();
    }
}