package com.untitled.ecm.services.mail;

import com.untitled.ecm.constants.EnvironmentType;
import org.apache.commons.lang3.StringUtils;

public class MailerFactory {

    public static Mailer getMailer(EnvironmentType environmentType, String sendGridDomainApiKey) {
        switch (environmentType) {
            case PRODUCTION:
                if (StringUtils.isEmpty(sendGridDomainApiKey) || StringUtils.isWhitespace(sendGridDomainApiKey)) {
                    throw new RuntimeException("null sendgrid api key provided");
                }
                return new SendGridMailer(sendGridDomainApiKey);
            case TEST:
                return new InMemoryMailer();
            default:
                throw new RuntimeException("unknown environment type " + environmentType.toString());
        }
    }

}
