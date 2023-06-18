package com.automup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.automup.domain.SysScript;

@Repository
public interface ScriptRepository extends CrudRepository<SysScript, String>{
	
	@Query("FROM SysScript s WHERE s.name LIKE :name")
	List<SysScript> findByName(@Param("name") String name);

}
