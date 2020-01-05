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
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
            try {
                pullAndSave(value, property, restTemplate, orderRepository);
            } catch (Exception e) {
                Log log = buildLog(value, e);
                logRepository.save(log);
            }
        });

        if (log.isDebugEnabled()) {
            log.info("{} synced. ", property.getOrderChannel());
        }
    }

    private void pullAndSave(Schedule schedule, Property property,
                             RestTemplate restTemplate, OrderRepository orderRepository) {
        var pageInfo = getPage(restTemplate, property, schedule);

        int pageNumber = Objects.requireNonNull(pageInfo).getTotalPages();
        while (pageNumber-- > 0) {
            List<JSONObject> data = getData(restTemplate, property, schedule, pageNumber);

            var now = LocalDateTime.now();
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
                } catch (Exception e) {
                    throw new SyncException("Save data failed", e);
                }
            });
        }
    }

    private PageInfo getPage(RestTemplate restTemplate, Property property, Schedule schedule) {
        try {
            String pageUrl = getParameterUrl(property.getHost(), property.getPagePath(),
                    schedule.getStartTime().format(formatter), schedule.getEndTime().format(formatter),
                    property.getPageSize(), null);
            return restTemplate.exchange(RequestEntity.get(new URI(pageUrl))
                            .header(property.getTokenName(), getUrl(property.getHost(), property.getTokenPath()))
                            .build(),
                    new ParameterizedTypeReference<PageInfo>() {
                    })
                    .getBody();
        } catch (Exception e) {
            throw new SyncException("Get page failed", e);
        }
    }

    @SneakyThrows
    private List<JSONObject> getData(RestTemplate restTemplate, Property property, Schedule schedule, int pageNumber) {
        try {
            String dataUrl = getParameterUrl(property.getHost(), property.getPagePath(),
                    schedule.getStartTime().format(formatter), schedule.getEndTime().format(formatter),
                    property.getPageSize(), pageNumber);
            return restTemplate.exchange(RequestEntity.get(new URI(dataUrl))
                            .header(property.getTokenName(), getUrl(property.getHost(), property.getTokenPath()))
                            .build(),
                    new ParameterizedTypeReference<List<JSONObject>>() {
                    })
                    .getBody();
        } catch (Exception e) {
            throw new SyncException("Get data failed", e);
        }
    }

    private OmniOrder buildOrder(JSONObject datum, Property property, LocalDateTime now) {
        OmniOrder order = new OmniOrder();
        order.setCid(getJsonValue(datum, property.getCidPath()));
        order.setTid(getJsonValue(datum, property.getTidPath()));
        order.setRid(getJsonValue(datum, property.getRidPath()));
        order.setData(datum.toString());
        order.setCreatedTime(LocalDateTime.parse(getJsonValue(datum, property.getCreatedTimePath()), formatter));
        order.setUpdatedTime(LocalDateTime.parse(getJsonValue(datum, property.getUpdatedTimePath()), formatter));
        order.setSyncCreatedTime(now);
        order.setSyncUpdatedTime(now);
        return order;
    }

    private String getJsonValue(JSONObject object, String path) {
        return JSONPath.eval(object, path).toString();
    }

    private Log buildLog(Schedule value, Exception e) {
        Log log = new Log();
        log.setSchedule(value);
        log.setMessage(e.getLocalizedMessage());
        log.setCreatedTime(LocalDateTime.now());
        return log;
    }

    private String getParameterUrl(String host, String path, String startTime, String endTime,
                                   Integer pageSize, Integer pageNumber) {
        String result = String.format(getUrl(host, path) + "?startTime=%s&endTime=%s&pageSize=%d",
                startTime, endTime, pageSize);
        if (pageNumber != null) {
            result += "&pageNumber=" + pageNumber;
        }
        return result;
    }

    private String getUrl(String host, String path) {
        return host + "/" + path;
    }

}
