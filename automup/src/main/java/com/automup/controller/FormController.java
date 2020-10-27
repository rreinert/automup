package com.automup.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.automup.entity.Form;
import com.automup.repository.FormRepository;
import com.automup.service.ScriptService;

@Controller
@RequestMapping("/form")
public class FormController {

    @Autowired
    private FormRepository formRepository;
    
    @Autowired
    private ScriptService scriptService;
    
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

    	Form f = new Form();
    	if (id != null) {
        	Optional<Form> found = formRepository.findById(id);
        	f = found.get();
    	}
    	f.setContent(content);
    	f.setName(name);
    	
    	Form saved = formRepository.save(f);
    	
    	return saved.getId().toString();
    }
	
	@RequestMapping(value = "/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public @ResponseBody String submit(HttpServletRequest request) {

//		System.out.println(request.getParameter("submission[data][password]"));
    	
		scriptService.run("test", request);
		
    	return "";
    }

}
