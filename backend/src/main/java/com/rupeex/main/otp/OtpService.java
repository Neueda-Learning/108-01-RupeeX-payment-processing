package com.rupeex.main.otp;

import com.rupeex.main.notification.template.OtpEmailTemplateBuilder;
import com.rupeex.main.repository.AccountsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final JavaMailSender mailSender;
    private final OtpEmailTemplateBuilder templateBuilder;
    private final AccountsRepository accountsRepository;

    @Value("${notification.mail.from}")
    private String fromEmail;

    // email (lowercase) -> OtpEntry
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public OtpService(JavaMailSender mailSender,
                      OtpEmailTemplateBuilder templateBuilder,
                      AccountsRepository accountsRepository) {
        this.mailSender = mailSender;
        this.templateBuilder = templateBuilder;
        this.accountsRepository = accountsRepository;
    }

    /**
     * Generates a 4-digit OTP, stores it, and sends it to the given email.
     * The account number is used to resolve the recipient's name for the email greeting.
     *
     * @param email         Recipient email address (from the account record)
     * @param accountNumber Source account number (used only to look up the holder name)
     */
    public void generateAndSend(String email, String accountNumber) {
        String otp = String.format("%04d", 1000 + random.nextInt(9000));
        store.put(email.toLowerCase(), new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));

        // Resolve the account holder's name for a personalised greeting (best-effort)
        String holderName = accountsRepository
                .findByAccountNumber(accountNumber)
                .map(a -> a.getAccountHolder())
                .orElse(null);

        sendOtpEmail(email, otp, holderName);
    }

    /**
     * Verifies the OTP for the given email.
     * Returns true if the OTP matches and has not expired.
     * The entry is removed on a successful verification.
     */
    public boolean verify(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        if (!entry.otp().equals(otp)) {
            return false;
        }
        store.remove(email.toLowerCase());
        return true;
    }

    private void sendOtpEmail(String toEmail, String otp, String recipientName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[RupeeX] Your Payment OTP");
        message.setText(templateBuilder.buildOtpBody(recipientName, otp, OTP_EXPIRY_MINUTES));
        mailSender.send(message);
    }

    // Internal record to hold OTP + expiry
    private record OtpEntry(String otp, LocalDateTime expiresAt) {}
}
