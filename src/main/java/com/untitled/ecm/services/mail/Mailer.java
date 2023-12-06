package com.untitled.ecm.services.mail;

import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dtos.CampaignEvent;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class Mailer {
    final static Map<String, String> mimes = ImmutableMap.of("html", "text/html");
    @Getter
    private List<String> errors = new ArrayList<>();

    static Optional<List<String>> validateEmail(String email) {
        final List<String> errors = new ArrayList<>();
        if (email == null) {
            errors.add("email cannot be null");
            return Optional.of(errors);
        } else {
            if (email.length() == 0) {
                errors.add("email cannot be empty");
            }
        }
        // changing to lower case
        email = email.toLowerCase();
        // todo improve checks
        if (StringUtils.countMatches(email, "@") != 1) {
            errors.add(email + ": email must have @");
        }
        if (errors.size() > 0) {
            return Optional.of(errors);
        }
        return Optional.empty();
    }

    static Optional<List<String>> validateNameOfDakEmail(String name) {
        final List<String> errors = new ArrayList<>();

        if (name == null) {
            // in this case only email was provided
            return Optional.empty();
        } else {
            if (name.length() == 0) {
                // if name is set then must not be something proper
                errors.add("name cannot be empty");
            }
        }
        if (name.contains(";") || name.contains(",") || name.contains(".")) {
            errors.add(name + ": name cannot contain ; , or .");
        }

        if (errors.size() > 0) {
            return Optional.of(errors);
        }
        return Optional.empty();
    }

    /**
     * collects all the errors and puts them in error list if any found
     *
     * @param dak dak to be validated
     * @return optinal of error list if any found
     */
    public static Optional<List<String>> validateDak(Dak dak) {
        // start fresh
        final List<String> errors = new ArrayList<>();

        if (dak == null) {
            errors.add("null dak provided, cannot validate");
            return Optional.of(errors);
        }

        String subject = dak.getSubject();
        if (subject == null) {
            errors.add("subject is null");
        } else {
            if (subject.length() == 0) {
                errors.add("subject string is empty");
            }
            if (subject.length() > 150) {
                log.warn("subject is longer than 150 chars");
            }
        }

        if (!mimes.containsKey(dak.getContentType())) {
            errors.add("invalid content type, allowed types: text or html");
        }

        if (dak.getContent() == null || dak.getContent().length() == 0) {
            errors.add("null or empty content string");
        }

        final DakEmail from = dak.getFrom();

        if (from == null) {
            errors.add("null from");
        } else {
            validateEmail(from.getEmail()).ifPresent(errors::addAll);

            if (from.getName() == null || from.getName().length() == 0) {
                errors.add("null or empty from-name");
            }
        }

        final DakEmail replyTo = dak.getReplyTo();

        if (replyTo == null) {
            errors.add("null replyTO");
        } else {
            validateEmail(replyTo.getEmail()).ifPresent(errors::addAll);
            if (replyTo.getName() == null || replyTo.getName().length() == 0) {
                errors.add("null or empty reply-to-name");
            }
        }
        if (errors.size() > 0) {
            return Optional.of(errors);
        }
        return Optional.empty();
    }

    public abstract void addMailContainer(Dak dak, Map<String, String> mailTrackingDetails);

    public abstract boolean addRecipient(DakEmail dakEmail);

    public abstract boolean addRecipients(List<DakEmail> dakEmails);

    public abstract boolean sendMails(LogDAO logDAO, CampaignEvent campaignEvent);
}
