package org.example.account.account.infrastructure.configuration.bind;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class StoreMethodHandler implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(StoreId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String storeIdString = webRequest.getHeader("StoreId");
        if (storeIdString == null) return 0;
        int storeId = 0;
        try {
            storeId = Integer.parseInt(storeIdString);
        } catch (Exception ignore) {
        }
        return storeId;
    }
}