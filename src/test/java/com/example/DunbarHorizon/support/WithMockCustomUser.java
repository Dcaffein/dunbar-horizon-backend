package com.example.DunbarHorizon.support;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String id() default "1";
    String role() default "ROLE_USER";
}