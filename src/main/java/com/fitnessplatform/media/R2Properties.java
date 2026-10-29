package com.fitnessplatform.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;

@Validated
@ConfigurationProperties("media.r2")
public record R2Properties(String accountId, String accessKeyId, String secretAccessKey, String bucketName,
                           Duration uploadTtl, Duration getTtl) {
    public R2Properties {
        uploadTtl = uploadTtl == null ? Duration.ofMinutes(5) : uploadTtl;
        getTtl = getTtl == null ? Duration.ofMinutes(10) : getTtl;
    }
    @AssertTrue(message = "R2 signed URL TTLs must be between one second and fifteen minutes")
    public boolean isTtlValid() {
        return validTtl(uploadTtl) && validTtl(getTtl);
    }
    private boolean validTtl(Duration ttl) {
        return ttl.compareTo(Duration.ofSeconds(1)) >= 0 && ttl.compareTo(Duration.ofMinutes(15)) <= 0;
    }
    public boolean configured() {
        return accountId != null && accountId.matches("[a-fA-F0-9]{32}")
                && nonblank(accessKeyId) && nonblank(secretAccessKey) && nonblank(bucketName);
    }
    private boolean nonblank(String value) { return value != null && !value.isBlank(); }
    // Do not use the generated record toString: configuration contains credentials.
    @Override public String toString() { return "R2Properties[credentials redacted]"; }
}
