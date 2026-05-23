package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Chat {

    private static final String EXCHANGE_PREFIX = "grupo.";
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

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
        String usuario = scanner.nextLine().trim();

        channel.queueDeclare(usuario, false, false, false, null);

        // destinatario[0]: nome do destino atual (usuário ou grupo)
        // ehGrupo[0]: true se o destino atual é um grupo
        final String[] destinatario = {""};
        final boolean[] ehGrupo = {false};

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                    AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    ChatProto.Mensagem msg = ChatProto.Mensagem.parseFrom(body);
                    String conteudo = msg.getConteudo().getCorpo().toStringUtf8();
                    String dataHora = msg.getData() + " às " + msg.getHora();

                    String linha;
                    if (!msg.getGrupo().isEmpty()) {
                        linha = "(" + dataHora + ") " + msg.getEmissor() + "#" + msg.getGrupo() + " diz: " + conteudo;
                    } else {
                        linha = "(" + dataHora + ") @" + msg.getEmissor() + " diz: " + conteudo;
                    }

                    System.out.print("\r");
                    System.out.println(linha);
                    System.out.print(prompt(destinatario[0], ehGrupo[0]));
                    System.out.flush();
                } catch (Exception e) {
                    // mensagem não-protobuf (compatibilidade com etapa 1)
                    System.out.print("\r");
                    System.out.println(new String(body, "UTF-8"));
                    System.out.print(prompt(destinatario[0], ehGrupo[0]));
                    System.out.flush();
                }
            }
        };

        channel.basicConsume(usuario, true, consumer);

        System.out.print("<< ");
        System.out.flush();

        while (true) {
            String input = scanner.nextLine();

            // Troca destinatário para usuário
            if (input.startsWith("@")) {
                destinatario[0] = input.substring(1).trim();
                ehGrupo[0] = false;
                System.out.print(prompt(destinatario[0], ehGrupo[0]));
                System.out.flush();
                continue;
            }

            // Troca destinatário para grupo
            if (input.startsWith("#")) {
                destinatario[0] = input.substring(1).trim();
                ehGrupo[0] = true;
                System.out.print(prompt(destinatario[0], ehGrupo[0]));
                System.out.flush();
                continue;
            }

            // Comando de administração
            if (input.startsWith("!")) {
                handleCommand(channel, usuario, input.substring(1).trim(), destinatario, ehGrupo);
                System.out.print(prompt(destinatario[0], ehGrupo[0]));
                System.out.flush();
                continue;
            }

            if (destinatario[0].isEmpty()) {
                System.out.println("Escolha um destinatário com @usuario ou #grupo");
                System.out.print("<< ");
                System.out.flush();
                continue;
            }

            // Envia mensagem serializada com protobuf
            LocalDateTime agora = LocalDateTime.now();
            ChatProto.Conteudo conteudo = ChatProto.Conteudo.newBuilder()
                    .setTipo("text/plain")
                    .setCorpo(ByteString.copyFromUtf8(input))
                    .build();

            ChatProto.Mensagem.Builder msgBuilder = ChatProto.Mensagem.newBuilder()
                    .setEmissor(usuario)
                    .setData(agora.format(FMT_DATA))
                    .setHora(agora.format(FMT_HORA))
                    .setConteudo(conteudo);

            if (ehGrupo[0]) {
                msgBuilder.setGrupo(destinatario[0]);
                channel.basicPublish(EXCHANGE_PREFIX + destinatario[0], "", null,
                        msgBuilder.build().toByteArray());
            } else {
                channel.basicPublish("", destinatario[0], null,
                        msgBuilder.build().toByteArray());
            }

            System.out.print(prompt(destinatario[0], ehGrupo[0]));
            System.out.flush();
        }
    }

    private static String prompt(String destinatario, boolean ehGrupo) {
        if (destinatario.isEmpty()) return "<< ";
        return (ehGrupo ? "#" : "@") + destinatario + "<< ";
    }

    private static void handleCommand(Channel channel, String usuario, String cmd,
            String[] destinatario, boolean[] ehGrupo) throws IOException {

        String[] partes = cmd.split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return;

        switch (partes[0]) {
            case "addGroup": {
                if (partes.length < 2) { System.out.println("Uso: !addGroup <nome>"); return; }
                String grupo = partes[1];
                channel.exchangeDeclare(EXCHANGE_PREFIX + grupo, BuiltinExchangeType.FANOUT, false, false, null);
                // criador é adicionado automaticamente
                channel.queueBind(usuario, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Grupo '" + grupo + "' criado.");
                break;
            }
            case "addUser": {
                if (partes.length < 3) { System.out.println("Uso: !addUser <usuario> <grupo>"); return; }
                String user  = partes[1];
                String grupo = partes[2];
                // garante que a fila existe antes de fazer o bind
                channel.queueDeclare(user, false, false, false, null);
                channel.queueBind(user, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Usuário '" + user + "' adicionado ao grupo '" + grupo + "'.");
                break;
            }
            case "removeUser": {
                if (partes.length < 3) { System.out.println("Uso: !removeUser <usuario> <grupo>"); return; }
                String user  = partes[1];
                String grupo = partes[2];
                channel.queueUnbind(user, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Usuário '" + user + "' removido do grupo '" + grupo + "'.");
                break;
            }
            case "removeGroup": {
                if (partes.length < 2) { System.out.println("Uso: !removeGroup <nome>"); return; }
                String grupo = partes[1];
                channel.exchangeDelete(EXCHANGE_PREFIX + grupo);
                // limpa o destinatário se estava nesse grupo
                if (ehGrupo[0] && destinatario[0].equals(grupo)) {
                    destinatario[0] = "";
                    ehGrupo[0] = false;
                }
                System.out.println("Grupo '" + grupo + "' removido.");
                break;
            }
            default:
                System.out.println("Comando desconhecido: !" + partes[0]);
        }
    }
}
