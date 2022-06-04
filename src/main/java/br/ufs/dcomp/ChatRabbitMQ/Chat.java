package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.*;
import com.google.protobuf.ByteString;

public class Chat {
  
  public static String usuario = "";
 
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.226.142.214"); // 
    factory.setUsername("ramon"); // Alterar
    factory.setPassword("ramontimoteo"); // Alterar
    factory.setVirtualHost("/");
    
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel(); //canal
    Channel channel_arquivos = connection.createChannel(); //canal dos arquivos

    Scanner entrada = new Scanner(System.in);
    Grupo grupo = new Grupo(channel,channel_arquivos); //grupo
    
    Mensagem line = new Mensagem();
    
    System.out.print("User: ");
    String user = entrada.nextLine();
    
    String nomeGrupo = "";
    line.criarDiretorio(user);
    
    String QUEUE_NAME = user; //fila para cada nome
    String QUEUE_NAME_FILE = QUEUE_NAME + "F"; //fila dos arquivos
    
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null); //declara canal
    channel_arquivos.queueDeclare(QUEUE_NAME_FILE, false,   false,     false,       null); //declara canal de arquivo
    
    Consumer consumer = new DefaultConsumer(channel) { //consumidor
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
        throws IOException {
          try{
            System.out.println(line.recebeMessagem(body, user));
          } catch (Exception ex){
            System.out.println (ex.toString());
          }
          System.out.println(Chat.usuario + ">>");
        }
    };
    

    String msg = "";
    
    channel.basicConsume(QUEUE_NAME, true,    consumer); //parte para consumir o canal
    channel_arquivos.basicConsume(QUEUE_NAME_FILE, true, consumer); //parte para connsumir os arquivos

    while(msg.equals(".") == false){
      System.out.print(Chat.usuario + ">> " + ""); //usuario normal
      msg = entrada.nextLine();

      if(msg.equals(".") == true) //se for digitado ponto, para
        break;
      if(msg.charAt(0) == '@'){ //@indica que é para um usuario
        Chat.usuario = msg;
        nomeGrupo = "";
      }
      else if(msg.charAt(0) == '#'){ //indicda que é para um grupo
        Chat.usuario = msg;
        nomeGrupo = msg.substring(1);
      }
      else if(msg.charAt(0) == '!'){ //indica uma das funcoes de grp
        grupo.verificaMensagem(msg, user, Chat.usuario.substring(1), nomeGrupo);
      }
      else if(Chat.usuario.equals("") == false){  
        if(Chat.usuario.charAt(0) == '#')
          line.enviarMessagem(user, msg, "", channel, nomeGrupo);
        else 
          line.enviarMessagem(user, msg, Chat.usuario.substring(1), channel, "");
      }
    }
    channel.close();
    connection.close();
  }
}