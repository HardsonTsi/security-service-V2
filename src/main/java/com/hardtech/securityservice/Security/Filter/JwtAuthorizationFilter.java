package com.hardtech.securityservice.Security.Filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hardtech.securityservice.Security.JWTUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
        response.addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type," +
                " Access-Control-Request-Method, Access-Control-Request-Headers, authorization, username, roles");
        response.addHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin, Access-Control-Allow-Credentials, authorization, username, roles");

        // Recuperation du token du Header
        if (request.getServletPath().equals("/refreshToken")) {
            filterChain.doFilter(request, response);
        }
        else if(request.getMethod().equals("OPTIONS")){
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else {
            String authorizationToken = request.getHeader(JWTUtils.AUTH_HEADER);

            if (authorizationToken != null && authorizationToken.startsWith(JWTUtils.PREFIX)) {
                try {
                    String jwt = authorizationToken.substring("Bearer ".length());

                    // Decodage
                    Algorithm algorithm = Algorithm.HMAC256(JWTUtils.SECRET);
                    JWTVerifier jwtVerifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = jwtVerifier.verify(jwt);

                    // Recuperation des informations
                    String username = decodedJWT.getSubject();
                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);

                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    // Authentifier l'uilisateur
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    // Passer au filtre suivant
                    filterChain.doFilter(request, response);
                } catch (Exception e) {
                    response.setHeader("error-message", e.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
//                    response.sendError(HttpStatus.FORBIDDEN.value());
                }

            } else {
                filterChain.doFilter(request, response);
            }
        }

    }
}
