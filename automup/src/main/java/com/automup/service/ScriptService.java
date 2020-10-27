package com.automup.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.automup.entity.Script;
import com.automup.repository.ScriptRepository;

@Service("ScriptService")
public class ScriptService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScriptRepository scriptRepository;
	
    @Transactional
    public void run(@RequestParam String scriptName, HttpServletRequest request) {

        List<Script> scripts = scriptRepository.findByName(scriptName);
        
        String code = "";
        if (scripts != null && scripts.size() > 0) {
        	code = scripts.get(0).getCode();
        }
		
        Context context = Context.newBuilder().allowAllAccess(true).build();
        
        context.getBindings("js").putMember("database", jdbcTemplate);
        context.getBindings("js").putMember("request", request);
        
        runJavaScript("function add(num){return num +20;}", context);
        runJavaScript(code, context);
        
    }
    
    public static Value runJavaScript(String script, Context context) {
	  return context.eval("js", script);
    }

}
