package com.gitee.easyopen.support;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gitee.easyopen.ApiConfig;
import com.gitee.easyopen.ApiContext;
import com.gitee.easyopen.ApiResult;
import com.gitee.easyopen.ParamNames;
import com.gitee.easyopen.RespWriter;
import com.gitee.easyopen.Result;
import com.gitee.easyopen.auth.Oauth2Manager;
import com.gitee.easyopen.auth.Oauth2Service;
import com.gitee.easyopen.bean.ApiSearch;
import com.gitee.easyopen.bean.Consts;
import com.gitee.easyopen.bean.RequestMode;
import com.gitee.easyopen.config.ConfigClient;
import com.gitee.easyopen.doc.ApiDocHolder;
import com.gitee.easyopen.exception.ApiException;
import com.gitee.easyopen.interceptor.ApiInterceptor;
import com.gitee.easyopen.limit.LimitConfig;
import com.gitee.easyopen.limit.LimitConfigManager;
import com.gitee.easyopen.limit.LimitSearch;
import com.gitee.easyopen.message.Errors;
import com.gitee.easyopen.register.AbstractInitializer;
import com.gitee.easyopen.template.DocTemplate;
import com.gitee.easyopen.template.HandshakeTemplate;
import com.gitee.easyopen.template.InvokeTemplate;
import com.gitee.easyopen.template.LimitTemplate;
import com.gitee.easyopen.template.MonitorTemplate;
import com.gitee.easyopen.template.StatusParam;
import com.gitee.easyopen.template.WebfluxInvokeTemplate;
import com.gitee.easyopen.util.RequestUtil;

/**
 * ??????API????????????,?????????????????????????????????.RequestMapping??????value????????????
 * 
 * <pre>
 * &#64;Controller
 * &#64;RequestMapping(value = "/api")
 * public class IndexController extends ApiController {
 * }
 * 
 * ????????????????????????????????????:http://ip:port/contextPath/api
 * </pre>
 * @author tanghc
 */
public abstract class ApiController extends AbstractInitializer implements ApplicationListener<ContextRefreshedEvent> , ResponseHandler, VelocityContextHandler {
	protected static volatile ApplicationContext ctx;
    protected volatile ApiConfig apiConfig;

    private InvokeTemplate invokeTemplate;
    private WebfluxInvokeTemplate webfluxInvokeTemplate;
    private DocTemplate docTemplate;
    private MonitorTemplate monitorTemplate;
    private LimitTemplate limitTemplate;
    private HandshakeTemplate handshakeTemplate;

    @Autowired(required = false)
    protected Oauth2Manager oauth2Manager;
    
    protected Oauth2Service oauth2Service;

    /**
     * ?????????apiConfig?????????spring?????????????????????????????????????????????????????????????????????@Autowired??????????????????
     * @param apiConfig
     */
    protected abstract void initApiConfig(ApiConfig apiConfig);

    @Component
    private static class Ctx implements ApplicationContextAware {
        /** ????????????onApplicationEvent???????????? */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            ctx = applicationContext;
        }
    }

    /**
        spring???????????????????????????
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
    	ApplicationContext appCtx = this.getApplicationContext();
    	if(appCtx == null) {
    		appCtx = event.getApplicationContext();
    	}
        this.onStartup(appCtx);
    }
    
    protected ApplicationContext getApplicationContext() {
    	return ctx;
    }

    protected synchronized void onStartup(ApplicationContext applicationContext) {
    	if(this.apiConfig != null) {
    		return;
    	}
    	// ??????ApplicationContext
        ApiContext.setApplicationContext(applicationContext);
        // ????????????ApiConfig
        this.apiConfig = newApiConfig();
        ApiContext.setApiConfig(apiConfig);
        // ???????????????
        this.initApiConfig(this.apiConfig);

        this.apiConfig.loadPrivateKey();
        // ?????????Template
        this.initTemplate();
        // easyopen??????????????????????????????
        this.init(applicationContext, this.apiConfig);
        // ?????????????????????
        // ????????????
        this.initComponent();
    }

    @Override
    public void onRegistFinished(ApiConfig apiConfig) {
        ConfigClient configClient = this.apiConfig.getConfigClient();
        if(configClient != null) {
            configClient.init();
        } else {
            LimitConfigManager limitConfigManager = apiConfig.getLimitConfigManager();
            if(limitConfigManager != null) {
                limitConfigManager.loadToLocalCache();
            }
        }
    }

    protected void initTemplate() {
        this.invokeTemplate = new InvokeTemplate(apiConfig, this);
        this.webfluxInvokeTemplate = new WebfluxInvokeTemplate(apiConfig, this);
        this.docTemplate = new DocTemplate(apiConfig, this);
        this.monitorTemplate = new MonitorTemplate(apiConfig,this);
        this.limitTemplate = new LimitTemplate(apiConfig,this);
        this.handshakeTemplate = new HandshakeTemplate(apiConfig);
    }
    
    protected ApiConfig newApiConfig() {
        return ApiContext.getApiConfig();
    }
    
    private void initComponent() {
        if(oauth2Manager != null) {
            apiConfig.initOauth2Service(oauth2Manager);
            oauth2Service = apiConfig.getOauth2Service();
        }

        initInterceptor();
    }
    
    /** ????????????????????? */
    private void initInterceptor() {
        if(this.apiConfig.getShowMonitor()) {
            ApiInterceptor[] interceptors = this.apiConfig.getInterceptors();
            int len = interceptors.length + 1;
            ApiInterceptor[] newInterceptors = new ApiInterceptor[len];
            // ???????????????????????????
            newInterceptors[0] = this.apiConfig.getMonitorInerceptor();
            for (int i = 0; i < interceptors.length; i++) {
                newInterceptors[i + 1] = interceptors[i];
            }
            this.apiConfig.setInterceptors(newInterceptors);
        }
    }
    
    /**
     * ????????????
     * 
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET})
    public void index(HttpServletRequest request, HttpServletResponse response) {
        // ??????????????????,????????????@Api???????????????
        ApiContext.setRequestMode(RequestMode.SIGNATURE);
        this.invokeTemplate.processInvoke(request, response);
    }
    
    /**
     * ??????????????????????????????http://www.xxx.com/api/?????????/<br>
     * ???:<br>
     * http://www.xxx.com/api/goods.get/
     * <strong>??????????????????????????????/?????????</strong><br>
     * @param name ?????????
     * @param request
     * @param response
     */
    @RequestMapping(value="/{name}/", method = {RequestMethod.POST, RequestMethod.GET})
    public void rest(@PathVariable("name") String name,
    		HttpServletRequest request, HttpServletResponse response) {
    	this.doRest(name, null, request, response);
    }
    
    /**
     * ??????????????????????????????????????????http://www.xxx.com/api/?????????/?????????/<br>
     * ???:<br>
     * http://www.xxx.com/api/goods.get/1.0/
     * <strong>??????????????????????????????/?????????</strong><br>
     * @param name ?????????
     * @param version ?????????
     * @param request
     * @param response
     */
    @RequestMapping(value="/{name}/{version}/", method = {RequestMethod.POST, RequestMethod.GET})
    public void rest2(@PathVariable("name") String name, @PathVariable("version") String version,
    		HttpServletRequest request, HttpServletResponse response) {
    	this.doRest(name, version, request, response);
    }
    
    protected void doRest(String name, String version,
    		HttpServletRequest request, HttpServletResponse response) {
    	if(name == null) {
    		throw new IllegalArgumentException("name????????????");
    	}
    	if(version == null) {
    		version = this.apiConfig.getDefaultVersion();
    	}
    	request.setAttribute(Consts.REST_PARAM_NAME, name);
    	request.setAttribute(Consts.REST_PARAM_VERSION, version);
    	this.index(request, response);
    }

    /**
     * Mock????????????
     *
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "mock",method = {RequestMethod.POST, RequestMethod.GET})
    public void indexMock(HttpServletRequest request, HttpServletResponse response) {
        // ??????????????????,????????????@Api???????????????
        ApiContext.setRequestMode(RequestMode.SIGNATURE);
        this.invokeTemplate.processInvokeMock(request, response);
    }
    
    /**
     * ??????????????????
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "ssl", method = RequestMethod.POST)
    public void ssl(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ApiContext.setRequestMode(RequestMode.ENCRYPT);
        this.invokeTemplate.processInvoke(request, response);
    }

    /**
     * ??????????????????
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "ssl2", method = RequestMethod.POST)
    public void ssl2(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ApiContext.useNewSSL(request);
        this.invokeTemplate.processInvoke(request, response);
    }
    

    /**
     * ?????????????????????
     * @param response
     * @param result ??????
     */
    @Override
    public void responseResult(HttpServletRequest request, HttpServletResponse response, Object result) {
        if(result == null) {
            return;
        }
        RespWriter respWriter = this.apiConfig.getRespWriter();
        respWriter.write(request, response, result);
    }

    @Override
    public Result caugthException(Throwable e) {
        String code = Errors.SYS_ERROR.getCode();
        String msg = e.getMessage();
        Object data = null;
        e = this.apiConfig.getExceptionFormatter().apply(e);
        if (e instanceof ApiException) {
            ApiException apiEx = (ApiException) e;
            code = apiEx.getCode();
            msg = apiEx.getMessage();
            data = apiEx.getData();
        }

        return this.apiConfig.getResultCreator().createErrorResult(code, msg, data);
    }

    /**
     * ????????????
     * 
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping("doc")
    public void doc(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        this.docTemplate.processDoc(request, response);
    }

    /**
     * ????????????
     *
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping("doc/download")
    public void docDl(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        this.docTemplate.downloadPdf(request, response);
    }

    @RequestMapping("json/doc")
    @ResponseBody
    public Map jsondoc(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Map context = new HashMap();
        context.put("apiModules", ApiDocHolder.getApiDocBuilder().getApiModules());

        context.put("ACCESS_TOKEN_NAME", ParamNames.ACCESS_TOKEN_NAME);
        context.put("API_NAME", ParamNames.API_NAME);
        context.put("APP_KEY_NAME", ParamNames.APP_KEY_NAME);
        context.put("DATA_NAME", ParamNames.DATA_NAME);
        context.put("FORMAT_NAME", ParamNames.FORMAT_NAME);
        context.put("SIGN_METHOD_NAME", ParamNames.SIGN_METHOD_NAME);
        context.put("SIGN_NAME", ParamNames.SIGN_NAME);
        context.put("TIMESTAMP_NAME", ParamNames.TIMESTAMP_NAME);
        context.put("TIMESTAMP_PATTERN", ParamNames.TIMESTAMP_PATTERN);
        context.put("VERSION_NAME", ParamNames.VERSION_NAME);

        context.put("code_name", "code");
        context.put("code_description", "????????????\"0\"?????????????????????????????????");

        context.put("msg_name", "msg");
        context.put("msg_description", "??????????????????????????????");

        context.put("data_name", "data");
        context.put("data_description", "?????????????????????????????????{}");
        return context;
    }

    @RequestMapping("doc/pwd")
    @ResponseBody
    public Map docPwd(String password, HttpServletRequest request) throws Throwable {
        Map map = new HashMap();
        if(apiConfig.getDocPassword().equals(password)){
            map.put("code",0);
            map.put("msg","????????????");
            Map<String, String> data = new HashMap<>();
            data.put("id", "doc");
            data.put("username", RequestUtil.getClientIP(request));
            String jwt = ApiContext.createJwt(data);
            map.put("jwt",jwt);
        }else{
            map.put("code",1);
            map.put("msg","????????????");
        }
        return map;
    }

    /**
     * ??????????????????
     * @throws IOException
     * @throws ServletException 
     */
    @RequestMapping("monitor")
    public void monitor(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        this.monitorTemplate.processMonitor(request, response);
    }
    
    /** ?????????????????? */
    @RequestMapping("monitor/data")
    @ResponseBody
    public Object monitorData(ApiSearch apiSearch,HttpServletRequest request) throws IOException {
        return this.monitorTemplate.monitorData(apiSearch, request);
    }
    
    /** ?????????????????? */
    @RequestMapping("monitor/del")
    @ResponseBody
    public Object monitorDel(String name,String version) throws IOException {
        return this.monitorTemplate.monitorDel(name, version);
    }
    
    /**
     * ???????????????
     * @param request
     * @param response
     * @return ??????????????????
     * @throws Throwable
     */
    @RequestMapping(value = "handshake", method = RequestMethod.POST)
    @ResponseBody
    public Object handshake(HttpServletRequest request, HttpServletResponse response) {
        return this.handshakeTemplate.handshake(request, response);
    }

    /**
     * ???????????????
     * @param request
     * @param response
     * @return ??????????????????
     * @throws Throwable
     */
    @RequestMapping(value = "handshake2", method = RequestMethod.POST)
    @ResponseBody
    public Object handshake2(HttpServletRequest request, HttpServletResponse response) {
        return this.handshakeTemplate.handshake2(request, response);
    }

    /**
     * oauth2????????????code
     * @param request
     * @param resp
     * @return ??????code
     * @throws URISyntaxException
     * @throws OAuthSystemException
     */
    @RequestMapping("authorize")
    public Object authorize(HttpServletRequest request,HttpServletResponse resp) throws URISyntaxException, OAuthSystemException {
        OAuthResponse response = oauth2Service.authorize(request, resp, apiConfig);
        if(response == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(response.getLocationUri()));
        return new ResponseEntity<String>(headers, HttpStatus.valueOf(response.getResponseStatus()));
    }

    /**
     * ??????code??????accessToken
     * @param request
     * @return ??????accessToken
     * @throws URISyntaxException
     * @throws OAuthSystemException
     */
    @RequestMapping("accessToken")
    public HttpEntity<?> accessToken(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
        OAuthResponse response = oauth2Service.accessToken(request, apiConfig);
        return new ResponseEntity<String>(  
                response.getBody(), HttpStatus.valueOf(response.getResponseStatus())); 
    }
    
    /**
     * ???????????????<br>
     * ??????????????????????????????
     * <code>InvokeTemplate.processError()</code>????????????????????????????????????????????????
     * @param request
     * @param response
     * @param e
     */
    @ExceptionHandler(value = Throwable.class)
    public void jsonErrorHandler(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        logger.error("jsonErrorHandler error", e);
        ApiResult result = new ApiResult();
        result.setCode(Errors.SYS_ERROR.getCode());
        result.setMsg(e.getMessage());
        try {
            apiConfig.getRespWriter().write(request, response, result);
        } catch (Exception e1) {
            logger.error("com.gitee.easyopen.support.ApiController.jsonErrorHandler()???json??????", e1);
        }
    }

    public void setInvokeTemplate(InvokeTemplate invokeTemplate) {
        this.invokeTemplate = invokeTemplate;
    }

    public void setWebfluxInvokeTemplate(WebfluxInvokeTemplate webfluxInvokeTemplate) {
        this.webfluxInvokeTemplate = webfluxInvokeTemplate;
    }


    

    /* ***********************************????????????start*********************************** */
    /**
     * ??????????????????
     * @throws IOException
     * @throws ServletException 
     */
    @RequestMapping("limit")
    public void limit(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        this.limitTemplate.processLimit(request, response);
    }

    @RequestMapping("limit/data")
    @ResponseBody
    public Object limitData(LimitSearch apiSearch,HttpServletRequest request) throws IOException {
        return this.limitTemplate.limitData(apiSearch, request);
    }
    
    @RequestMapping("limit/modify")
    @ResponseBody
    public Object limitModify(LimitConfig limitConfig, HttpServletRequest request) throws IOException {
        return this.limitTemplate.limitModify(limitConfig, request);
    }
    
    @RequestMapping("limit/status")
    @ResponseBody
    public Object limitStatus(@RequestBody StatusParam statusParam, HttpServletRequest request) throws IOException {
        return this.limitTemplate.limitStatus(statusParam, request);
    }
    /* ***********************************????????????end*********************************** */

    @Override
    public void processDocVelocityContext(VelocityContext context) {

    }

    @Override
    public void processLimitVelocityContext(VelocityContext context) {

    }

    @Override
    public void processMonitorVelocityContext(VelocityContext context) {

    }

    @Override
    public String getDocRemark() {
        return "";
    }

    public InvokeTemplate getInvokeTemplate() {
        return invokeTemplate;
    }

    public WebfluxInvokeTemplate getWebfluxInvokeTemplate() {
        return webfluxInvokeTemplate;
    }

    public DocTemplate getDocTemplate() {
        return docTemplate;
    }

    public void setDocTemplate(DocTemplate docTemplate) {
        this.docTemplate = docTemplate;
    }

    public MonitorTemplate getMonitorTemplate() {
        return monitorTemplate;
    }

    public void setMonitorTemplate(MonitorTemplate monitorTemplate) {
        this.monitorTemplate = monitorTemplate;
    }

    public LimitTemplate getLimitTemplate() {
        return limitTemplate;
    }

    public void setLimitTemplate(LimitTemplate limitTemplate) {
        this.limitTemplate = limitTemplate;
    }

    public HandshakeTemplate getHandshakeTemplate() {
        return handshakeTemplate;
    }

    public void setHandshakeTemplate(HandshakeTemplate handshakeTemplate) {
        this.handshakeTemplate = handshakeTemplate;
    }

}
