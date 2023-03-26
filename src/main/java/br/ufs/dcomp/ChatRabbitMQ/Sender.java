package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Sender extends Thread {
    private Channel channel;
    private String queueName;
    private String preText = ">> ";
    private String sendTo = "";
    private String groupName = "";
    public Sender(Channel channel, String queueName) {
        this.channel = channel;
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
                this.channel.exchangeDeclare(groupName.trim(), BuiltinExchangeType.FANOUT);
                this.channel.queueBind(this.queueName.trim(), groupName.trim(), "");
                System.out.println(groupName + " created");
                break;

            case "addUser":
                username = text.split(" ")[1];
                groupName = text.split(" ")[2];
                this.channel.queueBind(username.trim(), groupName.trim(), "");
                break;

            case "delFromGroup":
                username = text.split(" ")[1];
                groupName = text.split(" ")[2];
                this.channel.queueUnbind(username.trim(), groupName.trim(), "");
                break;

            case "removeGroup":
                groupName = text.split(" ")[1];
                this.channel.exchangeDelete(groupName.trim());
                break;
            case "upload":
                filepath = text.split(" ")[1];
                System.out.println(command + " " + filepath);
                try {
                    Path source = Paths.get(filepath);
                    type = Files.probeContentType(source);
                    byte[] data = Files.readAllBytes(source);
                    String receiver = (this.groupName.length() > 0) ? ("#" + this.groupName) : ("@" + this.queueName);
                    System.out.printf("Enviando %s para %s .\n", filepath, receiver);
                    this.send(data, String.valueOf(source.getFileName()),type);
                    System.out.printf("\nArquivo %s foi enviado para %s !\n", filepath, receiver);
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
        bMessage.setEmissor(this.queueName);
        bMessage.setData(dtf_data.format(now));
        bMessage.setHora(dtf_hora.format(now));
        bMessage.setGrupo(this.groupName);
        bMessage.setConteudo(bContent);

        MensagemProto.Mensagem message = bMessage.build();

        //  (exchange, routingKey, props, message-body             );
        this.channel.basicPublish(this.groupName, this.sendTo, null, message.toByteArray());
    }

    @Override
    public void run(){
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(preText);
            String text = scanner.nextLine();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (text.length() == 0) {
                continue;
            }

            switch (text.charAt(0)) {
                case '@':
                    this.preText = text + ">> ";
                    this.sendTo = text.substring(1);

                    System.out.print(this.preText);
                    scanner = new Scanner(System.in);
                    text = scanner.nextLine();
                    break;

                case '#':
                    this.preText = text + ">> ";
                    this.groupName = text.substring(1);
                    this.sendTo = "";

                    scanner = new Scanner(System.in);
                    System.out.print(preText);
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
                    try {
                        this.send(text.getBytes(), "", "text/plain");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }
}