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
 **/
public class Chat extends ConnectionFactory {
  private Connection connection;
  private Channel channel;
  private String queueName;
  private String preText = ">> ";
  private String sendTo = "";
  private String groupName = "";

  public Chat(String connHost, String connUser, String connPass) throws Exception {
    this.setHost(connHost);
    this.setUsername(connUser);
    this.setPassword(connPass);
    this.setVirtualHost("/");

    System.out.println("> connecting...");
    this.connection = this.newConnection();
    System.out.println("> creating channel...");
    this.channel = this.connection.createChannel();
  }

  public void setQueueName(String queueName) throws Exception {
    this.queueName = queueName;
    //                        (queue-name, durable, exclusive, auto-delete, params);
    this.channel.queueDeclare(this.queueName, false, false, false, null);
  }

  public void groupCommands(String text) throws Exception {
    String command = text.split(" ")[0].substring(1);

    if (command == "addGroup") {
      String groupName = text.split(" ")[1];
      String answer = this.channel.exchangeDeclare(groupName, "direct").toString();
      System.out.println("< " + answer + " >");

    } else if (command == "addUser") {
      String username = text.split(" ")[1];
      String groupName = text.split(" ")[2];
      this.channel.queueBind(username, groupName, "");

    } else if (command == "delFromGroup") {
      String username = text.split(" ")[1];
      String groupName = text.split(" ")[2];
      this.channel.queueUnbind(username, groupName, "");

    } else if (command == "removeGroup") {
      String groupName = text.split(" ")[1];
      this.channel.exchangeDelete(groupName);
    }
  }

  public void sendText(String text) throws Exception {
    MensagemProto.Conteudo.Builder bContent = MensagemProto.Conteudo.newBuilder();
    bContent.setTipo("text/plain");
    bContent.setCorpo(ByteString.copyFrom(text.getBytes()));
    bContent.setNome("");

    DateTimeFormatter dtf_data = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    DateTimeFormatter dtf_hora = DateTimeFormatter.ofPattern("HH:mm:ss");

    LocalDateTime now = LocalDateTime.now();

    MensagemProto.Mensagem.Builder bMessage = MensagemProto.Mensagem.newBuilder();
    bMessage.setEmissor(this.queueName);
    bMessage.setData(dtf_data.format(now));
    bMessage.setHora(dtf_hora.format(now));
    bMessage.setGrupo(this.groupName);
    bMessage.setConteudo(bContent);

    MensagemProto.Mensagem message = bMessage.build();

    //  (exchange, routingKey, props, message-body             );
    this.channel.basicPublish(this.groupName, this.sendTo, null, message.toByteArray());
  }

  public void checkMessages() throws Exception {
    Consumer consumer = new DefaultConsumer(this.channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd as HH:mm:ss");
        // (21/09/2016 às 20:53) marciocosta diz:

        MensagemProto.Mensagem recMessage = MensagemProto.Mensagem.parseFrom(body);

        String emissor = recMessage.getEmissor();
        String data = recMessage.getData();
        String hora = recMessage.getHora();
        String grupo = recMessage.getGrupo();
        MensagemProto.Conteudo recConteudo = recMessage.getConteudo();

        String tipo = recConteudo.getTipo();
        String corpo = recConteudo.getCorpo().toStringUtf8();
        String nome = recConteudo.getNome();

        System.out.println("\n(" + data + " as " + hora + ") " + emissor + " diz: " + corpo);
      }
    };
    //(queue-name, autoAck, consumer);
    this.channel.basicConsume(this.queueName, true, consumer);
  }

  public void run() throws Exception {
    Scanner scanner = new Scanner(System.in);
    System.out.print("User: ");
    this.setQueueName(scanner.nextLine());

    while (true) {
      scanner = new Scanner(System.in);
      System.out.print(preText);
      String text = scanner.nextLine();

      if (text.length() == 0) {
        continue;
      }

      if (text.charAt(0) == '@') {
        this.preText = text + this.preText;
        this.sendTo = text.substring(1);

        System.out.print(this.preText);
        scanner = new Scanner(System.in);
        text = scanner.nextLine();

      } else if (text.charAt(0) == '#') {
        this.preText = text + this.preText;
        this.groupName = text.substring(1);
        this.sendTo = "";

        scanner = new Scanner(System.in);
        System.out.print(preText);
        text = scanner.nextLine();
      } else if (text.charAt(0) == '!') {
        this.groupCommands(text);
      }

      this.sendText(text);
      this.checkMessages();
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