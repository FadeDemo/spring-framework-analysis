# spring-beans

### 核心的几个类

* `BefaultListableBeanFactory`
* `XmlBeanDefinitionReader`
* `AbstarctBeanFactory`

### XML配置文件的读取流程

XML配置文件 ---> `Resource` ---> `Document` ---> `Element` ---> `BeanDefinition` 

### 获取bean的入口

`getBean()` 方法

### 循环依赖问题

在使用Spring框架时碰到循环依赖是很经常的事，目前主要碰到的有这几种：

* 构造器循环依赖（Spring不能解决）
* 单例setter依赖（Spring可以解决）
* 原型setter依赖（Spring不能解决）

这里描述下Spring是如何解决单例setter依赖的：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="testASingleton" class="org.fade.demo.springframework.beans.TestA">
        <property name="testB" ref="testBSingleton" />
    </bean>

    <bean id="testBSingleton" class="org.fade.demo.springframework.beans.TestB">
        <property name="testC" ref="testCSingleton" />
    </bean>

    <bean id="testCSingleton" class="org.fade.demo.springframework.beans.TestC">
        <property name="testA" ref="testASingleton" />
    </bean>

</beans>
```

首先创建 `testASingleton` -> 暴露 `ObjectFactory` ，将其加入三级缓存 `singletonFactories` -> 检测到依赖 `testBSingleton` ，创建 `testBSingleton` -> 暴露 `ObjectFactory` ，将其加入三级缓存 -> 检测到依赖 `testCSingleton` ，创建 `testCSingleton` -> 暴露 `ObjectFactory` ，将其加入三级缓存 -> 检测到依赖 `testASingleton` -> 从三级缓存中获取 `testASingleton` ，获取到后从三级缓存中移除它，并加入到二级缓存 `earlySingletonObjects` 中 -> `testCSingleton` 创建完毕，从前两级缓存中移除它，并加入到一级缓存 `singletonObjects` 中 -> `testBSingleton` 创建完毕，从前两级缓存中移除它，并加入到一级缓存 `singletonObjects` 中 -> `testASingleton` 创建完毕，从前两级缓存中移除它，并加入到一级缓存 `singletonObjects` 中

总结，Spring解决单例setter依赖使用了三级缓存：

* `singletonObjects` （一级缓存）
* `earlySingletonObjects` （二级缓存）
* `singletonFactories` （三级缓存）

其中，使用 `ObjectFactory` 的目的是为了延迟代理对象的创建，至于为什么要使用三级缓存，而不使用二级缓存，这个网上说法很多。但是可以明确的是，直接使用二级缓存也是可以的，比如：

```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.add(beanName);	
        }
    }
}
```

把上面的代码改成：

```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            this.earlySingletonObjects.put(beanName, singletonFactory.getObject());
            this.registeredSingletons.add(beanName);	
        }
    }
}
```

这依旧可以解决循环依赖。

个人倾向于认为这样设计是为了不破坏Spring的设计原则，比如说单一职责原则， `singletonObjects` 负责缓存创建好并初始化好的实例， `earlySingletonObjects` 负责缓存创建好但未初始化好的实例， `singletonFactories` 负责缓存 `ObjectFactory` 。


### Spring后置处理器

Spring的后置处理器主要有两类：

* BeanFactoryPostProcessor
* BeanPostProcessor

第一类 `BeanFactoryPostProcessor` 在 `AbstractApplicationContext#invokeBeanFactoryPostProcessors` 中完成注册和调用。

第二类 `BeanPostProcessor` 在 `AbstractApplicationContext#registerBeanPostProcessors` 中完成注册，一般在 `AbstractAutowireCapableBeanFactory#initializeBean` 中完成调用，但是也有例外，如 `InstantiationAwareBeanPostProcessor` 这类 `BeanPostProcessor` 在 `AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation` 中完成调用，以便用于返回代理来代替真正的实例

### Spring i18n

这里仅介绍消息的国际化。消息的国际化通过 `MessageSource` 实现，先在resources目录下创建资源文件，再配置一下 `MessageSource` 的 `basenames` 参数。需要注意的是，当有操作系统对应locale的资源文件时，如果此时需要获取一个不存在对应资源文件的locale的消息，它不会去使用默认的资源文件。举个例子：

现在有两个资源文件：

* `messages.properties`
* `messages_zh_CN.properties`

`messages.properties` 是默认的资源文件，如果此时要获取locale为 `Locale.US` 的消息，它不会使用 `messages.properties` ，而是会使用操作系统对应locale的资源文件（即 `messages_zh_CN.properties` ）

### 非spi形式注入集合类型时的排序

单纯的 `ClassPathXmlApplicationContext` 是无法处理 `Ordered` 接口或者 `Order` 注解的，要么定制化，要么使用 `AnnotationConfigApplicationContext`

这种排序（官方的实现）与待排序的bean是否实现 `Comparable` 接口无关

处理的位置在：

**org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveMultipleBeans**

![beans#1](resources/2022-09-05_09-57-14.png)