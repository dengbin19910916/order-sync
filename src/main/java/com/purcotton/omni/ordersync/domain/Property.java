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

    @Column(length = 30)
    private String orderType = "";

    @Column(length = 10)
    private String shopCode = "";

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "datetime")
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
    @Column(columnDefinition = "integer")
    private Integer startTimeOffset;
    /**
     * 同步时间延时（默认60秒）
     */
    @Column(columnDefinition = "integer")
    private Integer delay;
    /**
     * 每页大小。
     */
    @Column(columnDefinition = "integer")
    private Integer pageSize;

    @Column(columnDefinition = "integer")
    private Integer triggerInterval;

    @Column(length = 100)
    private String host;

    @Column(length = 300)
    private String pagePath;

    @Column(length = 300)
    private String dataPath;

    @Column(length = 30)
    private String tokenName;

    @Column(length = 300)
    private String tokenValue;

    @Column(length = 300)
    private String tokenPath;

    @Column(length = 100)
    private String cidPath;

    @Column(length = 100)
    private String tidPath;

    @Column(length = 100)
    private String ridPath;

    @Column(length = 100)
    private String createdTimePath;

    @Column(length = 100)
    private String updatedTimePath;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdTime = LocalDateTime.now();

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedTime = LocalDateTime.now();

    @JSONField(serialize = false)
    @OneToMany(mappedBy = "property")
    private List<Schedule> schedules;

    @JSONField(serialize = false)
    @OneToMany(mappedBy = "property")
    private List<OmniOrder> orders;

    @JSONField(serialize = false)
    public String getBeanName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
                Stream.of(orderChannel, orderType, shopCode)
                        .filter(value -> !ObjectUtils.isEmpty(value))
                        .collect(Collectors.joining("_")));
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
