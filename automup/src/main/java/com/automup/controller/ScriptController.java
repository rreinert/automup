package com.automup.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.automup.entity.Script;
import com.automup.entity.RestResult;
import com.automup.repository.ScriptRepository;

@Controller
@RequestMapping("/script")
public class ScriptController implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ScriptRepository scriptRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return "redirect:/script/index.html";
    }
    
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> list() {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		List<Script> ret = new ArrayList<Script>();
            Iterable<Script> scripts = scriptRepository.findAll();
            for (Script script : scripts) {
				script.setName("<a href='#' onclick='selectScript("+script.getId()+");'>"+script.getName()+"</a>");
            	ret.add(script);
			}
            
            return RestResult.create(ret, "");
        }).exceptionally(RestResult::create);
    }
    
    @RequestMapping(value = "/open", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> open(@RequestParam String id) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Long scriptId = Long.valueOf(id);
            
            Optional<Script> scriptFound = scriptRepository.findById(scriptId);
            
            String code = "";

            if (scriptFound != null) {
            	Script script = scriptFound.get();
            	code = script.getCode();
            }
            
            if (code == null) {
            	code = "";
            }
            
            System.setOut(previousConsole);
            
            return RestResult.create(code, out.toString());
        }).exceptionally(RestResult::create);
    }
    
    @RequestMapping(value = "/save", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> save(@RequestParam String script, @RequestParam String id, @RequestParam String name) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Script s = null;

            if (StringUtils.isEmpty(id)) {
                s = new Script();
                s.setName(name);
            }else {
                Long scriptId = Long.valueOf(id);
                Optional<Script> scriptFound = scriptRepository.findById(scriptId);
                
                if (scriptFound != null) {
                	s = scriptFound.get();
                }
            }
            
            s.setCode(script);
            Script ret = scriptRepository.save(s);
            
            System.setOut(previousConsole);
            
            return RestResult.create(ret.getId(), out.toString());
        }).exceptionally(RestResult::create);
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> delete(@RequestParam String id) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Long scriptId = Long.valueOf(id);
            scriptRepository.deleteById(scriptId);
            
            System.setOut(previousConsole);
            
            return RestResult.create("", out.toString());
        }).exceptionally(RestResult::create);
    }

    @RequestMapping(value = "/run", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> run(@RequestParam String script) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Context context = Context.newBuilder().allowAllAccess(true).build();
            
            context.getBindings("js").putMember("database", jdbcTemplate);
            
            runJavaScript("function add(num){return num +20;}", context);
            runJavaScript(script, context);
            
            System.setOut(previousConsole);
            
            return RestResult.create(newConsole.toString(), out.toString());
        }).exceptionally(RestResult::create);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private static Value runJavaScript(String script, Context context) {
	  return context.eval("js", script);
    }

}
