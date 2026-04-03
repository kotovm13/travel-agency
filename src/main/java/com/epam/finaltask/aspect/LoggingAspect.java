package com.epam.finaltask.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final String SERVICE_POINTCUT = "execution(* com.epam.finaltask.service.impl..*(..))";

    @Pointcut(SERVICE_POINTCUT)
    public void serviceLayer() {
        // pointcut definition
    }

    @Before("serviceLayer()")
    public void logMethodEntry(JoinPoint joinPoint) {
        if (log.isDebugEnabled()) {
            log.debug("Entering: {}.{}() with arguments: {}",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs()));
        }
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logMethodExit(JoinPoint joinPoint, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("Exiting: {}.{}() with result: {}",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    result);
        }
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        log.error("Exception in {}.{}(): {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }

    @Around("execution(* com.epam.finaltask.service.impl.OrderServiceImpl.orderVoucher(..))")
    public Object logOrderVoucher(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.info("Order requested: voucherId={}, username={}", args[0], args[1]);

        Object result = joinPoint.proceed();

        log.info("Order placed successfully: voucherId={}, username={}", args[0], args[1]);
        return result;
    }

    @Around("execution(* com.epam.finaltask.service.impl.OrderServiceImpl.cancelOrder(..))")
    public Object logCancelOrder(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.info("Cancel order requested: bookingId={}, username={}", args[0], args[1]);

        Object result = joinPoint.proceed();

        log.info("Order canceled successfully: bookingId={}, username={}", args[0], args[1]);
        return result;
    }

    @Around("execution(* com.epam.finaltask.service.impl.OrderServiceImpl.changeStatus(..))")
    public Object logChangeStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.info("Status change requested: bookingId={}, request={}", args[0], args[1]);

        Object result = joinPoint.proceed();

        log.info("Status changed successfully: bookingId={}", args[0]);
        return result;
    }

    @Around("execution(* com.epam.finaltask.service.impl.AuthenticationServiceImpl.register(..))")
    public Object logRegistration(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof com.epam.finaltask.dto.request.RegisterDTO registerDTO) {
            log.info("User registration requested: username={}", registerDTO.getUsername());
        }

        Object result = joinPoint.proceed();

        log.info("User registered successfully");
        return result;
    }

    @Around("execution(* com.epam.finaltask.service.impl.UserManagementServiceImpl.blockUser(..))")
    public Object logBlockUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.warn("User block requested: userId={}", args[0]);

        Object result = joinPoint.proceed();

        log.warn("User blocked: userId={}", args[0]);
        return result;
    }

    @Around("execution(* com.epam.finaltask.service.impl.UserManagementServiceImpl.changeRole(..))")
    public Object logChangeRole(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.warn("Role change requested: userId={}, request={}", args[0], args[1]);

        Object result = joinPoint.proceed();

        log.warn("Role changed: userId={}", args[0]);
        return result;
    }
}
