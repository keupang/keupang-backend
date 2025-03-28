package com.example.keupanguser.service;

import com.example.keupanguser.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body){
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); //html 이메일 지원가능

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send email",ex);
            throw new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50302,
                "이메일 전송에 실패 했습니다.",
                "잠시 후 시도하거나 고객센터에 문의해주시길 바랍니다.",
                "EMAIL_SENDING_FAILED");
        }
    }
}
