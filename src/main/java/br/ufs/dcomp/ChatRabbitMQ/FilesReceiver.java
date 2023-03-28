package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FilesReceiver extends Receiver{
    public FilesReceiver(Connection connection, String queueName) {
        super(connection,queueName);
    }

    @Override
    public void checkQueue(Channel channel) throws Exception {
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // (21/09/2016 às 20:53) marciocosta diz:

                MensagemProto.Mensagem recMessage = MensagemProto.Mensagem.parseFrom(body);

                String sender = recMessage.getEmissor();
                String date = recMessage.getData();
                String hour = recMessage.getHora();
                String group = recMessage.getGrupo();
                MensagemProto.Conteudo recContent = recMessage.getConteudo();

                String type = recContent.getTipo();
                byte[] content = recContent.getCorpo().toByteArray();
                String filename = recContent.getNome();

                group = (group.length() > 0) ? ("#" + group) : group;

                Path source = Paths.get("C:\\downloads\\" + filename);
                Files.write(source, content, StandardOpenOption.CREATE_NEW);

                System.out.printf("\n(%s às %s) Arquivo \"%s\" recebido de @%s%s%n !\n", date, hour, filename, sender, group);
            }
        };
        //(queue-name, autoAck, consumer);
        channel.basicConsume(this.getQueueName(), true, consumer);
    }
}
