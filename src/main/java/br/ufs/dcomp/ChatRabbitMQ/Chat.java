package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.ByteString;

import java.io.*;
import java.util.Scanner;

import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;   

public class Chat {

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.196.28.123");
    factory.setUsername("admin");
    factory.setPassword("password");
    factory.setVirtualHost("/");
    
    System.out.println("> criando conexão...");
    Connection connection = factory.newConnection();
    System.out.println("> criando canal...");
    Channel channel = connection.createChannel();
    
    Scanner scanner = new Scanner(System.in);
    System.out.print("User: ");
    String username = scanner.nextLine();
    
    String QUEUE_NAME = username;
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    String preText = ">> ";
    String sendTo = "";
    String grupoNome = "";
    while(true) {
      scanner = new Scanner(System.in);
      System.out.print(preText);
      String text = scanner.nextLine();
      
      if(text.charAt(0) == '@') {
        preText = "@" + preText;
        sendTo = text.substring(1);
        scanner = new Scanner(System.in);
        System.out.print(preText);
        text = scanner.nextLine();
      }
      else if(text.charAt(0) == '!') {
        String comando = text.split(" ")[0].substring(1);
        
        
        if(comando == "addGroup") {
          String comandoGrupo = text.split(" ")[1];
          channel.exchangeDeclare(comandoGrupo, "fanout");
        }
        else if(comando == "addUser") {
          String comandoUsuario = text.split(" ")[1];
          String comandoGrupo = text.split(" ")[2];
          channel.queueBind(comandoUsuario, comandoGrupo, "");
        }
        else if(comando == "delFromGroup") {
          String comandoUsuario = text.split(" ")[1];
          String comandoGrupo = text.split(" ")[2];
          channel.queueUnbind(comandoUsuario, comandoGrupo, "");
        }
        else if(comando == "removeGroup") {
          String comandoGrupo = text.split(" ")[1];
          channel.exchangeDelete(comandoGrupo);
        }
      }
      else if(text.charAt(0) == '#') {
        preText = "#" + preText;
        grupoNome = text.substring(1);
        sendTo = "";
        scanner = new Scanner(System.in);
        System.out.print(preText);
        text = scanner.nextLine();
      }
      
      MensagemProto.Conteudo.Builder bConteudo = MensagemProto.Conteudo.newBuilder();
      bConteudo.setTipo("text/plain");
      bConteudo.setCorpo(ByteString.copyFrom(text.getBytes()));
      bConteudo.setNome(null);
      
      DateTimeFormatter dtf_data = DateTimeFormatter.ofPattern("yyyy/MM/dd");
      DateTimeFormatter dtf_hora = DateTimeFormatter.ofPattern("HH:mm:ss");
      
      LocalDateTime now = LocalDateTime.now();
      
      MensagemProto.Mensagem.Builder bMensagem = MensagemProto.Mensagem.newBuilder();
      bMensagem.setEmissor(username);
      bMensagem.setData(dtf_data.format(now));
      bMensagem.setHora(dtf_hora.format(now));
      bMensagem.setGrupo(null);
      bMensagem.setConteudo(bConteudo);
      
      MensagemProto.Mensagem messagem = bMensagem.build();
      
                      //  (exchange, routingKey, props, message-body             ); 
      channel.basicPublish(grupoNome,     sendTo,  null,  messagem.toByteArray());
      
      Consumer consumer = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd às HH:mm:ss");
          // (21/09/2016 às 20:53) marciocosta diz: 
          
          MensagemProto.Mensagem recMessage = MensagemProto.Mensagem.parseFrom(body);
          
          String emissor = recMessage.getEmissor();
          String data = recMessage.getData();
          String hora = recMessage.getHora();
          String grupo = recMessage.getGrupo();
          MensagemProto.Conteudo recConteudo = recMessage.getConteudo();

          String tipo = recConteudo.getTipo();
          String corpo = recConteudo.getCorpo().toString();
          String nome = recConteudo.getNome();

          System.out.println("(" + data + " às " + hora + ") " + emissor + " diz: " + corpo);
        }
      };
                        //(queue-name, autoAck, consumer);    
      channel.basicConsume(QUEUE_NAME, true,    consumer);
    }
  }
}