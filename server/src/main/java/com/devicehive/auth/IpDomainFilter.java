package com.devicehive.auth;

import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class IpDomainFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(IpDomainFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String address = request.getRemoteAddr();
        InetAddress inetAddress = InetAddress.getByName(address);
        ThreadLocalVariablesKeeper.setClientIP(inetAddress);
        if (request instanceof HttpServletRequest) {
            String url = ((HttpServletRequest) request).getRequestURL().toString();
            String method = ((HttpServletRequest) request).getMethod();
            logger.debug("Current thread : {}. URI : {}, Method : {}", Thread.currentThread().getName(), url, method);
            HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);
            String canonicalHostName = httpServletRequest.getHeader(HttpHeaders.ORIGIN);
            ThreadLocalVariablesKeeper.setHostName(canonicalHostName);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
