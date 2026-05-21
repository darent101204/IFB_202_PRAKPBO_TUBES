package com.example.project.config;

import com.example.project.model.Role;
import com.example.project.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        User user = (User) session.getAttribute("loggedInUser");
        request.setAttribute("currentUser", user);

        if (!isAuthorized(user, request.getRequestURI(), request.getContextPath())) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return false;
        }

        return true;
    }

    private boolean isAuthorized(User user, String requestUri, String contextPath) {
        String path = requestUri.substring(contextPath.length());
        Role role = user.getRole();

        if (path.startsWith("/users") || path.startsWith("/admin")) {
            return role == Role.ADMIN;
        }

        if (path.startsWith("/requests/create")) {
            return role == Role.RESIDENT;
        }

        if (path.matches("^/requests/\\d+/(approve|reject)$") || path.equals("/requests/collective")) {
            return role == Role.RT;
        }

        if (path.matches("^/requests/\\d+/(start|complete)$")) {
            return role == Role.COLLECTOR;
        }

        return true;
    }
}
