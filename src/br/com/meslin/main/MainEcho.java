/**
 * Implements a echo server using processing node<br>
 * Only unicast messages, no groupcastmessage<br>
 * Does not need or use GroupDefiner<br>
 * No user interface<br>
 */
package br.com.meslin.main;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
public class MainEcho {
	private ModelApplication model;
	private Swap swap;
	private ObjectMapper objectMapper;

	/**
	 * Constructor
	 */
	public MainEcho() {
        this.objectMapper = new ObjectMapper();
        this.swap = new Swap(objectMapper);
		this.model = new ModelApplication() {
			@SuppressWarnings("rawtypes")
			@Override
			public void recordReceived(ConsumerRecord record) {
		        this.logger.info("Record Received " + record.value().toString());
		        String texto = new String((byte[])record.value(), StandardCharsets.UTF_8); 	// Conteúdo da mensagem em byte array
		        sendUnicastMessage((String)record.key(), texto);
			}
		};
	}

	/**
	 * Main function
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		// check for all environment variable needed
		/*
		System.err.println(System.getenv("app.consumer.topics"));
		System.err.println(System.getenv("app.consumer.auto.offset.reset"));
		System.err.println(System.getenv("app.consumer.bootstrap.servers"));
		System.err.println(System.getenv("app.consumer.group.id"));
		System.err.println(System.getenv("app.producer.bootstrap.servers"));
		System.err.println(System.getenv("app.producer.retries"));
		System.err.println(System.getenv("app.producer.enable.idempotence"));
		System.err.println(System.getenv("app.producer.linger.ms"));
		System.err.println(System.getenv("app.producer.acks"));
		*/
		Map<String,String> env = new HashMap<String, String>();
		env.putAll(System.getenv());
		if(System.getenv("app.consumer.topics") == null) env.put("app.consumer.topics", "AppModel"); 
		if(System.getenv("app.consumer.auto.offset.reset") == null) env.put("app.consumer.auto.offset.reset", "latest"); 
		if(System.getenv("app.consumer.bootstrap.servers") == null) env.put("app.consumer.bootstrap.servers", "127.0.0.1:9092"); 
		if(System.getenv("app.consumer.group.id") == null) env.put("app.consumer.group.id", "gw-consumer"); 
		if(System.getenv("app.producer.bootstrap.servers") == null) env.put("app.producer.bootstrap.servers", "127.0.0.1:9092"); 
		if(System.getenv("app.producer.retries") == null) env.put("app.producer.retries", "3"); 
		if(System.getenv("app.producer.enable.idempotence") == null) env.put("app.producer.enable.idempotence", "true"); 
		if(System.getenv("app.producer.linger.ms") == null) env.put("app.producer.linger.ms", "1"); 
		if(System.getenv("app.producer.acks") == null) env.put("app.producer.acks", "all"); 
		try {
			StaticLibrary.setEnv(env);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		try {
			if(System.getenv("app.consumer.topics") == null) StaticLibrary.setEnv("app.consumer.topics", "AppModel");
			if(System.getenv("app.consumer.auto.offset.reset") == null) StaticLibrary.setEnv("app.consumer.auto.offset.reset", "latest");
			if(System.getenv("app.consumer.bootstrap.servers") == null) StaticLibrary.setEnv("app.consumer.bootstrap.servers", "127.0.0.1:9092");
			if(System.getenv("app.consumer.group.id") == null) StaticLibrary.setEnv("app.consumer.group.id", "gw-consumer");
			if(System.getenv("app.producer.bootstrap.servers") == null) StaticLibrary.setEnv("app.producer.bootstrap.servers", "127.0.0.1:9092");
			if(System.getenv("app.producer.retries") == null) StaticLibrary.setEnv("app.producer.retries", "3");
			if(System.getenv("app.producer.enable.idempotence") == null) StaticLibrary.setEnv("app.producer.enable.idempotence", "true");
			if(System.getenv("app.producer.linger.ms") == null) StaticLibrary.setEnv("app.producer.linger.ms", "1");
			if(System.getenv("app.producer.acks") == null) StaticLibrary.setEnv("app.producer.acks", "all");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println(System.getenv("app.consumer.topics"));
		System.err.println(System.getenv("app.consumer.auto.offset.reset"));
		System.err.println(System.getenv("app.consumer.bootstrap.servers"));
		System.err.println(System.getenv("app.consumer.group.id"));
		System.err.println(System.getenv("app.producer.bootstrap.servers"));
		System.err.println(System.getenv("app.producer.retries"));
		System.err.println(System.getenv("app.producer.enable.idempotence"));
		System.err.println(System.getenv("app.producer.linger.ms"));
		System.err.println(System.getenv("app.producer.acks"));
		*/
		
		new MainEcho();
	}
	
	/**
	 * sendUnicastMessage<br>
	 * Sends a unicast message<br>
	 * @param uuid destination UUID
	 * @param message message text
	 */
	private void sendUnicastMessage(String uuid, String message) {
		try {
			model.sendRecord(model.createRecord("PrivateMessageTopic", uuid, swap.SwapDataSerialization(createSwapData(message))));
		}
		catch (Exception e) {
			e.printStackTrace();
            model.logger.error("Error SendPrivateMessage", e);
		}
	}
	
    /**
     * createSwapData<br>
     * create a SwapData
     * 
     * @return a serialized SwapData object
     */
    public SwapData createSwapData(String data) {
        byte[] content = data.getBytes(StandardCharsets.UTF_8);
        SwapData serializableData = new SwapData();
        serializableData.setMessage(content);
        serializableData.setDuration(60);
        return serializableData;
    }
}
