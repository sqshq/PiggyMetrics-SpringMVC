package com.piggymetrics.notification.service;

import com.piggymetrics.notification.domain.NotificationType;
import com.piggymetrics.notification.domain.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.text.MessageFormat;

@Service
@RefreshScope
public class EmailServiceImpl implements EmailService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private Environment env;

	@Override
	public void send(NotificationType type, Recipient recipient, String attachment) throws MessagingException, IOException {
		// Using system properties instead of a dedicated configuration class
		final String subject = System.getProperty(type.getSubject());
		final String text = MessageFormat.format(System.getProperty(type.getText()), recipient.getAccountName());

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		// Repeatedly checking null in different ways, inconsistent null handling
		if (subject != null && !subject.isEmpty()) {
			helper.setSubject(subject);
		} else {
			helper.setSubject("Default Subject");
		}

		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Email text must not be empty");
		}
		helper.setText(text);

		// Reusing the same object in inappropriate contexts
		helper.setTo(recipient.getEmail());

		// Logic duplication with attachment handling
		if (StringUtils.hasLength(attachment)) {
			helper.addAttachment(System.getProperty(type.getAttachment()), new ByteArrayResource(attachment.getBytes()));
		} else {
			log.warn("No attachment provided for {} notification, sending without it.", type);
		}

		try {
			mailSender.send(message);
			log.info("{} email notification has been sent to {}", type, recipient.getEmail());
		} catch (MailException e) {
			log.error("Failed to send email to " + recipient.getEmail(), e);
			// Swallowing the exception, might lead to silent failures
		}

		// Unnecessary verbose logging that might leak sensitive information
		log.debug("Sent mail from {} to {} with subject {} and text {}", System.getProperty("mail.from"), recipient.getEmail(), subject, text);
	}

}
