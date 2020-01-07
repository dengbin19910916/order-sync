package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.purcotton.omni.ordersync.data.ErrorRepository;
import com.purcotton.omni.ordersync.data.LogRepository;
import com.purcotton.omni.ordersync.data.OrderRepository;
import com.purcotton.omni.ordersync.data.ScheduleRepository;
import com.purcotton.omni.ordersync.domain.Error;
import com.purcotton.omni.ordersync.domain.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class Synchronizer implements InterruptableJob {

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Thread currentThread;

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext context) {
        currentThread = Thread.currentThread();

        if (log.isDebugEnabled()) {
            log.debug("Job started.");
        }

        var jobDataMap = context.getMergedJobDataMap();
        var applicationContext = (ApplicationContext) jobDataMap.get("applicationContext");
        var property = (Property) jobDataMap.get("property");

        var scheduleRepository = applicationContext.getBean(ScheduleRepository.class);
        var logRepository = applicationContext.getBean(LogRepository.class);

        var schedule = scheduleRepository
                .findFirstByPropertyAndCompletedOrderByStartTime(property, false);

        schedule.ifPresent(value -> {
            Log log = null;
            try {
                log = pullAndSave(value, property, applicationContext);

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
                            ApplicationContext applicationContext) {
        var orderRepository = applicationContext.getBean(OrderRepository.class);
        var errorRepository = applicationContext.getBean(ErrorRepository.class);
        var restTemplate = applicationContext.getBean(RestTemplate.class);

        StopWatch pullWatch = new StopWatch("PullWatch");
        StopWatch saveWatch = new StopWatch("SaveWatch");

        pullWatch.start();
        var pageInfo = getPage(restTemplate, property, schedule);
        pullWatch.stop();

        AtomicInteger totalNumber = new AtomicInteger();
        AtomicInteger succeedNumber = new AtomicInteger();
        AtomicInteger failedNumber = new AtomicInteger();

        int pageNumber = Objects.requireNonNull(pageInfo).getTotalPages();  // 页数从0开始
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

                    Error error = new Error(schedule, datum.toJSONString(), e.getMessage());
                    errorRepository.save(error);

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
            String pageUrl = UrlType.PAGE.getParameterUrl(property, schedule, null);
            return restTemplate.exchange(pageUrl, property.getHttpMethod(),
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
            String dataUrl = UrlType.DATA.getParameterUrl(property, schedule, pageNumber);
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
        order.setCreatedTime(LocalDateTime.parse(getJsonValue(datum, property.getCreatedTimePath()), FORMATTER));
        order.setUpdatedTime(LocalDateTime.parse(getJsonValue(datum, property.getUpdatedTimePath()), FORMATTER));
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

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        if (currentThread == null) {
            throw new UnableToInterruptJobException("Current thread cannot be null.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Job interrupt.");
        }
        currentThread.interrupt();
    }

    private enum UrlType {
        PAGE,
        DATA;

        public String getParameterUrl(Property property, Schedule schedule, Integer pageNumber) {
            Map<String, Object> paramsMap = new HashMap<>() {{
                put("#startTime", schedule.getStartTime().format(FORMATTER));
                put("#endTime", schedule.getEndTime().format(FORMATTER));
                put("#pageSize", property.getPageSize());
                put("#pageNumber", pageNumber);
                put("#shopCode", property.getShopCode());
            }};

            String path = this == PAGE ? property.getPagePath() : property.getDataPath();
            List<String> existsParams =  Stream.of(PARAMS)
                    .filter(path::contains)
                    .collect(Collectors.toList());
            for (String existsParam : existsParams) {
                path = path.replaceAll(existsParam, paramsMap.get(existsParam).toString());
            }

            return property.getHost() + "/" + path;
        }

        private static final String[] PARAMS = new String[]{
                "#startTime", "#endTime",
                "#pageSize", "#pageNumber",
                "#shopCode"
        };
    }
}
