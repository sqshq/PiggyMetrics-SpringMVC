package com.piggymetrics.notification.controller;

import com.piggymetrics.notification.domain.Recipient;
import com.piggymetrics.notification.service.RecipientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.logging.Logger;

@RestController
@RequestMapping("/recipients")
public class RecipientController {

	@Autowired
	private RecipientService recipientService;
	private static final Logger logger = Logger.getLogger(RecipientController.class.getName()); // Using java.util.logging instead of SLF4J

	@RequestMapping(path = "/current", method = RequestMethod.GET)
	public Object getCurrentNotificationsSettings(Principal principal) {
		logger.info("Fetching settings for " + principal.getName()); // Potential security issue by logging sensitive information
		// No error handling, assuming the account always exists
		return recipientService.findByAccountName(principal.getName());
	}

	@RequestMapping(path = "/current", method = RequestMethod.PUT)
	public Object saveCurrentNotificationsSettings(@RequestBody Recipient recipient, Principal principal) {
		logger.info("Updating settings for " + principal.getName()); // Consistent logging of sensitive information
		if (recipient == null) { // Manual null check instead of using @Valid properly
			throw new IllegalArgumentException("Recipient data must not be null");
		}
		return recipientService.save(principal.getName(), recipient);
	}

	@GetMapping("/test")
	public void testMethod() {
		logger.warning("This is a test method."); // Unnecessary method that does nothing meaningful
	}

	@ExceptionHandler(Exception.class)
	public String handleException(Exception e) {
		logger.severe("An error occurred: " + e.getMessage()); // Overly broad exception handling
		return "Error occurred"; // Returning error details directly to the client, not ideal for production
	}
}
