package br.ufs.dcomp.ChatRabbitMQ;

import java.io.IOException;
import java.util.Scanner;

import com.rabbitmq.client.*;

public class Chat {

  public static void main(String[] argv) throws Exception {

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("18.206.187.39");
    factory.setUsername("admin");
    factory.setPassword("password");
    factory.setVirtualHost("/");

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    Scanner scanner = new Scanner(System.in);

    System.out.print("User: ");
    String usuario = scanner.nextLine();

    String QUEUE_NAME = usuario;

    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope,
          AMQP.BasicProperties properties, byte[] body) throws IOException {

        String message = new String(body, "UTF-8");
        System.out.println("\n" + message);
        System.out.print("<< ");
      }
    };

    channel.basicConsume(QUEUE_NAME, true, consumer);

    String destinatario = "";

    System.out.print("<< ");

    while (true) {

      String input = scanner.nextLine();

      if (input.startsWith("@")) {
        destinatario = input.substring(1);
        System.out.print("@" + destinatario + "<< ");
        continue;
      }

      if (destinatario.isEmpty()) {
        System.out.println("Escolha um destinatário com @usuario");
        System.out.print("<< ");
        continue;
      }

      String mensagem = "@" + usuario + ": " + input;

      channel.basicPublish(
          "",
          destinatario,
          null,
          mensagem.getBytes("UTF-8")
      );

      System.out.printl("@" + destinatario + "<< ");
    }
  }
}