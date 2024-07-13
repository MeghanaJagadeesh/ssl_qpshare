package com.qp.quantum_share.webhook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

	private static final String VERIFY_TOKEN = "VERIFYTOKEN@123";
	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping
	public String verifyWebhook(@RequestParam("hub.mode") String mode, @RequestParam("hub.verify_token") String token,
			@RequestParam("hub.challenge") String challenge) {
		if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
			return challenge;
		} else {
			return "Verification failed";
		}
	}

//    @PostMapping
//    public void handleWebhook(@RequestBody String payload) {
//        try {
//            JsonNode jsonNode = objectMapper.readTree(payload);
//            if (jsonNode.has("entry")) {
//                JsonNode entries = jsonNode.get("entry");
//                for (JsonNode entry : entries) {
//                    // Process each entry
//                    System.out.println("Entry: " + entry);
//                    // Further processing based on the event type
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
	@PostMapping
	public void handleWebhook(@RequestBody String payload) {
		try {
			JsonNode jsonNode = objectMapper.readTree(payload);
			if (jsonNode.has("entry")) {
				JsonNode entries = jsonNode.get("entry");
				for (JsonNode entry : entries) {
					if (entry.has("changes")) {
						JsonNode changes = entry.get("changes");
						for (JsonNode change : changes) {
							String field = change.get("field").asText();
							if ("feed".equals(field)) {
								JsonNode value = change.get("value");
								String item = value.get("item").asText();
								String verb = value.get("verb").asText();
								String postId = value.get("post_id").asText();

								// Handle the feed change
								System.out.println("Received feed event: " + item + " " + verb + " for post " + postId);

								// Further processing based on the event type
								if ("post".equals(item)) {
									if ("add".equals(verb)) {
										// New post added
										String message = value.get("message").asText();
										System.out.println("New post: " + message);
									} else if ("edit".equals(verb)) {
										// Post edited
										System.out.println("Post edited: " + postId);
									}
								} else if ("comment".equals(item)) {
									// Handle comment events
									String commentId = value.get("comment_id").asText();
									if ("add".equals(verb)) {
										// New comment added
										String message = value.get("message").asText();
										System.out.println("New comment: " + message);
									} else if ("edit".equals(verb)) {
										// Comment edited
										System.out.println("Comment edited: " + commentId);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
