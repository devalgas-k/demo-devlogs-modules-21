package com.devalgas.productpage.simulation;

public final class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(ServiceName service) {
        super(service + " API is unavailable");
    }
}
