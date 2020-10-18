package com.automup.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.automup.entity.Form;
import com.automup.entity.Script;

@Repository
public interface FormRepository extends CrudRepository<Form, Long>{
	
	@Query("FROM Form f WHERE f.name LIKE :name")
	Script findByName(@Param("name") String name);

}
