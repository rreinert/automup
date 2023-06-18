package com.automup.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.automup.domain.Record;
import com.automup.domain.SysScript;
import com.automup.job.ScriptRunnable;
import com.automup.repository.ScriptRepository;

@Service
public class ScriptService {

	@Autowired
	private ScriptRepository scriptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ProcessService processService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private TableService tableService;

    @Autowired
    private ColumnService columnService;

    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private MailService mailService;

    @Autowired
    private ScriptRunnable scriptRunnable;

	private static final Logger log = LogManager.getLogger();

	private Context context = Context.newBuilder().allowAllAccess(true).build();
	
    @Transactional
    public void run(@RequestParam String scriptName) {
    	run(scriptName, null);
    }

    @Transactional
    public void run(@RequestParam String scriptName, HttpServletRequest request) {

        List<SysScript> scripts = scriptRepository.findByName(scriptName);
        
        String code = "";
        if (scripts != null && scripts.size() > 0) {
        	code = scripts.get(0).getCode();
        }
		
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
        context.getBindings("js").putMember("scriptRunnable", scriptRunnable);
        
        runJavaScript(code, context);
    }

    @Transactional
    public void runCode(String code) {
    	runCode(code, null);
    }
    
    @Transactional
    public void runCode(String code, HttpServletRequest request) {

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
        context.getBindings("js").putMember("scriptRunnable", scriptRunnable);
        
        runJavaScript(code, context);
    }
    
    @Transactional
    public void runRule(String code, HttpServletRequest request, Record record) {

        context.getBindings("js").putMember("database", jdbcTemplate);
        context.getBindings("js").putMember("request", request);
        context.getBindings("js").putMember("log", log);
        context.getBindings("js").putMember("processService", processService);
        context.getBindings("js").putMember("recordService", recordService);
        context.getBindings("js").putMember("tableService", tableService);
        context.getBindings("js").putMember("columnService", columnService);
        context.getBindings("js").putMember("record", record);
        context.getBindings("js").putMember("databaseService", databaseService);
        context.getBindings("js").putMember("scheduleService", scheduleService);
        context.getBindings("js").putMember("mailService", mailService);
        context.getBindings("js").putMember("scriptRunnable", scriptRunnable);
        
        runJavaScript(code, context);
    }

    public static Value runJavaScript(String script, Context context) {
	  return context.eval("js", script);
    }

	public Iterable<SysScript> findAll() {
		return scriptRepository.findAll();
	}

	public Optional<SysScript> findById(String id) {
		return scriptRepository.findById(id);
	}

	public SysScript save(SysScript s) {
		
		SysScript script = new SysScript();
		
		String id = s.getId();
		
		if (StringUtils.isNotEmpty(id)) {
			Optional<SysScript> g = scriptRepository.findById(id);
			script = g.get();
		}else {
			script.setId(""+UUID.randomUUID());
		}
		
		script.setCode(s.getCode());
		script.setName(s.getName());
		
		return scriptRepository.save(script);
	}

	public void deleteById(String id) {
		scriptRepository.deleteById(id);
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

}
