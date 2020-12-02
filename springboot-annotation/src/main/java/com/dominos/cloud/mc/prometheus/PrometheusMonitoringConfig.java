package com.dominos.cloud.mc.prometheus;

import org.apache.catalina.manager.StatusManagerServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;

@Configuration
class PrometheusMonitoringConfig {

	@Bean
	ServletRegistrationBean<?> servletRegistrationBean() {
		DefaultExports.initialize();
		return new ServletRegistrationBean<>(new MetricsServlet(), "/prometheus");
	}

	@Bean
	public ServletRegistrationBean<?> registerServlet() {
		ServletRegistrationBean<?> servletRegistrationBean = new ServletRegistrationBean<>(new StatusManagerServlet(),
				"/status");
		return servletRegistrationBean;
	}
}