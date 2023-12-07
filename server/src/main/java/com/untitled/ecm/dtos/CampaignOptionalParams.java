package com.untitled.ecm.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CampaignOptionalParams {
    @NonNull
    Integer perUserMailLimit;
    @NonNull
    Integer campaignRepeatThreshold;
    @NonNull
    String campaignCategory;
    @NonNull
    Integer chunkCount;
    @NonNull
    Integer mailsPerChunk;
    @NonNull
    Integer delayPerChunkInMinutes;
}
