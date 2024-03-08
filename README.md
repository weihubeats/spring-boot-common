# spring-boot-nebula

# 模块说明
- [spring-boot-nebula-common](spring-boot-nebula-common) 基础工具组件
- [spring-boot-nebula-aggregate](spring-boot-nebula-aggregate) ddd聚合根组件
- [spring-boot-nebula-dependencies](spring-boot-nebula-dependencies) 统一依赖
- [spring-boot-nebula-samples](spring-boot-nebula-samples) 使用示例
- [spring-boot-nebula-web](spring-boot-nebula-web) web封装组件(包括统一异常返回，简化返回，自定义异常报警)

> 每个模块可单独引用使用，不耦合

##  解决问题
1. 统一公司所有`spring boot`项目的依赖管理 
 
不使用`common`可能存在的问题: 
- a项目使用了 redission 3.14 b项目 使用3.61,然后导致相同代码可能运行结果不一致
- 统一使用`web-spring-boot-start`模块或者`boot-common-parent`可解决不同项目依赖版本不一致问题。
在`boot-common-parent`管理公司的所有依赖，以后应用项目无需手动指定各种依赖版本只需引用依赖即可，统一在`boot-common-parent`管理即可
4. 提供开箱即用的`web-spring-boot-start`模块，解决web开发需要手动封装工具类的痛点


## demo
使用参考 [spring-boot-nebula-samples](spring-boot-nebula-samples)模块

### [spring-boot-nebula-web](spring-boot-nebula-web) 使用

1. 引入依赖
```xml
 <dependency>
    <groupId>io.github.weihubeats</groupId>
    <artifactId>spring-boot-nebula-web</artifactId>
    <version>0.0.01</version>
</dependency>
```

1. 运行[Application.java](spring-boot-nebula-samples%2Fspring-boot-nebula-web-sample%2Fsrc%2Fmain%2Fjava%2Fcom%2Fnebula%2Fweb%2Fsample%2FApplication.java)
2. 运行 [http-test-controller.http](spring-boot-nebula-samples%2Fspring-boot-nebula-web-sample%2Fsrc%2Fmain%2Fhttp%2Fhttp-test-controller.http)中的`GET localhost:8088/test`

原先web项目需要使用的返回值比如`Response<T> ss`;
```java
    @GetMapping("/test")
    public Response<String> test() {
        return Response.success("小奏");
    }
```

现在不需要将自己的返回对象包裹起来,只需要添加注解`@NebulaResponseBody`
```java
    @GetMapping("/test")
    @NebulaResponseBody
    public String test() {
        return "小奏";
    }
```
返回结果
```json
{
  "code": 200,
  "data": "小奏",
  "msg": "success"
}
```

#### 提供开箱即用的分页对象
- [NebulaPage.java](spring-boot-nebula-web%2Fsrc%2Fmain%2Fjava%2Fcom%2Fnebula%2Fweb%2Fboot%2Fapi%2FNebulaPage.java)
- [NebulaPageQuery.java](spring-boot-nebula-web%2Fsrc%2Fmain%2Fjava%2Fcom%2Fnebula%2Fweb%2Fboot%2Fapi%2FNebulaPageQuery.java)

使用

#### 提供时间戳自动转`LocalDateTime`注解
@GetTimestamp

```java
    @GetMapping("/test")
    @NebulaResponseBody
    public String test(@GetTimestamp LocalDateTime time) {
        return time.toString();
    }
```