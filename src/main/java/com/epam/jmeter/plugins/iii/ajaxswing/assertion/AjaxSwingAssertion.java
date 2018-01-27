package com.epam.jmeter.plugins.iii.ajaxswing.assertion;

import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.threads.*;
import org.apache.jmeter.util.TidyException;
import org.apache.jmeter.util.XPathUtil;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.json.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by Evgenii_Stukanov on 10.01.2018.
 */
public class AjaxSwingAssertion extends AbstractTestElement implements Serializable, Assertion {
    private static final Logger log = LoggingManager.getLoggerForClass();
    public static final String INVERT = "INVERT";
    public static final String SEARCH_FOR = "SEARCH_FOR";
    public static final String SEARCH_TYPE = "SEARCH_TYPE";


    public boolean isInvert() {
        return this.getPropertyAsBoolean("INVERT");
    }
    public void setInvert(boolean invert) {
        this.setProperty("INVERT", invert);
    }

    public String getSearchFor() {
        return this.getPropertyAsString("SEARCH_FOR");
    }

    public void setSearchFor(String searchFor) {
        this.setProperty("SEARCH_FOR", searchFor);
    }


    public void setSearchType(String searchType) {
        this.setProperty("SEARCH_TYPE", searchType);
    }

    public String getSearchType() {
        return this.getPropertyAsString("SEARCH_TYPE");
    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private String json2HTML(String res) throws JSONException
    {
        JSONObject obj = new JSONObject(res);
        JSONArray arr = obj.getJSONArray("actionableComponentStates");
        String escapedHTML = "";
        for(int i = 0; i < arr.length(); i++)
        {
            escapedHTML += arr.getJSONObject(i).getString("html");
        }
        //log.warn(StringEscapeUtils.unescapeJava(escapedHTML));
        return StringEscapeUtils.unescapeJava(escapedHTML);
    }

    private String getJsonScript(String res) throws JSONException
    {
        JSONObject obj = new JSONObject(res);
        return StringEscapeUtils.unescapeJava(obj.getString("script"));
    }

    private AssertionResult validate(String response,String validateSource){
        AssertionResult result = new AssertionResult(this.getName());
        if("substring".equals(getSearchType())){
            if(isInvert()){
                if(response.contains(getSearchFor())){
                    result.setResultForFailure(validateSource + " contains substring that it mustn't contain! : \"" + getSearchFor() +"\"");
                }
            } else {
                if(!response.contains(getSearchFor())){
                    result.setResultForFailure(validateSource + " doesn't contain required substring! : \"" + getSearchFor() +"\"");
                }
            }
        }
        if ("xpath".equals(getSearchType())){
            if(isInvert()){
                if(checkXpath(response,getSearchFor())){
                    result.setResultForFailure(validateSource + " matches given xpath but it must not! : \"" + getSearchFor() +"\"");
                }
            } else {
                if(!checkXpath(response,getSearchFor())){
                    result.setResultForFailure(validateSource + " doesn't match given xpath! : \"" + getSearchFor() +"\"");
                }
            }
        }
        if ("regex".equals(getSearchType())){
            if(isInvert()){
                if(checkRegex(response,getSearchFor())){
                    result.setResultForFailure(validateSource + " matches given regex but it must not! : \"" + getSearchFor() +"\"");
                }
            } else {
                if(!checkRegex(response,getSearchFor())){
                    result.setResultForFailure(validateSource + " doesn't match given regex! : \"" + getSearchFor() +"\"");
                }
            }
        }
        return result;
    }

    private SampleResult sendGET(String purpose){
        JMeterContext jmctx = JMeterContextService.getContext();
        HTTPSamplerProxy sampler = (HTTPSamplerProxy)jmctx.getCurrentSampler();
        //JMeterProperty jp = cookeManager.getCookies().get(0); // JSESSIONID
        HTTPSampler get = new HTTPSampler();
        get.setMethod("GET");
        get.setHeaderManager(sampler.getHeaderManager());
        get.setCookieManager(sampler.getCookieManager());
        get.setDomain(sampler.getDomain());
        get.setPath(sampler.getPath());
        get.setPort(sampler.getPort());
        get.setProtocol(sampler.getProtocol());
        get.setName(sampler.getName() + purpose);
        get.setThreadName(sampler.getThreadName());
        SampleResult res = get.sample(null);
        get = null;
        notifyListeners(res);
        return res;
    }

    private SampleResult sendUpdate() {
        JMeterContext jmctx = JMeterContextService.getContext();
        HTTPSamplerProxy sampler = (HTTPSamplerProxy)jmctx.getCurrentSampler();
        //JMeterProperty jp = cookeManager.getCookies().get(0); // JSESSIONID
        HTTPSampler post = new HTTPSampler();
        Arguments arguments = sampler.getArguments();
        Arguments newArguments = new Arguments();
        for (int i = 0 ; i < arguments.getArgumentCount(); i++) {
            Argument arg = arguments.getArgument(i);
            if (!"actionString".equals(arg.getName())) {
                newArguments.addArgument(new HTTPArgument(arg.getName(), arg.getValue(), arg.getMetaData()));
            }
        }
        newArguments.addArgument(new HTTPArgument("actionString","/update/global/"));
        post.setMethod("POST");
        post.setArguments(newArguments);
        post.setHeaderManager(sampler.getHeaderManager());
        post.setCookieManager(sampler.getCookieManager());
        post.setDomain(sampler.getDomain());
        post.setPath(sampler.getPath());
        post.setPort(sampler.getPort());
        post.setProtocol(sampler.getProtocol());
        post.setName(sampler.getName() + "_Additional_POST(/update/global/)");
        post.setThreadName(sampler.getThreadName());
        SampleResult res = post.sample(null);
        post = null;
        notifyListeners(res);
        return res;
    }

    private AssertionResult refreshPage(SampleResult sampleResult){
        SampleResult afterGET = sendGET("_Additional_GET(reloadPage)");
        sampleResult.setResponseData(afterGET.getResponseData());
        return validate(afterGET.getResponseDataAsString(),"Reloaded HTML");
    }

    protected void notifyListeners(SampleResult res) {
        ListenerNotifier lnf = new ListenerNotifier();
        JMeterContext threadContext = this.getThreadContext();
        JMeterVariables threadVars = threadContext.getVariables();
        SamplePackage pack = (SamplePackage)threadVars.getObject("JMeterThread.pack");
        if(pack == null) {
            log.warn("Could not fetch SamplePackage");
        } else {
            SampleEvent event = new SampleEvent(res, threadContext.getThreadGroup().getName(), threadVars, false);
            res = null;
            lnf.notifyListeners(event, pack.getSampleListeners());
        }

    }

    private boolean isHTML(String text){
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        tidy.setShowErrors(0);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        tidy.parse(new ByteArrayInputStream(text.getBytes()), os);
        if((long)tidy.getParseErrors() > 0){
            return false;
        } else {
            return true;
        }
    }

    private boolean checkXpath(String text,String xpath){
        AssertionResult result = new AssertionResult(this.getName());
        Document doc;
        try{
            doc = XPathUtil.makeDocument(new ByteArrayInputStream(text.getBytes()), false, false, false, true, true, false, false, false, false);
        } catch (SAXException var6) {
            //log.warn("Caught sax exception: " + var6);
            return false;
        } catch (IOException var7) {
            //log.warn("Cannot parse result content", var7);
            return false;
        } catch (ParserConfigurationException var8) {
            //log.warn("Cannot parse result content", var8);
            return false;
        } catch (TidyException var9) {
            //log.warn("Cannot parse result content", var9);
            return false;
        }
        if(doc != null && doc.getDocumentElement() != null) {
            XPathUtil.computeAssertionResult(result, doc, xpath, false);
            if(result.isFailure()){
                //log.warn(result.getFailureMessage());
                return false;
            } else {
                return true;
            }
        } else {
            //log.warn("HTML doc is null");
            return false;
        }

    }

    private AssertionResult delayValidation(AssertionResult firstResult,long secondsToDelay) throws InterruptedException,JSONException{
        if(firstResult.isFailure()){
            Thread.sleep(500);
            long end = System.currentTimeMillis() + (secondsToDelay * 1000);
            AssertionResult extraResult = validate(json2HTML(sendUpdate().getResponseDataAsString()),"Unescaped HTML from Update JSON");
            while((extraResult.isFailure()) && (System.currentTimeMillis() <= end)){
                Thread.sleep(500);
                extraResult = validate(json2HTML(sendUpdate().getResponseDataAsString()),"Unescaped HTML from Update JSON");
            }
            return extraResult;
        }
            return firstResult;
    }

    private AssertionResult delayValidationGET(AssertionResult firstResult,long secondsToDelay)throws InterruptedException {
        if(firstResult.isFailure()){
            Thread.sleep(500);
            long end = System.currentTimeMillis() + (secondsToDelay * 1000);
            AssertionResult extraResult = validate(sendGET("_Additional_GET(revalidation)").getResponseDataAsString(),"HTML from extra GET");
            while((extraResult.isFailure()) && (System.currentTimeMillis() <= end)){
                Thread.sleep(500);
                extraResult = validate(sendGET("_Additional_GET(revalidation)").getResponseDataAsString(),"HTML from extra GET");
            }
            return extraResult;
        }
        return firstResult;
    }

    private boolean checkRegex(String text,String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();

    }

    public AssertionResult getResult(SampleResult samplerResult) {
        AssertionResult result = new AssertionResult(this.getName());
        String responseData = samplerResult.getResponseDataAsString();
        if(responseData.isEmpty()) {
            return refreshPage(samplerResult);
        } else {
            if (isJSONValid(responseData)){
                try{
                    String script = getJsonScript(responseData);        //Extract script from JSON
                    String html = json2HTML(responseData);              //Extract html from JSON and unescape
                    if(script.contains("reloadPage")){
                        return refreshPage(samplerResult);
                    } else {
                        try{
                            return delayValidation(validate(html,"Unescaped HTML from JSON"),5);
                            //return validate(html,"Unescaped HTML from JSON");
                        } catch (InterruptedException e){
                            return result.setResultForFailure("Validation delay sleep Interrupted : " + e);
                        }

                    }
                } catch (JSONException e){
                    return result.setResultForFailure("It must never happen, but somehow failed to parse JSON after JSON check :" + e.getMessage());
                }

            } else {
                if(isHTML(responseData)){
                    return validate(responseData,"HTML Response data");
                }else{
                    result.setResultForFailure("The response was not HTML, and not json ... what the hell are you?");
                }
            }
        }
        return result;
    }

}
