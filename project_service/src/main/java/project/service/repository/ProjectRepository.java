package project.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import project.service.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	boolean existsByThumbnail(String thumbnail);
}
