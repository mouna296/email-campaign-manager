package com.untitled.ecm.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Dak {
    private int id;
    private String subject;
    private String content;
    private String contentType;
    private DakEmail replyTo;
    private DakEmail from;
    private String creator;
    private String mailType;
}
