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
import com.automup.repository.ScriptRepository;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Controller for evaluating scripts from console.
 */
@Controller
@RequestMapping("/console")
public class ConsoleController implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ScriptRepository scriptRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return "redirect:/console/index.html";
    }
    
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> list() {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		List<Script> ret = new ArrayList<Script>();
            Iterable<Script> scripts = scriptRepository.findAll();
            for (Script script : scripts) {
				script.setName("<a href='#' onclick='selectScript("+script.getId()+");'>"+script.getName()+"</a>");
            	ret.add(script);
			}
            
            return ScriptResult.create(ret, "");
        }).exceptionally(ScriptResult::create);
    }
    
    @RequestMapping(value = "/open", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> open(@RequestParam String id) {

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
            
            System.setOut(previousConsole);
            
            return ScriptResult.create(code, out.toString());
        }).exceptionally(ScriptResult::create);
    }
    
    @RequestMapping(value = "/save", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> save(@RequestParam String script, @RequestParam String id, @RequestParam String name) {

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
            
            return ScriptResult.create(ret.getId(), out.toString());
        }).exceptionally(ScriptResult::create);
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> delete(@RequestParam String id) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Long scriptId = Long.valueOf(id);
            scriptRepository.deleteById(scriptId);
            
            System.setOut(previousConsole);
            
            return ScriptResult.create("", out.toString());
        }).exceptionally(ScriptResult::create);
    }

    @RequestMapping(value = "/run", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> run(@RequestParam String script) {

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
            
            return ScriptResult.create(newConsole.toString(), out.toString());
        }).exceptionally(ScriptResult::create);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private static Value runJavaScript(String script, Context context) {
	  return context.eval("js", script);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class ScriptResult {

        private String[] output;
        private Object result;

        private ScriptResult() {
        }

        public String[] getOutput() {
            return output;
        }

        public Object getResult() {
            return result;
        }

        private static ScriptResult create(Throwable throwable) {
            String message = throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage();
            return create(null, message);
        }

        private static ScriptResult create(Object result, String output) {
            ScriptResult scriptletResult = new ScriptResult();
            scriptletResult.result = result;
            if (StringUtils.hasLength(output)) {
                scriptletResult.output = output.split(System.lineSeparator());
            }
            
            return scriptletResult;
        }
    }
}
