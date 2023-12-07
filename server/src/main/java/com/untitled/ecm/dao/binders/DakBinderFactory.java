package com.untitled.ecm.dao.binders;

import com.untitled.ecm.dtos.DakV2;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;

import java.lang.annotation.Annotation;

public class DakBinderFactory implements BinderFactory {

    @Override
    public Binder build(Annotation annotation) {
        return new Binder<BindDak, DakV2>() {
            @Override
            public void bind(SQLStatement<?> q, BindDak bind, DakV2 dak) {
                q.bind("subject", dak.getSubject());
                q.bind("content", dak.getContent());
                q.bind("content_type", dak.getContentType());
                q.bind("mail_type", dak.getMailType());
                q.bind("from_name", dak.getFrom().getName());
                q.bind("from_email", dak.getFrom().getEmail());
                q.bind("reply_to_name", dak.getReplyTo().getName());
                q.bind("reply_to_email", dak.getReplyTo().getEmail());
                q.bind("creator", dak.getCreator());
            }
        };
    }
}
