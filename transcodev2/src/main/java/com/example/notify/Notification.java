package com.example.notify;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.RequestOptions;
import com.azure.core.util.BinaryData;
import com.azure.messaging.webpubsub.WebPubSubServiceClient;
import com.azure.messaging.webpubsub.WebPubSubServiceClientBuilder;
import com.azure.messaging.webpubsub.models.WebPubSubContentType;

public class Notification {

	WebPubSubServiceClient webPubSubServiceClient = new WebPubSubServiceClientBuilder()
            .credential(new AzureKeyCredential("t3CdUQAziUdUcpa48bfhOMxTOYHYo+9NxJ43fvVqKro="))
            .endpoint("https://teams.microsoft.com/l/message/19:7c5ca95907bd4b3288f8c837f3383d0d@thread.v2/1689571384303?context=%7B%22contextType%22%3A%22chat%22%7D")
            .hub("Queue")
            .buildClient();

	public void sendToHub(String Id, String status) {
		WebPubSubServiceClient webPubSubServiceClient = new WebPubSubServiceClientBuilder()
				.credential(new AzureKeyCredential("t3CdUQAziUdUcpa48bfhOMxTOYHYo+9NxJ43fvVqKro="))
				.endpoint(
						"https://teams.microsoft.com/l/message/19:7c5ca95907bd4b3288f8c837f3383d0d@thread.v2/1689571384303?context=%7B%22contextType%22%3A%22chat%22%7D")
				.hub("Queue").buildClient();

		String message = "Id = ".concat(Id).concat(" , ").concat("Status = ").concat(status);
		webPubSubServiceClient.sendToAll(message, WebPubSubContentType.TEXT_PLAIN);

//		webPubSubServiceClient.sendToConnection("Endpoint=https://transcodejobs-mq.webpubsub.azure.com;AccessKey=t3CdUQAziUdUcpa48bfhOMxTOYHYo+9NxJ43fvVqKro=;Version=1.0;", status, null)

	}
}
