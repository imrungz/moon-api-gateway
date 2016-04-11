package com.longcoding.undefined.services.impl;

import com.longcoding.undefined.helpers.Const;
import com.longcoding.undefined.helpers.netty.NettyClientFactory;
import com.longcoding.undefined.models.ResponseInfo;
import com.longcoding.undefined.services.ProxyService;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.http.HttpFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import play.libs.Json;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Created by longcoding on 16. 4. 6..
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    @Autowired
    NettyClientFactory nettyClientFactory;



    private DeferredResult<ResponseEntity> deferredResult;
    private ResponseInfo responseInfo;

    public void requestProxyService(HttpServletRequest request, DeferredResult<ResponseEntity> deferredResult) {

        this.deferredResult = deferredResult;

        this.responseInfo = (ResponseInfo) request.getAttribute(Const.RESPONSE_INFO_DATA);
        Request proxyRequest = nettyClientFactory.getNettyClient().newRequest(responseInfo.getRequestURI());

        setHeaderAndQueryInfo(proxyRequest, responseInfo).send(new BufferingResponseListener() {
            @Override
            public void onComplete(Result result) {
                if (!result.isFailed()) {
                    ResponseEntity responseEntity = null;

                    HttpFields responseHeaders = result.getResponse().getHeaders();
                    //TODO: not contain. compare equals.
                    if ( responseHeaders.contains(Const.REQUEST_RESPONSE_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) ){
                        responseEntity =
                                new ResponseEntity(Json.parse(getContentAsString(Charset.forName(Const.SERVER_DEFAULT_ENCODING_TYPE))), HttpStatus.OK);
                    } else {
                        System.out.println("잘못된 요청");
                        //TODO : occur ERROR
                    }
                    deferredResult.setResult(responseEntity);
                }
            }
            @Override
            public void onFailure(Response response, Throwable failure) {
                //TODO : occur ERROR
            }

        });

    }

    private Request setHeaderAndQueryInfo(Request request, ResponseInfo responseInfo) {
        Map<String, String> requestHeaders = responseInfo.getHeaders();

        for ( String headerKey : requestHeaders.keySet() ) {
            request.header(headerKey, requestHeaders.get(headerKey));
        }

        request.accept(responseInfo.getRequestAccept());
        request.header(Const.REQUEST_RESPONSE_CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);

        Map<String, String> requestQueryParams = responseInfo.getQueryStringMap();
        for ( String queryKey : requestQueryParams.keySet() ) {
            request.param(queryKey, requestQueryParams.get(queryKey));
        }

        return request;
    }

}
