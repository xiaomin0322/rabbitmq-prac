package com.dominos.cloud.mc.prometheus;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.prometheus.client.Summary;

/**
 * Controller记录失败信息
 * 
 * @author Zengmin.Zhang
 *
 */
@Aspect
@Component
public class PrometheusControllerAspect {

	@Pointcut("execution(public * com.dominos.cloud.*.controller.*.*(..))")
	public void controllerLog() {
	}

	// Note (1)
	private static final Summary responseTimeInMs = Summary.build().name("tracer_http_response_time_milliseconds")
			.labelNames("method", "uri", "status").help("tracer request completed time in milliseconds").register();

	// 针对所有Controller层的方法的切面
	@Around("controllerLog()")
	public Object doSurround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();

		long startTime = System.currentTimeMillis();
		// 方法的执行结果
		Object result = proceedingJoinPoint.proceed();
		long completedTime = System.currentTimeMillis() - startTime;
		String errCode = "00";
		responseTimeInMs.labels(request.getMethod(), getMethodSign(proceedingJoinPoint), errCode)
				.observe(completedTime);

		return result;
	}

	/**
	 * 获取唯一方法签名名称
	 * 
	 * @return
	 */
	public String getMethodSign(JoinPoint proceedingJoinPoint) {
		StringBuilder builder = new StringBuilder();
		String className = proceedingJoinPoint.getSignature().getDeclaringTypeName();
		String methodName = proceedingJoinPoint.getSignature().getName();
		String[] argNames = ((MethodSignature) proceedingJoinPoint.getSignature()).getParameterNames(); // 参数名
		builder.append(className).append(".").append(methodName);
		if (ArrayUtils.isNotEmpty(argNames)) {
			String argStr = StringUtils.join(argNames);
			builder.append("(");
			builder.append(argStr);
			builder.append(")");
		}
		return builder.toString();
	}

}