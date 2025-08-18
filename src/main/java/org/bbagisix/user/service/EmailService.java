package org.bbagisix.user.service;

import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;  // 👈 이것으로 변경

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username:noreply@dondothat.com}")
	private String fromEmail;

	public void sendVerificationCode(String toEmail, String verificationCode) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(toEmail);
			message.setSubject("DonDoThat 이메일 인증코드");

			String emailContent = String.format(
				"안녕하세요!\n\n" +
					"DonDoThat 회원가입을 위한 인증코드입니다.\n\n" +
					"인증코드: %s\n\n" +
					"이 코드는 5분 후에 만료됩니다.\n\n",
				verificationCode
			);

			message.setText(emailContent);
			mailSender.send(message);

		} catch (Exception e) {
			throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
		}
	}

	public String generateVerificationCode() {
		return String.format("%06d", (int)(Math.random() * 1000000));
	}
}
