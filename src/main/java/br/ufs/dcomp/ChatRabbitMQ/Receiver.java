package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

public class Receiver extends Thread {
    private Connection connection;
    private String queueName;

    public Receiver(Connection connection, String queueName) {
        this.connection = connection;
        this.queueName = queueName;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getQueueName() {
        return queueName;
    }

    public void checkQueue(Channel channel) throws Exception {
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
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

                grupo = (grupo.length() > 0) ? ("#" + grupo) : grupo;

                System.out.printf("\n(%s às %s) %s%s diz: %s%n", data, hora, emissor, grupo, corpo);
            }
        };
        //(queue-name, autoAck, consumer);
        channel.basicConsume(this.getQueueName(), true, consumer);
    }

    @Override
    public void run() {
        Channel channel;
        try {
            channel = this.getConnection().createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(true) {
            try {
                this.checkQueue(channel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}