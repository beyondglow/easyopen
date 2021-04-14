package com.gitee.durcframework.easyopen;

import com.gitee.easyopen.ApiValidator;
import junit.framework.TestCase;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author tanghc
 */
public class ValidatorTest extends TestCase {

    private ApiValidator serviceParamValidator = new ApiValidator();

    public void testField() {
        Manager manager = new Manager("Jim", 22, new Date());
        Store store = new Store("仓库A", manager);
        Goods goods = new Goods("Apple", new BigDecimal(50000), store);
        serviceParamValidator.validateBusiParam(goods);
    }

    @Data
    @AllArgsConstructor
    static class Goods {
        @NotBlank(message = "商品名称不能为空")
        private String goodsName;

        @Min(value = 1, message = "商品价格最小值为1")
        private BigDecimal price;

        @NotNull(message = "仓库不能为空")
        private Store store;
    }

    @Data
    @AllArgsConstructor
    static class Store {
        @NotBlank(message = "库存名称不能为空")
        private String storeName;

        @NotNull(message = "管理员不能为空")
        private Manager manager;
    }

    @Data
    @AllArgsConstructor
    static class Manager  {
        @NotBlank(message = "管理员姓名不能为空")
        private String name;

        private int age;

        @NotNull(message = "时间不能为空")
        private Date date;
    }

}
