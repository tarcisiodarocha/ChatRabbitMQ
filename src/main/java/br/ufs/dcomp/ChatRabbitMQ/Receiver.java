package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
public class Receiver extends Thread {
    private Channel channel;
    private String queueName;

    public Receiver(Channel channel, String queueName) {
        this.channel = channel;
        this.queueName = queueName;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getQueueName() {
        return queueName;
    }

    public void checkQueue() throws Exception {
        Consumer consumer = new DefaultConsumer(this.getChannel()) {
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

                System.out.printf("(%s às %s) %s%s diz: %s%n", data, hora, emissor, grupo, corpo);
            }
        };
        //(queue-name, autoAck, consumer);
        this.getChannel().basicConsume(this.getQueueName(), true, consumer);
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.checkQueue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}