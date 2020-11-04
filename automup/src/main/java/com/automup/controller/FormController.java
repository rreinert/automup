package com.automup.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.automup.entity.Form;
import com.automup.entity.RestResult;
import com.automup.entity.Script;
import com.automup.repository.FormRepository;
import com.automup.service.ScriptService;

@Controller
@RequestMapping("/form")
public class FormController {

    @Autowired
    private FormRepository formRepository;
    
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> list() {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		List<Form> ret = new ArrayList<Form>();
            Iterable<Form> forms = formRepository.findAll();
            for (Form form : forms) {
				form.setName("<a href='#' onclick='selectForm("+form.getId()+");'>"+form.getName()+"</a>");
            	ret.add(form);
			}
            
            return RestResult.create(ret, "");
        }).exceptionally(RestResult::create);
    }

    @RequestMapping(value = "/open", method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody String open(@RequestParam String id) {
        Long formId = Long.valueOf(id);
    	Optional<Form> form = formRepository.findById(formId);
    	Form f = form.get();
    	return f.getContent();
    }

	@RequestMapping(value = "/save", method = {RequestMethod.GET, RequestMethod.POST})
	public @ResponseBody String save(
			@RequestParam(name="id") Long id, 
			@RequestParam(name="name") String name, 
			@RequestParam(name="content") String content) {

		Form f = null;

        if (StringUtils.isEmpty(id)) {
            f = new Form();
            f.setName(name);
        }else {
            Long formId = Long.valueOf(id);
            Optional<Form> formFound = formRepository.findById(formId);
            
            if (formFound != null) {
            	f = formFound.get();
            }
        }		
		
    	f.setContent(content);
    	
    	Form saved = formRepository.save(f);
    	
    	return saved.getId().toString();
    }
	
    @RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public CompletableFuture<RestResult> delete(@RequestParam String id) {

    	return CompletableFuture.supplyAsync(() -> {
    		
    		PrintStream previousConsole = System.out;
    		
            ByteArrayOutputStream newConsole = new ByteArrayOutputStream();
            System.setOut(new PrintStream(newConsole));
    		
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Long formId = Long.valueOf(id);
            formRepository.deleteById(formId);
            
            System.setOut(previousConsole);
            
            return RestResult.create("", out.toString());
        }).exceptionally(RestResult::create);
    }
	
	@RequestMapping(value = "/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public @ResponseBody String submit(HttpServletRequest request) {

//		System.out.println(request.getParameter("submission[data][password]"));
    	
		scriptService.run("test", request);
		
    	return "";
    }

}
