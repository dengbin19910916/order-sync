# 外部配置定时任务

## 1. 添加任务

post: localhost:8080/properties

```json
{
	"orderChannel": "TB", 
	"orderType": "TRADE",
	"shopCode": "10000",
	"originTime": "2020-01-01 00:00:00",
	"enabled": true,
	"timeInterval": 60,
	"startTimeOffset": 0,
	"delay": 60,
	"pageSize": 1000,
	"triggerInterval": 3,
	"host": "https://47.92.253.135:3389/api",
	"pagePath": "/trades/pages",
	"dataPath": "/trades",
	"tokenName": "token",
	"tokenValue": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.e30.hofgCo40uCztlOGBSu8P5wpvrG8ahEpY4AyDLM6WgaY",
	"cidPath": "$.tid",
	"tidPath": "$.tid",
	"ridPath": null,
	"createdTimePath": "$.jdpCreated",
	"updatedTimePath": "$.jdpModified"
}
```

```json
{
	"orderChannel": "TB", 
	"orderType": "REFUND",
	"shopCode": "8524",
	"originTime": "2020-01-04 15:30:00",
	"enabled": true,
	"timeInterval": 60,
	"startTimeOffset": 0,
	"delay": 60,
	"pageSize": 1000,
	"triggerInterval": 5,
	"host": "https://47.92.253.135:3389/api",
	"pagePath": "/refunds/pages",
	"dataPath": "/refunds",
	"tokenName": "token",
	"tokenValue": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.e30.hofgCo40uCztlOGBSu8P5wpvrG8ahEpY4AyDLM6WgaY",
	"cidPath": "$.refundId",
	"tidPath": "$.tid",
	"ridPath": "$.refundId",
	"createdTimePath": "$.jdpCreated",
	"updatedTimePath": "$.jdpModified"
}
```