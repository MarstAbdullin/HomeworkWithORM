package ormSpring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ormSpring.models.Document;

@Repository
public interface DocumentsRepository extends JpaRepository<Document, Long> {
}
