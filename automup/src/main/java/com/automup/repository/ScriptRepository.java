package com.automup.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.automup.entity.Script;

@Repository
public interface ScriptRepository extends CrudRepository<Script, Long>{
	
	@Query("FROM Script s WHERE s.name LIKE :name")
	Script findByName(@Param("name") String name);

}
