package com.untitled.ecm.services.mail;

import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dtos.CampaignEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**********************************************************************************************
 *
 * BEWARE!, this test cases for this codepath does not exist, every change to this class
 * must be validated by running a instance locally (environment variable in config yaml must not be "TEST")
 * and then hitting sendgrid spam-me endpoint or campaign get demo mail
 * to be extra sure verify via execute campaign now call
 *
 **********************************************************************************************/
@Slf4j
class SendGridAPICaller implements Runnable {
    private final List<Mail> mailsYetToBeSend;
    private final SendGrid sendGrid;
    private final LogDAO logDAO;
    private final CampaignEvent campaignEvent;
    private List<String> errors;

    SendGridAPICaller(SendGrid sendGrid,
                      List<Mail> mailsYetToBeSend,
                      List<String> errors,
                      LogDAO logDAO,
                      CampaignEvent campaignEvent) {
        if (sendGrid == null || mailsYetToBeSend == null || campaignEvent == null) {
            throw new InstantiationError("null value provided");
        }
        this.sendGrid = sendGrid;
        this.mailsYetToBeSend = mailsYetToBeSend;
        // continue adding errors to list of sendgridmailer
        this.errors = errors;
        this.logDAO = logDAO;
        this.campaignEvent = campaignEvent;

        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
    }

    @Override
    public void run() {
        int retryCount;
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        Response response;
        boolean mailSent;
        boolean mailCannotBeSend;

        final int MAX_RETRY_ATTEMPTS_ALLOWED = 10;
        // this will be random to reduce the possibility of threads starting at same time and waiting for same time
        int waitPerdiodMillis;

        // prohibited adding of new recipients(sendGridMailer.canAddRecipient()) in order to avoid any side-effects from modification of list from outside

        for (Mail mail : this.mailsYetToBeSend) {
            retryCount = 0;
            mailSent = false;
            mailCannotBeSend = false;

            try {
                request.setBody(mail.build());

                while (retryCount < MAX_RETRY_ATTEMPTS_ALLOWED) {

                    retryCount += 1;

                    response = this.sendGrid.api(request);

                    switch (response.getStatusCode()) {
                        case 200:
                            mailSent = true;
                            break;
                        case 202:
                            mailSent = true;
                            break;
                        case 429:
                            // take it easy, sendgrid is angry
                            waitPerdiodMillis = ThreadLocalRandom.current().nextInt(1, 5) * 1000;
                            Thread.sleep(waitPerdiodMillis);
                            break;
                        case 500:
                            // hope sendgrid fixes its internal stuff
                            waitPerdiodMillis = ThreadLocalRandom.current().nextInt(5, 120) * 1000;
                            Thread.sleep(waitPerdiodMillis);
                            break;
                        case 503:
                            // hope sendgrid comes up online
                            waitPerdiodMillis = ThreadLocalRandom.current().nextInt(30, 120) * 1000;
                            Thread.sleep(waitPerdiodMillis);
                            break;
                        default:
                            mailCannotBeSend = true;
                            break;
                    }

                    if (mailSent) {
                        this.campaignEvent.setMails_sent(this.campaignEvent.getMails_sent() + mail.getPersonalization().size());
                        break;
                    }
                    if (mailCannotBeSend) {
                        this.errors.add("non-fixable error occurred, see statuscode, response headers and request body for details. StatusCode: "
                                + Integer.toString(response.getStatusCode()) + ". Response Headers: " + response.getHeaders() + ". Request Body: " + request.getBody());
                        break;
                    }
                }
            } catch (Exception e) {
                this.errors.add(e.getMessage() + "|| Request Body: " + request.getBody());
            }
        }

        if (this.errors.size() > 0) {
            this.campaignEvent.setRemark(JSONObject.valueToString(this.errors));
        } else {
            this.campaignEvent.setRemark("Campaign execution finished successfully");
        }

        try {
            log.info(new ObjectMapper().writeValueAsString(campaignEvent));
        } catch (JsonProcessingException e) {
            log.error("could not convert campaign event to json", e);
        }

        try {
            if (this.logDAO != null) {
                this.logDAO.saveCampaignEvent(this.campaignEvent);
            }
        } catch (Exception e) {
            log.error("Could not save campaign events to db. Causing: " + e.getMessage());
        }
    }

}
