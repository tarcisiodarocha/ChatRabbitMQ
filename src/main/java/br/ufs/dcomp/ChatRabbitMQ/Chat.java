package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        // ehGrupo[0]: true quando o destino é um grupo
        final String[] destinatario = {""};
        final boolean[] ehGrupo = {false};

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                    AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    ChatProto.Mensagem msg = ChatProto.Mensagem.parseFrom(body);
                    String nomeArquivo = msg.getConteudo().getNome();

                    // --- Arquivo recebido: salva em background ---
                    if (!nomeArquivo.isEmpty()) {
                        final byte[] bytes    = msg.getConteudo().getCorpo().toByteArray();
                        final String dataHora = msg.getData() + " às " + msg.getHora();
                        final String emissor  = msg.getEmissor();

                        new Thread(() -> {
                            try {
                                Path dir = Paths.get(System.getProperty("user.home"), "chat", "downloads");
                                Files.createDirectories(dir);
                                Files.write(dir.resolve(nomeArquivo), bytes);

                                System.out.print("\r");
                                System.out.println("(" + dataHora + ") Arquivo \""
                                        + nomeArquivo + "\" recebido de @" + emissor + " !");
                                System.out.print(prompt(destinatario[0], ehGrupo[0]));
                                System.out.flush();
                            } catch (Exception e) {
                                System.out.println("Erro ao salvar arquivo: " + e.getMessage());
                            }
                        }).start();
                        return;
                    }

                    // --- Mensagem de texto ---
                    String conteudo = msg.getConteudo().getCorpo().toStringUtf8();
                    String dataHora = msg.getData() + " às " + msg.getHora();
                    String linha;
                    if (!msg.getGrupo().isEmpty()) {
                        linha = "(" + dataHora + ") " + msg.getEmissor()
                                + "#" + msg.getGrupo() + " diz: " + conteudo;
                    } else {
                        linha = "(" + dataHora + ") @" + msg.getEmissor() + " diz: " + conteudo;
                    }

                    System.out.print("\r");
                    System.out.println(linha);
                    System.out.print(prompt(destinatario[0], ehGrupo[0]));
                    System.out.flush();

                } catch (Exception e) {
                    // fallback para mensagens não-protobuf
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
                handleCommand(channel, connection, usuario, input.substring(1).trim(), destinatario, ehGrupo);
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

            // Envia mensagem de texto serializada com protobuf
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

    private static void handleCommand(Channel channel, Connection connection, String usuario,
            String cmd, String[] destinatario, boolean[] ehGrupo) throws IOException {

        // split com limite 2: partes[0]=comando, partes[1]=resto dos argumentos
        String[] partes = cmd.split("\\s+", 2);
        if (partes.length == 0 || partes[0].isEmpty()) return;
        String comando = partes[0];

        switch (comando) {
            case "addGroup": {
                if (partes.length < 2 || partes[1].isBlank()) {
                    System.out.println("Uso: !addGroup <nome>");
                    return;
                }
                String grupo = partes[1].trim();
                channel.exchangeDeclare(EXCHANGE_PREFIX + grupo,
                        BuiltinExchangeType.FANOUT, false, false, null);
                // criador é adicionado automaticamente ao grupo
                channel.queueBind(usuario, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Grupo '" + grupo + "' criado.");
                break;
            }
            case "addUser": {
                if (partes.length < 2) { System.out.println("Uso: !addUser <usuario> <grupo>"); return; }
                String[] args = partes[1].trim().split("\\s+");
                if (args.length < 2) { System.out.println("Uso: !addUser <usuario> <grupo>"); return; }
                String user  = args[0];
                String grupo = args[1];
                // declara a fila do usuário caso ele ainda não tenha se conectado
                channel.queueDeclare(user, false, false, false, null);
                channel.queueBind(user, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Usuário '" + user + "' adicionado ao grupo '" + grupo + "'.");
                break;
            }
            case "removeUser": {
                if (partes.length < 2) { System.out.println("Uso: !removeUser <usuario> <grupo>"); return; }
                String[] args = partes[1].trim().split("\\s+");
                if (args.length < 2) { System.out.println("Uso: !removeUser <usuario> <grupo>"); return; }
                String user  = args[0];
                String grupo = args[1];
                channel.queueUnbind(user, EXCHANGE_PREFIX + grupo, "");
                System.out.println("Usuário '" + user + "' removido do grupo '" + grupo + "'.");
                break;
            }
            case "removeGroup": {
                if (partes.length < 2 || partes[1].isBlank()) {
                    System.out.println("Uso: !removeGroup <nome>");
                    return;
                }
                String grupo = partes[1].trim();
                channel.exchangeDelete(EXCHANGE_PREFIX + grupo);
                if (ehGrupo[0] && destinatario[0].equals(grupo)) {
                    destinatario[0] = "";
                    ehGrupo[0] = false;
                }
                System.out.println("Grupo '" + grupo + "' removido.");
                break;
            }
            case "upload": {
                if (partes.length < 2 || partes[1].isBlank()) {
                    System.out.println("Uso: !upload <caminho>");
                    return;
                }
                if (destinatario[0].isEmpty()) {
                    System.out.println("Escolha um destinatário com @usuario ou #grupo primeiro.");
                    return;
                }

                String caminho = partes[1].trim();
                Path arquivoPath = Paths.get(caminho);
                if (!Files.exists(arquivoPath) || !Files.isRegularFile(arquivoPath)) {
                    System.out.println("Arquivo não encontrado: " + caminho);
                    return;
                }

                String nomeArquivo  = arquivoPath.getFileName().toString();
                String destExibicao = (ehGrupo[0] ? "#" : "@") + destinatario[0];

                // Feedback imediato — não bloqueia o chat
                System.out.println("Enviando \"" + caminho + "\" para " + destExibicao + ".");

                // Captura estado atual para uso seguro dentro da thread
                final String  destFinal    = destinatario[0];
                final boolean grupoFinal   = ehGrupo[0];
                final String  usuarioFinal = usuario;

                // Cada upload usa canal dedicado para não interferir no canal principal
                new Thread(() -> {
                    try (Channel uploadChannel = connection.createChannel()) {
                        byte[] bytes = Files.readAllBytes(arquivoPath);

                        String tipoMime = Files.probeContentType(arquivoPath);
                        if (tipoMime == null) tipoMime = "application/octet-stream";

                        LocalDateTime agora = LocalDateTime.now();
                        ChatProto.Conteudo conteudo = ChatProto.Conteudo.newBuilder()
                                .setTipo(tipoMime)
                                .setCorpo(ByteString.copyFrom(bytes))
                                .setNome(nomeArquivo)
                                .build();

                        ChatProto.Mensagem msg = ChatProto.Mensagem.newBuilder()
                                .setEmissor(usuarioFinal)
                                .setData(agora.format(FMT_DATA))
                                .setHora(agora.format(FMT_HORA))
                                .setGrupo(grupoFinal ? destFinal : "")
                                .setConteudo(conteudo)
                                .build();

                        if (grupoFinal) {
                            uploadChannel.basicPublish(EXCHANGE_PREFIX + destFinal, "", null,
                                    msg.toByteArray());
                        } else {
                            uploadChannel.basicPublish("", destFinal, null, msg.toByteArray());
                        }

                        System.out.print("\r");
                        System.out.println("Arquivo \"" + caminho + "\" foi enviado para "
                                + destExibicao + " !");
                        System.out.print(prompt(destinatario[0], ehGrupo[0]));
                        System.out.flush();
                    } catch (Exception e) {
                        System.out.println("Erro ao enviar arquivo: " + e.getMessage());
                    }
                }).start();

                // prompt já é impresso pelo caller; não retorna aqui para não duplicar
                break;
            }
            default:
                System.out.println("Comando desconhecido: !" + comando);
        }
    }
}
