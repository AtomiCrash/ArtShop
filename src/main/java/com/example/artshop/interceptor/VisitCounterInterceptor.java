package com.example.artshop.interceptor;

import com.example.artshop.service.VisitCounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VisitCounterInterceptor implements HandlerInterceptor {

    private VisitCounterService visitCounterService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String endpoint = request.getRequestURI();
            visitCounterService.recordVisit(endpoint);
        }
        return true;
    }
}