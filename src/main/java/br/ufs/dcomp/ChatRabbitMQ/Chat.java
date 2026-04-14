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

    final String[] destinatario = {""};

    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope,
          AMQP.BasicProperties properties, byte[] body) throws IOException {

        String message = new String(body, "UTF-8");

        String[] partes = message.split(":", 2);
        String remetente = partes[0];
        String texto = partes.length > 1 ? partes[1].trim() : "";

        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

        String dataFormatada = agora.format(formatter);

        System.out.print("\r");
        System.out.println("(" + dataFormatada + ") " + remetente + " diz: " + texto);
        System.out.print("@" + destinatario[0] + "<< ");
      }
    };

    channel.basicConsume(QUEUE_NAME, true, consumer);

    System.out.print("<< ");

    while (true) {

      String input = scanner.nextLine();

      if (input.startsWith("@")) {
        destinatario[0] = input.substring(1);
        System.out.print("@" + destinatario[0] + "<< ");
        continue;
      }

      if (destinatario[0].isEmpty()) {
        System.out.println("Escolha um destinatário com @usuario");
        System.out.print("<< ");
        continue;
      }

      String mensagem = "@" + usuario + ": " + input;

      channel.basicPublish(
          "",
          destinatario[0],
          null,
          mensagem.getBytes("UTF-8")
      );

      System.out.print("@" + destinatario[0] + "<< ");
    }
  }
}