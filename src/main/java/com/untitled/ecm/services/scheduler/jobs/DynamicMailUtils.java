package com.untitled.ecm.services.scheduler.jobs;

import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dtos.Dak;
import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Slf4j
public class DynamicMailUtils {

    public static Map<String, Map<String, Object>> getDynamicMailData(List<Map<String, Object>> resultSet, @NonNull final Dak dak) {
        if (resultSet == null || dak.getContent() == null) {
            throw new AssertionError("null value received");
        }

        Map<String, Map<String, Object>> dynamicMailData = new HashMap<>();


        final String recipientColumnName = "email";
        String recipient;
        final List<String> vars = getTemplateVars(dak);

        for (Map<String, Object> row : resultSet) {
            try {
                recipient = (String) row.get(recipientColumnName);
            } catch (Exception e) {
                log.warn("error while reading " + recipientColumnName + " column", e);
                continue;
            }

            if (!StringUtils.isEmpty(recipient) && !dynamicMailData.containsKey(recipient)) {
                HashMap<String, Object> data = new HashMap<>();
                for (String var : vars) {
                    if (!row.containsKey(var)) {
                        break;
                    }
                    final Object value = row.get(var);
                    if (value == null) {
                        break;
                    }
                    data.put(var, value);
                }
                if (isAllDataAvailableForThisRecipient(vars, data)) {
                    dynamicMailData.put(recipient, data);
                } else {
                    log.warn("mail to recipient " +
                            DakiyaUtils.getObfuscatedEmail(recipient) +
                            " could not find all the data to populate dynamic mail");
                }
            }
        }
        return dynamicMailData;
    }

    private static List<String> getTemplateVars(@NonNull final Dak dak) {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory();

        Mustache mustache = mustacheFactory.compile(new StringReader(dak.getContent()), "dynamic_mail_content");

        List<String> vars = extractTemplateVars(mustache);

        mustache = mustacheFactory.compile(new StringReader(dak.getSubject()), "dynamic_mail_subject");

        vars.addAll(extractTemplateVars(mustache));
        return vars;
    }

    private static List<String> extractTemplateVars(@NonNull final Mustache mustache) {
        List<String> vars = new ArrayList<>();
        for (Code code : mustache.getCodes()) {
            if (code.getName() != null) {
                vars.add(code.getName());
            }
        }
        return vars;
    }

    public static String compileMustache(String mailTemplate, Map<String, Object> data) {
        final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile(new StringReader(mailTemplate), UUID.randomUUID().toString());
        return compileMustache(mustache, data);
    }

    static String compileMustache(Mustache compiledMailTemplate, Map<String, Object> data) {
        StringWriter stringWriter = new StringWriter();
        compiledMailTemplate.execute(stringWriter, data);
        return stringWriter.toString();
    }


    private static boolean isAllDataAvailableForThisRecipient(List<String> vars, HashMap<String, Object> data) {
        for (String var : vars) {
            if (!data.containsKey(var) || data.get(var) == null) {
                return false;
            }
        }
        return true;
    }
}
