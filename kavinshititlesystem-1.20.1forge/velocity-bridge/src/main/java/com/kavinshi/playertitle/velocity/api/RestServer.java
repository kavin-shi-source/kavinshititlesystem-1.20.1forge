package com.kavinshi.playertitle.velocity.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kavinshi.playertitle.velocity.service.HeadingService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RestServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestServer.class);
    private final Javalin app;
    private final HeadingService headingService;
    private final Algorithm algorithm;

    public RestServer(int port, String jwtSecret, HeadingService headingService) {
        this.headingService = headingService;
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        
        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(port);

        setupRoutes();
    }

    private void setupRoutes() {
        app.before("/api/*", ctx -> {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).result("Missing or invalid token");
                return;
            }
            String token = authHeader.substring(7);
            try {
                DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
                String role = jwt.getClaim("role").asString();
                if (!"ROLE_WEB_ADMIN".equals(role) && !"ROLE_SUPER_ADMIN".equals(role)) {
                    ctx.status(403).result("Insufficient permissions");
                } else {
                    ctx.attribute("adminId", jwt.getSubject());
                }
            } catch (Exception e) {
                ctx.status(401).result("Invalid token");
            }
        });

        app.post("/api/v1/headings/grant", this::handleGrant);
        app.post("/api/v1/headings/revoke", this::handleRevoke);
    }

    private void handleGrant(Context ctx) {
        GrantRequest req = ctx.bodyAsClass(GrantRequest.class);
        String adminIdStr = ctx.attribute("adminId");
        if (adminIdStr == null) {
            ctx.status(401);
            return;
        }
        
        UUID adminId = UUID.fromString(adminIdStr);
        UUID targetId = UUID.fromString(req.targetUuid);
        
        CompletableFuture<Boolean> future = headingService.grantHeading(adminId, targetId, req.heading);
        ctx.future(() -> future.thenAccept(success -> {
            if (success) {
                ctx.status(200).json(new ApiResponse(true, "Heading granted"));
            } else {
                ctx.status(500).json(new ApiResponse(false, "Internal error"));
            }
        }));
    }

    private void handleRevoke(Context ctx) {
        GrantRequest req = ctx.bodyAsClass(GrantRequest.class);
        String adminIdStr = ctx.attribute("adminId");
        if (adminIdStr == null) {
            ctx.status(401);
            return;
        }
        
        UUID adminId = UUID.fromString(adminIdStr);
        UUID targetId = UUID.fromString(req.targetUuid);
        
        CompletableFuture<Boolean> future = headingService.revokeHeading(adminId, targetId);
        ctx.future(() -> future.thenAccept(success -> {
            if (success) {
                ctx.status(200).json(new ApiResponse(true, "Heading revoked"));
            } else {
                ctx.status(500).json(new ApiResponse(false, "Internal error"));
            }
        }));
    }

    public void stop() {
        app.stop();
    }

    public static class GrantRequest {
        public String targetUuid;
        public String heading;
    }

    public static class ApiResponse {
        public boolean success;
        public String message;
        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}