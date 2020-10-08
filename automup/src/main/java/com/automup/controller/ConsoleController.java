package com.automup.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Controller for evaluating scripts from console.
 */
@Controller
@RequestMapping("/console")
public class ConsoleController implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return "redirect:/console/index.html";
    }

    @RequestMapping(value = "/javascript", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<ScriptResult> executeJavascript(@RequestParam String script) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Context context = Context.newBuilder().allowAllAccess(true).build();
            
            context.getBindings("js").putMember("database", jdbcTemplate);
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
