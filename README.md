# AutoValidation

AutoValidation为jsr303也就是常用的`javax.validation`注解校验提供了一种声明式的校验方式，框架将在编译期为标记了`@AutoValidation`的类、类构造器、类方法按需生成入参校验代码。

# 快速开始

下边的例子将具体展示使用AutoValidation的优势（避免重复书写复杂的`javax.validation`校验代码）：

- 使用AutoValidation之前：

```java
class User {
    private String username;
    public User(@NotEmpty String username) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Constructor<User> constructor = User.class.getDeclaredConstructor(String.class);
        Set<ConstraintViolation<T>> cvSet = validator.forExecutables().validateConstructorParameters(constructor, new Object[] {username})
        if (!cvSet.isEmpty()) {
            throw new ConstraintViolationException(cvSet);
        }
        this.username = username;
    }
}
```

- 使用AutoValidation之后：

```java
@AutoValidation
class User {
    private String username;
    public User(@NotEmpty String username) {
        // 所有的校验代码框架在编译期自动生成
        this.username = username;
    }
}
```

AutoValidation框架的使用非常简单，只需要添加下方依赖就能够在项目中使用`@AutoValidation`（该注解可以用于类、类构造器、类方法， 标记了`@AutoValidation`的类、类构造器、类方法将会在编译期间按需生成入参校验代码， 使用`groups()`可以指定校验所用的分组）。

```xml
<dependency>
    <groupId>fun.fengwk.auto-validation</groupId>
    <artifactId>validation</artifactId>
    <version>0.0.7</version>
</dependency>
<dependency>
    <groupId>fun.fengwk.auto-validation</groupId>
    <artifactId>processor</artifactId>
    <version>0.0.7</version>
    <scope>provided</scope>
</dependency>
```

注意：AutoMapper基于JSR 269 Annotation Processing API实现，需要对代码进行重新编译才能生成相应的校验代码，常见的编译方法有maven的`mvn install`，如果你正在使用IDEA那么使用Build -> Rebuild Project也是一种很好的方式。