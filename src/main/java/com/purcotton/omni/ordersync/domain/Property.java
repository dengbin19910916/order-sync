package com.purcotton.omni.ordersync.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.CaseFormat;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"orderChannel", "orderType", "shopCode"})
)
@Entity
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String orderChannel;

    private String orderType = "";

    private String shopCode = "";

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originTime;

    private boolean enabled;
    /**
     * 时间区间的间隔大小（默认60秒）。
     */
    private Long timeInterval;
    /**
     * 时间向前的偏移量，主要是为了解决阿里订单的时延问题。
     * 如果同步的平台存在类似的问题的时候，可以选择覆盖此方法来缓解问题。
     */
    private Integer startTimeOffset;
    /**
     * 同步时间延时（默认60秒）
     */
    private Integer delay;
    /**
     * 每页大小。
     */
    private Integer pageSize;

    private Integer triggerInterval;

    private String host;

    private String pagePath;

    private String dataPath;

    private String tokenName;

    private String tokenValue;

    private String tokenPath;

    private String cidPath;

    private String tidPath;

    private String ridPath;

    private String createdTimePath;

    private String updatedTimePath;

    private LocalDateTime createdTime = LocalDateTime.now();

    private LocalDateTime updatedTime = LocalDateTime.now();

    @JSONField(serialize = false)
    @OneToMany(mappedBy = "property")
    private List<Schedule> schedules;

    @JSONField(serialize = false)
    public String getBeanName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
                Stream.of(orderChannel, orderType, shopCode)
                        .filter(value -> !ObjectUtils.isEmpty(value))
                        .collect(Collectors.joining("_")))
                + "Synchronizer";
    }

    @Override
    public String toString() {
        return "Property{" +
                "id=" + id +
                ", orderChannel='" + orderChannel + '\'' +
                ", orderType='" + orderType + '\'' +
                ", shopCode='" + shopCode + '\'' +
                ", originTime=" + originTime +
                ", enabled=" + enabled +
                ", timeInterval=" + timeInterval +
                ", startTimeOffset=" + startTimeOffset +
                ", delay=" + delay +
                ", pageSize=" + pageSize +
                ", triggerInterval=" + triggerInterval +
                ", host='" + host + '\'' +
                ", pagePath='" + pagePath + '\'' +
                ", dataPath='" + dataPath + '\'' +
                ", tokenName='" + tokenName + '\'' +
                ", tokenValue='" + tokenValue + '\'' +
                ", tokenPath='" + tokenPath + '\'' +
                ", cidPath='" + cidPath + '\'' +
                ", tidPath='" + tidPath + '\'' +
                ", ridPath='" + ridPath + '\'' +
                ", createdTimePath='" + createdTimePath + '\'' +
                ", updatedTimePath='" + updatedTimePath + '\'' +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}
