package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Sender extends Thread {
    private Connection connection;
    private Channel channel;
    private String queueName;
    private String preText = ">> ";
    private String sendTo = "";
    private String groupName = "";
    public Sender(Connection connection, String queueName) {
        this.connection = connection;
        this.queueName = queueName;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getPreText() {
        return preText;
    }

    public void setPreText(String preText) {
        this.preText = preText;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public void commands(String text) throws Exception {
        String command = text.split(" ")[0].substring(1);
        String username;
        String groupName;
        String filepath;
        String type;

        switch (command.trim()) {
            case "addGroup":
                groupName = text.split(" ")[1];
                this.getChannel().exchangeDeclare(groupName.trim(), BuiltinExchangeType.FANOUT);
                this.getChannel().queueBind(this.getQueueName().trim(), groupName.trim(), "");
                System.out.println(groupName + " created");
                break;

            case "addUser":
                username = text.split(" ")[1];
                groupName = text.split(" ")[2];
                this.getChannel().queueBind(username.trim(), groupName.trim(), "");
                break;

            case "delFromGroup":
                username = text.split(" ")[1];
                groupName = text.split(" ")[2];
                this.getChannel().queueUnbind(username.trim(), groupName.trim(), "");
                break;

            case "removeGroup":
                groupName = text.split(" ")[1];
                this.getChannel().exchangeDelete(groupName.trim());
                break;
            case "upload":
                filepath = text.split(" ")[1];
                System.out.println(command + " " + filepath);
                try {
                    Path source = Paths.get(filepath);
                    Sender fileSender = new FileSender(this.connection, this.queueName, source);
                    fileSender.start();
//                    this.send(data, String.valueOf(source.getFileName()), type);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

                break;
            default:
                System.out.println("command invalid: " + command);
        }
    }

    public void send(byte[] content, String filename, String type) throws Exception {
        MensagemProto.Conteudo.Builder bContent = MensagemProto.Conteudo.newBuilder();
        bContent.setTipo(type);
        bContent.setCorpo(ByteString.copyFrom(content));
        bContent.setNome(filename);

        DateTimeFormatter dtf_data = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter dtf_hora = DateTimeFormatter.ofPattern("HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();

        MensagemProto.Mensagem.Builder bMessage = MensagemProto.Mensagem.newBuilder();
        bMessage.setEmissor(this.getQueueName());
        bMessage.setData(dtf_data.format(now));
        bMessage.setHora(dtf_hora.format(now));
        bMessage.setGrupo(this.getGroupName());
        bMessage.setConteudo(bContent);

        MensagemProto.Mensagem message = bMessage.build();
        //  (exchange, routingKey, props, message-body             );
        this.getChannel().basicPublish(this.getGroupName(), this.getSendTo(), null, message.toByteArray());
    }

    @Override
    public void run(){
        try {
            this.setChannel(this.getConnection().createChannel());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            String text;
            Scanner scanner = new Scanner(System.in);
            System.out.print(preText);
            text = scanner.nextLine();

            if (text.length() == 0) {
                continue;
            }

            switch (text.charAt(0)) {
                case '@':
                    this.setPreText(text + ">> ");
                    this.setSendTo(text.substring(1));

                    try {
                        this.getChannel().queueDeclare(this.getSendTo(), false, false, false, null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.print(this.getPreText());
                    scanner = new Scanner(System.in);
                    text = scanner.nextLine();
                    break;

                case '#':
                    this.setPreText(text + ">> ");
                    this.setGroupName(text.substring(1));
                    this.setSendTo("");

                    scanner = new Scanner(System.in);
                    System.out.print(this.getPreText());
                    text = scanner.nextLine();
                    break;

                case '!':
                    try {
                        this.commands(text);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    break;
            }
            try {
                this.send(text.getBytes(), "", "text/plain");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}