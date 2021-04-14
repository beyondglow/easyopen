package com.gitee.durcframework.easyopen;

import com.gitee.easyopen.ApiConfig;
import com.gitee.easyopen.util.CopyUtil;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author tanghc
 */
public class PropCopyTest extends TestCase {


    @Test
    public void testCopy() {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setMarkdownDocDir("d:/doc");

        EasyopenPropertiesCopy properties = new EasyopenPropertiesCopy();
        properties.setShowDoc(true);
        properties.setInterceptors(Arrays.asList("A"));

        CopyUtil.copyPropertiesIgnoreNull(properties, apiConfig);


        System.out.println(apiConfig.getMarkdownDocDir());
    }

    @Test
    public void testCopy2() {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setMarkdownDocDir("d:/doc");

        EasyopenPropertiesCopy properties = new EasyopenPropertiesCopy();
        properties.setShowDoc(true);
        properties.setInterceptors(Arrays.asList("A"));

        CopyUtil.copyProperties(properties, apiConfig);


        System.out.println(apiConfig.getMarkdownDocDir());
    }

}
