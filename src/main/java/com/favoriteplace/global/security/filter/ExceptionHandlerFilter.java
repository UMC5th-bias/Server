package com.favoriteplace.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.ErrorResponse;
import com.favoriteplace.global.exception.RestApiException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try{
            filterChain.doFilter(request, response);
        }catch (ExpiredJwtException e){
            //토큰의 유효기간 만료
            log.info("토큰 유효기간 만료");
            setErrorResponse(response, ErrorCode.TOKEN_NOT_VALID);
        }catch (JwtException | IllegalArgumentException e){
            //유효하지 않은 토큰
            log.info("유효하지 않은 토큰");
            setErrorResponse(response, ErrorCode.TOKEN_NOT_VALID);
        } catch (RestApiException e) {
            setErrorResponse(response, e.getErrorCode());
        }
    }
    private void setErrorResponse(
        HttpServletResponse response,
        ErrorCode errorCode
    ){
        ObjectMapper objectMapper = new ObjectMapper();
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        com.favoriteplace.global.exception.ErrorResponse errorResponse = ErrorResponse.of(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
        try{
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
