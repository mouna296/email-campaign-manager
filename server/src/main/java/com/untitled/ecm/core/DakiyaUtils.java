package com.untitled.ecm.core;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.dtos.CampaignEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sendgrid.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CharacterPredicate;
import org.apache.commons.text.RandomStringGenerator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;

@Slf4j
public class DakiyaUtils {

    // this is done this way for o(1) lookup
    private static ImmutableMap<String, Boolean> invalidEmailUserStrings = ImmutableMap.<String, Boolean>builder()
            .put("xyz", true)
            .put("abc", true)
            .put("aaa", true)
            .put("aaaa", true)
            .put("test", true)
            .put("1234", true)
            .put("123", true)
            .put("11111", true)
            .put("1111", true)
            .put("111", true)
            .put("11", true)
            .put("1", true)
            .put("lol", true)
            .build();

    // this is done this way for o(1) lookup
    private static ImmutableMap<String, Boolean> invalidDomains = ImmutableMap.<String, Boolean>builder()
            .put("gamil.com", true)
            .put("gamil.co", true)
            .put("aexp.com", true)
            .put("mailinator.com", true)
            .put("housat.com", true)
            .put("muchomail.com", true)
            .put("getitinfomedia.com", true)
            .put("bhupati.org", true)
            .put("gmsil.com", true)
            .put("askme.com", true)
            .put("dsd.com", true)
            .put("lafarge.com", true)
            .put("gmqil.com", true)
            .put("thyssenkrupp.com", true)
            .put("aranca.com", true)
            .put("prodapt.com", true)
            .put("fareportal.com", true)
            .put("nexteducation.in", true)
            .put("ctc-tcs.com", true)
            .put("mahatransco.in", true)
            .put("yum.com", true)
            .put("hashedin.com", true)
            .put("tcgls.com", true)
            .put("mssupport.microsoft.com", true)
            .put("rri.res.in", true)
            .put("thirdware.com", true)
            .put("gmaim.com", true)
            .put("fileitr.com", true)
            .put("hmie.co.in", true)
            .put("yhoo.co.in", true)
            .put("247-inc.com", true)
            .put("dh.com", true)
            .build();

    public static boolean isBadEmail(String email) {
        if (StringUtils.isWhitespace(email)) {
            return true;
        }

        String[] splits = StringUtils.split(email, "@");
        if (splits == null || splits.length != 2) {
            return true;
        }

        String emailUserStr = splits[0].toLowerCase();
        String domain = splits[1].toLowerCase();
        return (invalidEmailUserStrings.containsKey(emailUserStr) || invalidDomains.containsKey(domain));
    }

    public static String getBcryptHashedString(String str) {
        if (str == null) {
            return null;
        }
        return BCrypt.hashpw(str, BCrypt.gensalt());
    }

    public static DateTime getISTDateTime(final Date date) {
        return new DateTime(date).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
    }
    public static DateTime getStartDateTime(String ISO86041_StartAt) {
        DateTime startDateTime = DateTime.parse(ISO86041_StartAt).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
        DateTime currentDateTime = DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
        if (startDateTime.isAfter(currentDateTime)) {
            return startDateTime;
        } else {
            return currentDateTime.plusDays(1).withHourOfDay(startDateTime.getHourOfDay()).withMinuteOfHour(startDateTime.getMinuteOfHour());
        }

    }

    public static String getObfuscatedEmail(String plainEmail) {
        if (StringUtils.isEmpty(plainEmail)) {
            return "";
        }

        if (StringUtils.countMatches(plainEmail, "@") < 1) {
            return plainEmail;
        }

        String emailWithoutDomain = StringUtils.substringBeforeLast(plainEmail, "@");

        StringBuilder obfuscatedEmail = new StringBuilder();

        int i = 0;
        while (i < (int) Math.floor(emailWithoutDomain.length() / 2)) {
            obfuscatedEmail.append("x");
            i++;
        }
        while (i < emailWithoutDomain.length()) {
            obfuscatedEmail.append(emailWithoutDomain.charAt(i));
            i++;
        }
        obfuscatedEmail.append("@");
        obfuscatedEmail.append(StringUtils.substringAfterLast(plainEmail, "@"));
        return obfuscatedEmail.toString();
    }

    public static String getRandomAlphaNumericString() {
        return getRandomAlphaNumericString(DakiyaStrings.MIN_PASS_LENGHT);
    }


    public static String getRandomAlphaNumericString(int length) {
        if (length < DakiyaStrings.MIN_PASS_LENGHT) {
            length = DakiyaStrings.MIN_PASS_LENGHT;
        }

        // RandomString Utils is deprecated

        SecureRandom secureRandom = new SecureRandom();

        return new RandomStringGenerator
                .Builder()
                .withinRange('0', 'z')
                .filteredBy((CharacterPredicate) codePoint -> StringUtils.isAlphanumeric(new String(Character.toChars(codePoint))))
                .usingRandom(secureRandom::nextInt)
                .build()
                .generate(length);
    }

    public static void notifyCampaignExecution(final SendGrid sendgrid, final CampaignEvent campaignEvent) {
        if (campaignEvent.getCampaign_last_modified_by() == null) {
            throw new IllegalArgumentException("null campaign last modified by");
        }
        if (sendgrid == null) {
            throw new IllegalArgumentException("null sengrid object received");
        }

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        Mail mail = new Mail();
        mail.addCategory("dakiya-campaign-execution-notification");
        Personalization personalization = new Personalization();
        personalization.setSubject("Dakiya Campaign Execution Notification");

        personalization.addTo(new Email(campaignEvent.getCampaign_last_modified_by()));


        mail.addPersonalization(personalization);

        final Email from = new Email();
        from.setName("Dakiya " + campaignEvent.getDakiya_instance_type() + " Notifier");
        from.setEmail("dakiya." + campaignEvent.getDakiya_instance_type() + ".notifier@example.com");

        mail.setFrom(from);
        mail.setReplyTo(new Email("teamuntitled.272@gmail.com"));
        Content content = new Content();
        content.setType("text/plain");
        StringBuilder builder = new StringBuilder();
        builder.append("Campaign Execution Event Notification:\n");
        builder.append("Details:\n");
        try {
            builder.append(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(campaignEvent));
        } catch (JsonProcessingException e) {
            log.error("could not parse serialize campaign event", e);
        }

        content.setValue(builder.toString());
        mail.addContent(content);

        try {
            request.setBody(mail.build());
            Response response = sendgrid.api(request);
            log.info("dumping response received from sendgrid for sending notification mail");
            log.info(String.valueOf(response.getHeaders()));
            log.info(String.valueOf(response.getBody()));
        } catch (IOException e) {
            log.error("could not send notification email", e);
        }
    }

    public static boolean isEmptyOrWhitespace(final String input) {
        return StringUtils.isEmpty(input) || StringUtils.isWhitespace(input);
    }
}
