package com.project.GGDriveClone.resource;

import com.project.GGDriveClone.DTO.LoginRequest;
import com.project.GGDriveClone.DTO.ResponseCase;
import com.project.GGDriveClone.DTO.ServerResponseDto;
import com.project.GGDriveClone.entity.PlanEntity;
import com.project.GGDriveClone.entity.UserEntity;
import com.project.GGDriveClone.jwt.JwtTokenProvider;
import com.project.GGDriveClone.security.CustomAuthenticationProvider;
import com.project.GGDriveClone.security.CustomUserDetails;
import com.project.GGDriveClone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api")
public class LoginAPI {
    @Autowired
    private CustomAuthenticationProvider authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ServerResponseDto> authenticateUser(@RequestBody LoginRequest loginRequest) {

        // Xác thực từ username và password.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken((CustomUserDetails) authentication.getPrincipal());
            return ResponseEntity.ok(new ServerResponseDto(ResponseCase.SUCCESS, token));
        } else {
            return ResponseEntity.ok(new ServerResponseDto(ResponseCase.LOGIN_FAIL));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ServerResponseDto> register(@Valid @RequestBody UserEntity user, HttpServletRequest request)
            throws UnsupportedEncodingException, MessagingException {

        if(userService.register(user, getSiteURL(request)) == 1) {
            return ResponseEntity.ok(new ServerResponseDto(ResponseCase.SUCCESS));
        }

        if(userService.register(user, getSiteURL(request)) == 0) {
            return ResponseEntity.ok(new ServerResponseDto(ResponseCase.LACK_OF_INFORMATION));
        }

        if(userService.register(user, getSiteURL(request)) == 2) {
            return ResponseEntity.ok(new ServerResponseDto(ResponseCase.EXISTED_NAME_OR_EMAIL));
        }
        return ResponseEntity.ok(new ServerResponseDto(ResponseCase.ERROR));
    }


    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }


    @GetMapping("/verify")
    public void verifyUser(@Param("code") String code, HttpServletResponse response) throws IOException {
        if (userService.verify(code)) {
            response.sendRedirect("https://truong-352-drive-clone.herokuapp.com/sign-in");
        } else {
            response.sendError(0);
        }
    }
}
