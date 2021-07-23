/**
 * A command line Processing Node example<br>
 * Sends and receives messages: unicast and groupcast
 * May need a GroupDefiner
 */
package br.com.meslin.main;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.meslin.auxiliar.StaticLibrary;
import ckafka.data.Swap;
import ckafka.data.SwapData;
import main.java.application.ModelApplication;

/**
 * @author meslin
 *
 */
public class MainPN extends ModelApplication {
	private Swap swap;
	private ObjectMapper objectMapper;

	/**
	 * Constructor
	 */
	public MainPN() {		
        this.objectMapper = new ObjectMapper();
        this.swap = new Swap(objectMapper);
	}

	/**
	 * Main function
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
    	// creating missing environment variable
		Map<String,String> env = new HashMap<String, String>();
		env.putAll(System.getenv());
		if(System.getenv("app.consumer.topics") == null) 			env.put("app.consumer.topics", "AppModel");
		if(System.getenv("app.consumer.auto.offset.reset") == null) env.put("app.consumer.auto.offset.reset", "latest");
		if(System.getenv("app.consumer.bootstrap.servers") == null) env.put("app.consumer.bootstrap.servers", "127.0.0.1:9092");
		if(System.getenv("app.consumer.group.id") == null) 			env.put("app.consumer.group.id", "gw-consumer");
		if(System.getenv("app.producer.bootstrap.servers") == null) env.put("app.producer.bootstrap.servers", "127.0.0.1:9092");
		if(System.getenv("app.producer.retries") == null) 			env.put("app.producer.retries", "3");
		if(System.getenv("app.producer.enable.idempotence") == null)env.put("app.producer.enable.idempotence", "true");
		if(System.getenv("app.producer.linger.ms") == null) 		env.put("app.producer.linger.ms", "1");
		if(System.getenv("app.producer.acks") == null) 				env.put("app.producer.acks", "all");
		try {
			StaticLibrary.setEnv(env);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MainPN vaiFazer = new MainPN();
		vaiFazer.fazTudo();
	}

	/**
	 * fazTudo<br>
	 * Read user option from keyboard (unicast or groupcast message)<br>
	 * Read destination receipt from keyboard (UUID or Group)<br>
	 * Read message from keyboard<br>
	 * Send message<br>
	 */
	private void fazTudo() {
		/*
		 * User interface (!)
		 */
		Scanner keyboard = new Scanner(System.in);
		boolean fim = false;
		while(!fim) {
			System.out.print("Mensagem para (G)rupo ou (I)ndivíduo (Z para terminar)? ");
			String linha = keyboard.nextLine();
			linha = linha.toUpperCase();
			System.out.println(String.format("Sua opção foi %s.", linha));
			switch (linha) {
				case "G":
					sendGroupcastMessage(keyboard);
					break;
				case "I":
					sendUnicastMessage(keyboard);
					break;
				case "Z":
					fim = true;
					break;

				default:
					System.out.println("Opção inválida");
					break;
			}
			if(linha.equals("Z")) break;
		}
		keyboard.close();
		System.out.println("FIM!");
		System.exit(0);
	}

	/**
	 * Sends a unicast message
	 * @param keyboard
	 */
	private void sendUnicastMessage(Scanner keyboard) {
		System.out.println("Mensagem unicast. Entre com o UUID do indivíduo:\nHHHHHHHH-HHHH-HHHH-HHHH-HHHHHHHHHHHH");
		String uuid = keyboard.nextLine();
		System.out.print("Entre com a mensagem: ");
		String messageText = keyboard.nextLine();
		System.out.println(String.format("Enviando mensagem |%s| para o indivíduo %s.", messageText, uuid));

		// Create and send the message
		try {
			sendRecord(createRecord("PrivateMessageTopic", uuid, swap.SwapDataSerialization(createSwapData(messageText))));
		}
		catch (Exception e) {
			e.printStackTrace();
            logger.error("Error SendPrivateMessage", e);
		}
	}

	/**
	 * sendGroupcastMessage<br>
	 * Sends a groupcast message<br>
	 * @param keyboard
	 */
	private void sendGroupcastMessage(Scanner keyboard) {
		System.out.print("Mensagem groupcast. Entre com o número do grupo: ");
		String group = keyboard.nextLine();
		System.out.print("Entre com a mensagem: ");
		String messageText = keyboard.nextLine();
		System.out.println(String.format("Enviando mensagem %s para o grupo %s.", messageText, group));
		
		try {
			sendRecord(createRecord("GroupMessageTopic", group, swap.SwapDataSerialization(createSwapData(messageText))));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error SendGroupCastMessage", e);
		}
	}

    /**
     * Call back
     */
	@SuppressWarnings("rawtypes")
	@Override
	public void recordReceived(ConsumerRecord record) {
		// value possui o conteúdo transmitido e recebido em byte[]
        this.logger.debug("Record Received " + record.value().toString());
        System.out.println(String.format("Mensagem recebida de %s", record.key()));	// String com UUID do remetente
        
        try {
			SwapData data = swap.SwapDataDeserialization((byte[]) record.value());
			String text = new String(data.getMessage(), StandardCharsets.UTF_8);
	        System.out.println("Mensagem recebida = " + text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
