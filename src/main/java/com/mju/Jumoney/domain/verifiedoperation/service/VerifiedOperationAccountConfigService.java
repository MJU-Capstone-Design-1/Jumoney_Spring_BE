package com.mju.Jumoney.domain.verifiedoperation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerifiedOperationAccountConfigService {

    private static final String CONFIG_PATH = "data/verified_operation_accounts.json";

    private final ObjectMapper objectMapper;
    private List<VerifiedOperationAccountConfig> cachedAccounts;

    public List<VerifiedOperationAccountConfig> getAccounts() {
        if (cachedAccounts == null) {
            cachedAccounts = loadAccounts();
        }
        return cachedAccounts;
    }

    public VerifiedOperationAccountConfig getAccount(String accountCode) {
        return getAccounts().stream()
                .filter(account -> account.accountCode().equals(accountCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown verified operation accountCode: " + accountCode));
    }

    private List<VerifiedOperationAccountConfig> loadAccounts() {
        try {
            ClassPathResource resource = new ClassPathResource(CONFIG_PATH);
            return objectMapper.readValue(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8),
                    new TypeReference<List<VerifiedOperationAccountConfig>>() {
                    }
            );
        } catch (Exception e) {
            throw new IllegalStateException("모의 운용 계정 설정을 읽을 수 없습니다: " + CONFIG_PATH, e);
        }
    }
}
