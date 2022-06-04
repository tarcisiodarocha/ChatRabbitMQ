package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.util.*;
import java.text.*;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.google.protobuf.ByteString;
import java.io.File;

public class Mensagem{
    
    // -------------------------------- Mensagem --------------------------------------- 

    private static final DateFormat DATA = new SimpleDateFormat("dd/MM/yyyy");
    private static final DateFormat HORA = new SimpleDateFormat("HH:mm");
    static Calendar cal = null;
    static void enviarMessagem(String user_Queue, String message, String user_Destination, Channel channel, String grupo) throws Exception{
        MensagemProto.Mensagem.Builder pacoteMensagem = MensagemProto.Mensagem.newBuilder();
         
        cal = Calendar.getInstance();
        pacoteMensagem.setEmissor(user_Queue);
        pacoteMensagem.setData((DATA.format(cal.getTime())));
        pacoteMensagem.setHora(HORA.format(cal.getTime()));
        pacoteMensagem.setGrupo(grupo);
         
        MensagemProto.Conteudo.Builder conteudoMensagem = MensagemProto.Conteudo.newBuilder();
        ByteString bs = ByteString.copyFrom(message.getBytes("UTF-8"));
         
        conteudoMensagem.setCorpo(bs);
        conteudoMensagem.setTipo("");
        conteudoMensagem.setNome("");
         
        pacoteMensagem.setConteudo(conteudoMensagem);

        MensagemProto.Mensagem msg = pacoteMensagem.build();
        byte[] buffer = msg.toByteArray();
         
        channel.basicPublish(grupo, user_Destination, null, buffer);
    }
  
  
    static String recebeMessagem(byte[] pacoteMesagem, String user) throws Exception{
        MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(pacoteMesagem);
        String emissor = mensagem.getEmissor();
        String data = mensagem.getData();
        String hora = mensagem.getHora();
        String grupo = mensagem.getGrupo();
           
        MensagemProto.Conteudo conteudoMensagem = mensagem.getConteudo();
        String nome = conteudoMensagem.getNome();
        String result = "";
           
        if(nome.equals("") == false){
            ArquivoDownload download = new ArquivoDownload(user, emissor, conteudoMensagem, data, hora, grupo);
        } else {
            ByteString corpo = conteudoMensagem.getCorpo();
            String info = corpo.toStringUtf8();
           
            if(grupo.equals("") == false){
                grupo = "#" + grupo;
            }
           
            result = "\n(" + data + " às " + hora + ") " + emissor + grupo + " diz: " + info;
        }
        return result;
    }
    
    
    static void upload(String caminho, String destino, String usuario, Channel channel, String grupo)throws Exception{
        cal = Calendar.getInstance();
        String data = DATA.format(cal.getTime());
        String hora = HORA.format(cal.getTime());
        ArquivoUpload uploader = new ArquivoUpload(caminho, destino, usuario, channel, data, hora, grupo);
    }
       
    static void criarDiretorio(String user){
        new File(user + "_downloads").mkdir();
    }
    
    
    
}