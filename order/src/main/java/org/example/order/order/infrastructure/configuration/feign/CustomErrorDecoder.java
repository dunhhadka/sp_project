package org.example.order.order.infrastructure.configuration.feign;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import java.util.List;

public class CustomErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        var exception = FeignException.errorStatus(s, response);
        if (List.of(429, 504).contains(response.status())) {
            throw new RetryableException(
                    response.status(),
                    exception.getMessage(),
                    response.request().httpMethod(),
                    4L,
                    response.request()
            );
        }
        return exception;
    }
}
