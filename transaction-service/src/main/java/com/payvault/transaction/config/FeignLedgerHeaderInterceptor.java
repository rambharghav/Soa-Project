package com.payvault.transaction.config;

import com.payvault.transaction.context.LedgerContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignLedgerHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String txRef = LedgerContext.getTransactionRef();
        if (txRef != null && !txRef.isBlank()) {
            template.header("X-Transaction-Ref", txRef);
        }

        String desc = LedgerContext.getDescription();
        if (desc != null && !desc.isBlank()) {
            template.header("X-Description", desc);
        }
    }
}
