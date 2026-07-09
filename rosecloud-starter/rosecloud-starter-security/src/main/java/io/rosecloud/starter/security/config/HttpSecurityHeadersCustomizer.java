 package io.rosecloud.starter.security.config;
 
 import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
 
 @FunctionalInterface
 public interface HttpSecurityHeadersCustomizer {
 
     void customize(HeadersConfigurer<?> headers);
 
     static HttpSecurityHeadersCustomizer noop() {
         return headers -> {};
     }
 }
