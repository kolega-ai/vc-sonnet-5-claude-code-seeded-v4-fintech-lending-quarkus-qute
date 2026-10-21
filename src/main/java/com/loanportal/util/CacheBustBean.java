package com.loanportal.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/** Exposes {@link NonCryptoUtil#cacheBustSuffix()} to Qute templates as cdi:cacheBust.suffix. */
@ApplicationScoped
@Named("cacheBust")
public class CacheBustBean {

    // Computed once per application start - a non-cryptographic cache-busting token is
    // intentionally stable for the process lifetime, not per-request.
    private final String suffix = NonCryptoUtil.cacheBustSuffix();

    public String getSuffix() {
        return suffix;
    }
}
