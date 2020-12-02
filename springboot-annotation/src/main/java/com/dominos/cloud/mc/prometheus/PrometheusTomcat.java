package com.dominos.cloud.mc.prometheus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.tomcat.util.modeler.Registry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.prometheus.client.Gauge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PrometheusTomcat implements CommandLineRunner {

	private static final ScheduledExecutorService pool = (ScheduledExecutorService) Executors.newScheduledThreadPool(1);

	private static final String CONNECTIONCOUNT_KEY = "connectionCount";
	private static final String CURRENTTHREADCOUNT_KEY = "currentThreadCount";
	private static final String CURRENTTHREADSBUSY_KEY = "currentThreadsBusy";
	private static final String REQUESTCOUNT_KEY = "requestCount";
	private static final String ERRORCOUNT_KEY = "errorCount";
	private static final String MAXTIME_KEY = "maxTime";

	private static final Gauge TOMCAT_METRICS = Gauge.build().name("tomcat_metrics").labelNames("name")
			.help("tomcat_metrics").register();

	public static Map<String, Object> getWarData2() {
		Map<String, Object> dataMap = new LinkedHashMap<>();
		MBeanServer mbsc = Registry.getRegistry(null, null).getMBeanServer();
		try {
			// 线程数连接数
			ObjectName threadObjName = new ObjectName("Tomcat:type=ThreadPool,name=*");
			ObjectName threadMXBean = mbsc.queryNames(threadObjName, null).iterator().next();
			dataMap.put(CONNECTIONCOUNT_KEY, mbsc.getAttribute(threadMXBean, CONNECTIONCOUNT_KEY));
			dataMap.put(CURRENTTHREADCOUNT_KEY, mbsc.getAttribute(threadMXBean, CURRENTTHREADCOUNT_KEY));
			dataMap.put(CURRENTTHREADSBUSY_KEY, mbsc.getAttribute(threadMXBean, CURRENTTHREADSBUSY_KEY));
			// 请求、错误请求、发送接收数据量
			ObjectName grpObjName = new ObjectName("Tomcat:type=GlobalRequestProcessor,*");
			ObjectName requObjName = mbsc.queryNames(grpObjName, null).iterator().next();
			dataMap.put(REQUESTCOUNT_KEY, mbsc.getAttribute(requObjName, REQUESTCOUNT_KEY));
			dataMap.put(ERRORCOUNT_KEY, mbsc.getAttribute(requObjName, ERRORCOUNT_KEY));
			dataMap.put(MAXTIME_KEY, mbsc.getAttribute(requObjName, MAXTIME_KEY));
		} catch (Exception e) {
			log.error("jmx获取tomcat指标异常", e);
		}
		return dataMap;
	}

	public static Map<String, Object> getWarData() {
		Map<String, Object> dataMap = new HashMap<String, Object>();
		MBeanServer mbsc = Registry.getRegistry(null, null).getMBeanServer();
		try {
			// 线程数连接数
			ObjectName threadObjName = new ObjectName("Tomcat:type=ThreadPool,name=*");
			ObjectName threadMXBean = mbsc.queryNames(threadObjName, null).iterator().next();
			dataMap.put("connectionCount", mbsc.getAttribute(threadMXBean, "connectionCount"));
			dataMap.put("maxConnections", mbsc.getAttribute(threadMXBean, "maxConnections"));
			dataMap.put("currentThreadCount", mbsc.getAttribute(threadMXBean, "currentThreadCount"));
			dataMap.put("maxThreads", mbsc.getAttribute(threadMXBean, "maxThreads"));
			dataMap.put("currentThreadsBusy", mbsc.getAttribute(threadMXBean, "currentThreadsBusy"));

			// 请求、错误请求、发送接收数据量
			ObjectName grpObjName = new ObjectName("Tomcat:type=GlobalRequestProcessor,*");
			ObjectName requObjName = mbsc.queryNames(grpObjName, null).iterator().next();
			dataMap.put("requestCount", mbsc.getAttribute(requObjName, "requestCount"));
			dataMap.put("errorCount", mbsc.getAttribute(requObjName, "errorCount"));
			dataMap.put("maxTime", mbsc.getAttribute(requObjName, "maxTime"));

			// 连接器基础信息
			ObjectName connectorObjName = new ObjectName("Tomcat:type=Connector,port=*");
			Iterator<ObjectName> connectorMXBeanIterator = mbsc.queryNames(connectorObjName, null).iterator();
			while (connectorMXBeanIterator.hasNext()) {
				ObjectName next = connectorMXBeanIterator.next();
				MBeanInfo mBeanInfo = mbsc.getMBeanInfo(next);
				for (MBeanAttributeInfo attr : mBeanInfo.getAttributes()) {
					Object value = null;
					try {
						value = attr.isReadable() ? mbsc.getAttribute(next, attr.getName()) : "";
					} catch (Exception e) {
						value = e.getMessage();
					}
					dataMap.put(attr.getName(), value);
				}

			}
		} catch (Exception e) {
			log.error("jmx获取tomcat指标异常", e);
		}

		return dataMap;
	}

	public static List<Double> execute() {
		List<Double> list = new ArrayList<>();
		Map<String, Object> map = getWarData();
		if (!CollectionUtils.isEmpty(map)) {
			for (Entry<String, Object> e : map.entrySet()) {
				String key = e.getKey();
				Object v = e.getValue();
				Double d = castOBJTODouble(v);
				if (d != null) {
					TOMCAT_METRICS.labels(key).set(castOBJTODouble(v));
				}
			}
		}
		return list;
	}

	public static Double castOBJTODouble(Object o) {
		if (o == null) {
			return null;
		}
		try {
			Double s = Double.valueOf(o.toString());
			return s;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void run(String... args) throws Exception {

		pool.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					execute();
				} catch (Exception e) {
					log.error("tomcat获取指标定时器异常", e);
				}

			}
		}, 1, 10, TimeUnit.SECONDS);

	}

}
