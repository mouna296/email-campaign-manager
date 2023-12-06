package com.untitled.ecm.core;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

// this class is workaround for https://github.com/dropwizard/dropwizard/issues/898
public class DakiyaUserAccessLogger implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        return;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String auth = httpServletRequest.getHeader("Authorization");
            if (auth != null && auth.startsWith("Basic")) {
                String base64 = auth.substring(6).trim();
                String cred = new String(Base64.getDecoder().decode(base64), Charset.forName("UTF-8"));
                Logger logger = LoggerFactory.getLogger(DakiyaUserAccessLogger.class);
                logger.info(
                        cred.split(":")[0]
                                + " accessed "
                                + httpServletRequest.getMethod()
                                + " "
                                + httpServletRequest.getRequestURI()
                                + " | remote address : " + httpServletRequest.getRemoteAddr()
                                + " | remote host " + httpServletRequest.getRemoteHost()
                );
            }
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.BAD_REQUEST_400);
            httpResponse.getWriter().print("Bad request, only http allowed");
            chain.doFilter(request, httpResponse);
        }
    }

    @Override
    public void destroy() {
        return;
    }
}
