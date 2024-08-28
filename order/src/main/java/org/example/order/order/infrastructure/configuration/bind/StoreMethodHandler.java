package org.example.order.order.infrastructure.configuration.bind;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
public class StoreMethodHandler implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(StoreId.class) != null;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        var header = webRequest.getHeader("StoreId");
        Integer storeId = null;
        try {
            storeId = header != null ? Integer.parseInt(header) : null;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (storeId == null) throw new Exception("required StoreId");
        return storeId;
    }
}
