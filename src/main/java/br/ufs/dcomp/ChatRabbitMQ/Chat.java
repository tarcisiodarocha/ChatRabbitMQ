// O package deve corresponder à estrutura de pastas do seu projeto
// package br.ufs.dcomp.ChatRabbitMQ; 

import com.rabbitmq.client.*;
import org.json.JSONObject; // Importa a biblioteca JSON

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

public class Chat {

    // --- Configuração do RabbitMQ ---
    // Altere aqui, como no seu arquivo de exemplo
    private static final String RABBITMQ_HOST = "localhost"; // Alterar (ex: "ip-da-instancia-da-aws")
    private static final String RABBITMQ_USER = "guest";     // Alterar (ex: "usuário-do-rabbitmq-server")
    private static final String RABBITMQ_PASS = "guest";     // Alterar (ex: "senha-do-rabbitmq-server")
    // ---------------------------------

    private static String username;
    private static String currentRecipient = "";
    private static Connection connection;

    // Objeto usado como "lock" para sincronizar a escrita no console
    private static final Object promptLock = new Object();

    public static void main(String[] argv) throws Exception {
        // 1. Configurar Conexão
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASS);
        factory.setVirtualHost("/");

        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            System.err.println("Erro ao conectar ao RabbitMQ: " + e.getMessage());
            System.err.println("Verifique se o host/usuário/senha estão corretos e o servidor está online.");
            return;
        }

        // 2. Perguntar o nome do usuário
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("User: ");
        username = reader.readLine().trim();
        String userQueueName = username + "_queue"; // Fila pessoal do usuário

        // 3. Iniciar o consumidor (receptor de mensagens)
        startReceiver(userQueueName);
        System.out.println("... Fila '" + userQueueName + "' criada. Aguardando mensagens.");

        // 4. Loop de Envio de Mensagens (na thread principal)
        while (true) {
            String prompt;
            synchronized (promptLock) {
                prompt = getPrompt();
                System.out.print(prompt);
                System.out.flush();
            }

            // Lê a entrada do usuário
            String input = reader.readLine();
            if (input == null) break; // Fim da entrada (Ctrl+D)

            input = input.trim();
            if (input.isEmpty()) continue;

            // --- Lógica de Comandos ---
            if (input.startsWith("@")) {
                String newRecipient = input.substring(1).trim();
                if (!newRecipient.isEmpty()) {
                    synchronized (promptLock) {
                        currentRecipient = newRecipient;
                        System.out.println("(Sistema) Conversando com @" + currentRecipient);
                    }
                }
            } else {
                // --- Lógica de Envio ---
                if (currentRecipient.isEmpty()) {
                    synchronized (promptLock) {
                        System.out.println("(Sistema) Use @<usuario> para definir um destinatário.");
                    }
                } else {
                    sendMessage(input);
                }
            }
        }
        
        System.out.println("Encerrando...");
        connection.close();
    }

    /**
     * Inicia o consumidor (receptor) em uma thread gerenciada pela biblioteca do RabbitMQ.
     */
    private static void startReceiver(String queueName) throws IOException {
        // Criamos um novo canal para o consumidor
        Channel channel = connection.createChannel();

        // (queue-name, durable, exclusive, auto-delete, params);
        // "durable = true" garante que a fila sobreviva a reinícios do RabbitMQ
        channel.queueDeclare(queueName, true, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                
                String rawMessage = new String(body, StandardCharsets.UTF_8);
                String sender = "desconhecido";
                String content = "";

                // Decodifica a mensagem JSON
                try {
                    JSONObject json = new JSONObject(rawMessage);
                    sender = json.optString("sender", "desconhecido");
                    content = json.optString("content", "");
                } catch (Exception e) {
                    System.err.println("Erro ao decodificar JSON: " + e.getMessage());
                    content = rawMessage; // Mostra a mensagem crua em caso de erro
                }

                String displayMessage = String.format("%s @%s diz: %s",
                        getTimestamp(), sender, content);

                // Sincroniza a escrita no console para não bagunçar com o prompt
                synchronized (promptLock) {
                    // 1. Limpa a linha atual (onde o usuário pode estar digitando)
                    // Esta é uma aproximação de "limpar linha" em Java.
                    System.out.print("\r" + " ".repeat(80) + "\r");

                    // 2. Imprime a mensagem recebida
                    System.out.println(displayMessage);

                    // 3. Redesenha o prompt
                    System.out.print(getPrompt());
                    System.out.flush();
                }
            }
        };

        // (queue-name, autoAck, consumer);
        // autoAck = true: remove a mensagem da fila assim que é entregue
        channel.basicConsume(queueName, true, consumer);
    }

    /**
     * Envia uma mensagem para o destinatário atual.
     */
    private static void sendMessage(String content) {
        // É boa prática usar um canal por thread/tarefa.
        // Usamos try-with-resources para garantir que o canal seja fechado.
        try (Channel channel = connection.createChannel()) {
            String recipientQueue = currentRecipient + "_queue";

            // Garante que a fila de destino exista (durable=true)
            channel.queueDeclare(recipientQueue, true, false, false, null);

            // Cria um payload JSON para enviar o remetente junto com a mensagem
            JSONObject json = new JSONObject();
            json.put("sender", username);
            json.put("content", content);
            String message = json.toString();

            // Publica a mensagem
            channel.basicPublish(
                    "",             // exchange (default)
                    recipientQueue, // routing key (nome da fila)
                    MessageProperties.PERSISTENT_TEXT_PLAIN, // Torna a msg persistente
                    message.getBytes(StandardCharsets.UTF_8)
            );

        } catch (IOException | TimeoutException e) {
            synchronized (promptLock) {
                System.err.println("\nErro ao enviar mensagem: " + e.getMessage());
            }
        }
    }

    /**
     * Retorna o texto do prompt atual com base no destinatário.
     */
    private static String getPrompt() {
        if (currentRecipient.isEmpty()) {
            return "<< ";
        } else {
            return "@" + currentRecipient + "<< ";
        }
    }

    /**
     * Retorna um timestamp formatado.
     */
    private static String getTimestamp() {
        // (21/09/2016 às 20:53)
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("'(dd/MM/yyyy 'às' HH:mm)'"));
    }
}
