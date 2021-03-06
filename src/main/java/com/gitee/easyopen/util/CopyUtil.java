package com.gitee.easyopen.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 属性拷贝
 * 
 * @author tanghc
 */
public class CopyUtil extends BeanUtils {

    /**
     * 属性拷贝，把map中的值拷贝到target中去
     * 
     * @param map
     * @param target
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void copyProperties(Map<String, Object> map, Object target) {
        Assert.notNull(map, "map must not be null");
        Assert.notNull(target, "Target must not be null");

        if (target instanceof Map) {
            Map targetMap = (Map) target;
            try {
                targetMap.putAll(map);
            }catch (Exception e) {
                throw new FatalBeanException("target的key类型必须为String，value类型为Object");
            }
        } else {

            Set<Entry<String, Object>> entrySet = map.entrySet();
            Class<?> targetClass = target.getClass();

            for (Entry<String, Object> entry : entrySet) {
                String propertyName = entry.getKey();
                Object val = entry.getValue();
                Method[] methods = targetClass.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    Class<?>[] methodParams = method.getParameterTypes();
                    // set开头
                    if (methodName.startsWith(ReflectionUtil.PREFIX_SET)) {
                        // 能否拷贝
                        boolean canCopy =
                                // 字段名一样
                                propertyName.equals(ReflectionUtil.buildFieldName(methodName))
                                        // 并且只有一个参数
                                && methodParams.length == 1
                                        // val是methodParams[0]或他的子类
                                && methodParams[0].isInstance(val) || Number.class.isInstance(val);

                        if (canCopy) {
                            try {
                                if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                                    method.setAccessible(true);
                                }
                                method.invoke(target, val);
                            } catch (Throwable ex) {
                                throw new FatalBeanException(
                                        "Could not copy property '" + propertyName + "' from map to target", ex);
                            }
                        }
                    }
                }

            }

        }

    }
    
    /**
     * 属性拷贝,第一个参数中的属性值拷贝到第二个参数中<br>
     * 注意:当第一个参数中的属性有null值时,不会拷贝进去
     * @param source 源对象
     * @param target 目标对象
     * @throws BeansException
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target, String ...ignoreProperties)
            throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        Class<?> actualEditable = target.getClass();
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null &&
                            ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }
                            Object value = readMethod.invoke(source);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            // 这里判断value是否为空 当然这里也能进行一些特殊要求的处理
                            // 例如绑定时格式转换等等
                            if (value != null) {
                                if (!Modifier.isPublic(writeMethod
                                        .getDeclaringClass().getModifiers())) {
                                    writeMethod.setAccessible(true);
                                }
                                writeMethod.invoke(target, value);
                            }
                        }
                        catch (Throwable ex) {
                            throw new FatalBeanException(
                                    "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                        }
                    }
                }
            }
        }
    }

    /*
    // 这里判断value是否为空 当然这里也能进行一些特殊要求的处理
                        // 例如绑定时格式转换等等
                        if (value != null) {
                            if (!Modifier.isPublic(writeMethod
                                    .getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        }
     */

    /*

     */

}
