package com.globalis.api.movidesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.globalis.api.movidesk.model.MovideskAction;

@Repository
public interface MovideskActionRepository extends JpaRepository<MovideskAction, MovideskAction.MovideskActionId> {
}
