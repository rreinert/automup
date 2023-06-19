package com.automup.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.automup.domain.RestResult;
import com.automup.domain.SysScript;
import com.automup.service.ColumnService;
import com.automup.service.DatabaseService;
import com.automup.service.MailService;
import com.automup.service.ProcessService;
import com.automup.service.RecordService;
import com.automup.service.ScheduleService;
import com.automup.service.ScriptService;
import com.automup.service.TableService;

@Controller
@RequestMapping("/script")
public class ScriptController implements ApplicationContextAware {

	//commit
	
    private ApplicationContext applicationContext;

    @Autowired
    private ScriptService scriptService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private MailService mailService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private TableService tableService;

    @Autowired
    private ColumnService columnService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private ScheduleService scheduleService;

	private static final Logger log = LogManager.getLogger(ScriptController.class);

    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return "scripts";
    }
    
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> list() {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		List<SysScript> ret = new ArrayList<SysScript>();
            Iterable<SysScript> scripts = scriptService.findAll();
            for (SysScript script : scripts) {
				script.setName("<a href='#' onclick=\"selectScript('"+script.getId()+"');\">"+script.getName()+"</a>");
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

            Optional<SysScript> scriptFound = scriptService.findById(id);
            
            String code = "";

            if (scriptFound != null) {
            	SysScript script = scriptFound.get();
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
            SysScript s = null;

            if (StringUtils.isEmpty(id)) {
                s = new SysScript();
                s.setName(name);
            }else {
                Optional<SysScript> scriptFound = scriptService.findById(id);
                
                if (scriptFound != null) {
                	s = scriptFound.get();
                }
            }
            
            s.setCode(script);
            SysScript ret = scriptService.save(s);
            
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

            scriptService.deleteById(id);
            
            System.setOut(previousConsole);
            
            return RestResult.create("", out.toString());
        }).exceptionally(RestResult::create);
    }

    @RequestMapping(value = "/run", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> run(HttpServletRequest request, @RequestParam String script) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Context context = Context.newBuilder().allowAllAccess(true).build();
            
            context.getBindings("js").putMember("database", jdbcTemplate);
            context.getBindings("js").putMember("mailService", mailService);
            context.getBindings("js").putMember("log", log);
            
            context.getBindings("js").putMember("database", jdbcTemplate);
            context.getBindings("js").putMember("request", request);
            context.getBindings("js").putMember("log", log);
            context.getBindings("js").putMember("recordService", recordService);
            context.getBindings("js").putMember("tableService", tableService);
            context.getBindings("js").putMember("columnService", columnService);
            context.getBindings("js").putMember("processService", processService);
            context.getBindings("js").putMember("databaseService", databaseService);
            context.getBindings("js").putMember("scheduleService", scheduleService);
            context.getBindings("js").putMember("mailService", mailService);
            
//            context.getBindings("js").putMember("scriptRunnable", scriptRunnable);
//            runJavaScript("function add(num){return num +20;}", context);
            
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

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
