package com.muserver.samples;

import io.muserver.ContentTypes;
import io.muserver.MuRequest;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.rest.Authorizer;
import io.muserver.rest.BasicAuthSecurityFilter;
import io.muserver.rest.RestHandlerBuilder;
import io.muserver.rest.UserPassAuthenticator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.muserver.samples.model.Reservation;
import com.muserver.samples.service.ReservationService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ComponentScan(basePackages = "com.muserver.samples.service")
@Configuration
public class MuServerApp {

    private static final Map<String, Map<String, List<String>>> usersToPasswordToRoles = new HashMap<>();

    public static void main(String[] args) {
    	//Receive the user details from backend
    	 usersToPasswordToRoles.put("Naga", singletonMap("pwd123!", asList("Customer", "Owner")));
         usersToPasswordToRoles.put("Suresh", singletonMap("pwd123", asList("Owner")));

         MyUserPassAuthenticator authenticator = new MyUserPassAuthenticator(usersToPasswordToRoles);
         MyAuthorizer authorizer = new MyAuthorizer();

        ApplicationContext ctx= new AnnotationConfigApplicationContext(MuServerApp.class);
        ReservationService rs = ctx.getBean(ReservationService.class);
        UserResource userResource = new UserResource(rs);
        MuServer server = MuServerBuilder.httpServer().withExceptionHandler((request, response, exception) -> {
            if (response.hasStartedSendingData()) return false; 
            if (exception instanceof NotAuthorizedException) return false;
            response.contentType(ContentTypes.TEXT_PLAIN_UTF8);
            response.write(exception.getMessage());
            return true;
        })
            .addHandler(RestHandlerBuilder.restHandler(userResource)
                    .addRequestFilter(new BasicAuthSecurityFilter("My-App", authenticator, authorizer))
            .addCustomWriter(new JacksonJaxbJsonProvider())
            .addCustomReader(new JacksonJaxbJsonProvider()))
           
            
            
            .start();

        System.out.println("Server Started: " + server.uri());

    }
    static class MyUserPassAuthenticator implements UserPassAuthenticator {
        private final Map<String, Map<String, List<String>>> usersToPasswordToRoles;

        public MyUserPassAuthenticator(Map<String, Map<String, List<String>>> usersToPasswordToRoles) {
            this.usersToPasswordToRoles = usersToPasswordToRoles;
        }

        @Override
        public Principal authenticate(String username, String password) {
            Principal principal = null;
            Map<String, List<String>> user = usersToPasswordToRoles.get(username);
            if (user != null) {
                List<String> roles = user.get(password);
                if (roles != null) {
                    principal = new MyUser(username, roles);
                }
            }
            return principal;
        }
    }
    private static class MyUser implements Principal {
        private final String name;
        private final List<String> roles;
        private MyUser(String name, List<String> roles) {
            this.name = name;
            this.roles = roles;
        }
        @Override
        public String getName() {
            return name;
        }
        public boolean isInRole(String role) {
            return roles.contains(role);
        }
    }

    static class MyAuthorizer implements Authorizer {
        @Override
        public boolean isInRole(Principal principal, String role) {
            if (principal == null) {
                return false;
            }
            MyUser user = (MyUser)principal;
            return user.isInRole(role);
        }
    }

    @Path("/reservation/app")
    public static class UserResource {

        ReservationService rs=null;
        public UserResource(ReservationService rs) {
            this.rs = rs;
        }

        @POST
        @Consumes("application/json")
        @Produces("text/plain")
        public Response reserveRestaurent(@Context SecurityContext securityContext,Reservation request)throws Exception{
        	 if (!securityContext.isUserInRole("User")) {
                 throw new ClientErrorException("This requires an User role", 403);
             }
        	rs.book(request);
            return Response.status(201).build();
        }
        
        @GET
        @Consumes("application/json")
        @Produces("application/json")
        public String reservationsList(@Context SecurityContext securityContext,@QueryParam("date") String date)throws Exception{
        	 if (!securityContext.isUserInRole("Owner")) {
                 throw new ClientErrorException("This requires an Owner role", 403);
             }
        	List<Reservation> list = rs.getList(date);
        	JSONArray jsArray = new JSONArray(list);

        	return jsArray.toString();
        }
    }
}