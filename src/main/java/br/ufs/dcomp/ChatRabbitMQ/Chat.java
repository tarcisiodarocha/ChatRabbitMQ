package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;

import com.google.protobuf.ByteString;

import java.util.Scanner;

import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;


/**
 * compile: mvn clean compile assembly:single
 * execute: java -jar <jarfile> 52.207.158.208 admin password
 * C:\\Users\\jadso\\Downloads\\Git-2.40.0-64-bit.exe
 **/
public class Chat extends Thread {
  private ConnectionFactory factory;
  private Connection connection;
  private Channel channel;

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

  public void run() {
    Scanner scanner = new Scanner(System.in);
    System.out.print("User: ");
    try {
      String queueName = scanner.nextLine();
      //(queue-name, durable, exclusive, auto-delete, params);
      channel.queueDeclare(queueName,false,false,false,null);

      Sender sender = new Sender(this.channel, queueName);
      Receiver receiver = new Receiver(this.channel, queueName);
      FilesReceiver filesReceiver = new FilesReceiver(this.channel, queueName);
      sender.start();
      receiver.start();
      filesReceiver.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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