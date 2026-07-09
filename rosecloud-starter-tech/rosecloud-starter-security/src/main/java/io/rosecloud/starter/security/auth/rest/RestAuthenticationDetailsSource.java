 package io.rosecloud.starter.security.auth.rest;
 
 import jakarta.servlet.http.HttpServletRequest;
 import org.springframework.security.authentication.AuthenticationDetailsSource;
 
 public class RestAuthenticationDetailsSource
         implements AuthenticationDetailsSource<HttpServletRequest, RestAuthenticationDetails> {
 
     @Override
     public RestAuthenticationDetails buildDetails(HttpServletRequest context) {
         return new RestAuthenticationDetails(context);
     }
 }
