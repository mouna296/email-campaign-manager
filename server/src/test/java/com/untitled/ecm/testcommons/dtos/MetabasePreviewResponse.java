package com.untitled.ecm.testcommons.dtos;

import lombok.Data;

import java.util.List;

@Data
public class MetabasePreviewResponse {
    int queryResultCount;
    List<String> previewEmails;
}

