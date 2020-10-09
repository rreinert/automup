package com.automup.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.automup.entity.Script;

@Repository
public interface ScriptRepository extends CrudRepository<Script, Long>{

}
