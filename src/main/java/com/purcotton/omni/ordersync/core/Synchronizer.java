package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.purcotton.omni.ordersync.data.LogRepository;
import com.purcotton.omni.ordersync.data.OrderRepository;
import com.purcotton.omni.ordersync.data.ScheduleRepository;
import com.purcotton.omni.ordersync.domain.Log;
import com.purcotton.omni.ordersync.domain.OmniOrder;
import com.purcotton.omni.ordersync.domain.Property;
import com.purcotton.omni.ordersync.domain.Schedule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class Synchronizer implements Job {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext context) {
        var jobDataMap = context.getMergedJobDataMap();
        var applicationContext = (ApplicationContext) jobDataMap.get("applicationContext");
        var property = (Property) jobDataMap.get("property");

        var scheduleRepository = applicationContext.getBean(ScheduleRepository.class);
        var orderRepository = applicationContext.getBean(OrderRepository.class);
        var logRepository = applicationContext.getBean(LogRepository.class);
        var restTemplate = applicationContext.getBean(RestTemplate.class);

        var schedule = scheduleRepository
                .findFirstByPropertyAndCompletedOrderByStartTime(property, false);

        schedule.ifPresent(value -> {
            Log log = null;
            try {
                log = pullAndSave(value, property, restTemplate, orderRepository);

                value.setCompleted(true);
                value.setUpdatedTime(LocalDateTime.now());
                scheduleRepository.save(value);
            } catch (Exception e) {
                log = buildLog(value, e);
                throw e;
            } finally {
                if (log != null) {
                    logRepository.save(log);
                }
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("{} [{} - {}] synced. ", property.getOrderChannel(),
                    property.getOrderType(), property.getShopCode());
        }
    }

    private Log pullAndSave(Schedule schedule, Property property,
                            RestTemplate restTemplate, OrderRepository orderRepository) {
        StopWatch pullWatch = new StopWatch("PullWatch");
        StopWatch saveWatch = new StopWatch("SaveWatch");

        pullWatch.start();
        var pageInfo = getPage(restTemplate, property, schedule);
        pullWatch.stop();

        AtomicInteger totalNumber = new AtomicInteger();
        AtomicInteger succeedNumber = new AtomicInteger();
        AtomicInteger failedNumber = new AtomicInteger();

        int pageNumber = Objects.requireNonNull(pageInfo).getTotalPages();
        while (pageNumber-- > 0) {
            pullWatch.start();
            List<JSONObject> data = getData(restTemplate, property, schedule, pageNumber);
            pullWatch.stop();

            var now = LocalDateTime.now();

            saveWatch.start();
            Objects.requireNonNull(data).parallelStream().forEach(datum -> {
                try {
                    var order = buildOrder(datum, property, now);
                    var oldOrder = orderRepository.findByCid(
                            getJsonValue(datum, property.getCidPath()));
                    if (oldOrder.isPresent()) {
                        if (!order.getUpdatedTime().isAfter(oldOrder.get().getUpdatedTime())) {
                            return;
                        }
                        order.setId(oldOrder.get().getId());
                        order.setSyncUpdatedTime(now);
                    }
                    orderRepository.save(order);

                    succeedNumber.getAndIncrement();
                    totalNumber.getAndIncrement();
                } catch (Exception e) {
                    e.printStackTrace();
                    failedNumber.getAndIncrement();
                    totalNumber.getAndIncrement();
                    throw new SyncException("Save data failed", e);
                }
            });
            saveWatch.stop();
        }

        return new Log(schedule, totalNumber.get(), succeedNumber.get(), failedNumber.get(),
                pullWatch.getTotalTimeSeconds(), saveWatch.getTotalTimeSeconds());
    }

    private PageInfo getPage(RestTemplate restTemplate, Property property, Schedule schedule) {
        try {
            String pageUrl = getParameterUrl(property, schedule, property.getPagePath(), null);
            return restTemplate.exchange(pageUrl, HttpMethod.GET,
                    new HttpEntity<>(null, headers(property.getTokenName(), property.getTokenValue())),
                    new ParameterizedTypeReference<PageInfo>() {
                    })
                    .getBody();
        } catch (Exception e) {
            log.error("Get page failed.", e);
            throw new SyncException("Get page failed", e);
        }
    }

    private HttpHeaders headers(String tokenName, String tokenValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(tokenName, tokenValue);
        return headers;
    }

    private List<JSONObject> getData(RestTemplate restTemplate, Property property, Schedule schedule, int pageNumber) {
        try {
            String dataUrl = getParameterUrl(property, schedule, property.getDataPath(), pageNumber);
            return restTemplate.exchange(dataUrl, HttpMethod.GET,
                    new HttpEntity<>(null, headers(property.getTokenName(), property.getTokenValue())),
                    new ParameterizedTypeReference<List<JSONObject>>() {
                    })
                    .getBody();
        } catch (Exception e) {
            log.error("Get data failed.", e);
            throw new SyncException("Get data failed", e);
        }
    }

    private OmniOrder buildOrder(JSONObject datum, Property property, LocalDateTime now) {
        OmniOrder order = new OmniOrder();
        order.setProperty(property);
        order.setCid(getJsonValue(datum, property.getCidPath()));
        order.setTid(getJsonValue(datum, property.getTidPath()));
        if (!ObjectUtils.isEmpty(property.getRidPath())) {
            order.setRid(getJsonValue(datum, property.getRidPath()));
        }
        order.setData(datum.toString());
        order.setCreatedTime(LocalDateTime.parse(getJsonValue(datum, property.getCreatedTimePath()), formatter));
        order.setUpdatedTime(LocalDateTime.parse(getJsonValue(datum, property.getUpdatedTimePath()), formatter));
        order.setSyncCreatedTime(now);
        order.setSyncUpdatedTime(now);
        return order;
    }

    private String getJsonValue(JSONObject object, String path) {
        try {
            return JSONPath.eval(object, path).toString();
        } catch (Exception e) {
            log.error("Build order failed.", e);
            throw new SyncException("Build order failed", e);
        }
    }

    private Log buildLog(Schedule value, Exception e) {
        Log log = new Log();
        log.setSchedule(value);
        log.setMessage(e.getLocalizedMessage());
        log.setCreatedTime(LocalDateTime.now());
        return log;
    }

    private String getParameterUrl(Property property, Schedule schedule,
                                   String path, Integer pageNumber) {
        String result = String.format(getUrl(property.getHost(), path)
                        + "?startTime=%s&endTime=%s&pageSize=%d&size=%d",
                schedule.getStartTime().format(formatter), schedule.getEndTime().format(formatter),
                property.getPageSize(), property.getPageSize());
        if (!ObjectUtils.isEmpty(property.getShopCode())) {
            result += "&shopCode=" + property.getShopCode();
        }
        if (pageNumber != null) {
            result += "&pageNumber=" + pageNumber + "&page=" + pageNumber;
        }
        return result;
    }

    private String getUrl(String host, String path) {
        return host + "/" + path;
    }

}
