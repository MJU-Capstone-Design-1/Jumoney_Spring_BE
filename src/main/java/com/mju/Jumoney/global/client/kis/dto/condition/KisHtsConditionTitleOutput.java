package com.mju.Jumoney.global.client.kis.dto.condition;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisHtsConditionTitleOutput(
        @JsonProperty("user_id") String userId,
        @JsonProperty("seq") String seq,
        @JsonProperty("grp_nm") String groupName,
        @JsonProperty("condition_nm") String conditionName
) {
}
