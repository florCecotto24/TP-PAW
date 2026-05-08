#!/bin/bash
BASEDIR="/home/dorbaj/TP-PAW/persistence/src/main/java/ar/edu/itba/paw/persistence/hibernate"
for f in "$BASEDIR"/*HibernateDao.java; do
  # Add Transactional import after Repository import
  sed -i 's|import org.springframework.stereotype.Repository;|import org.springframework.stereotype.Repository;\nimport org.springframework.transaction.annotation.Transactional;|' "$f"
  # Add @Transactional before @Repository
  sed -i 's|@Repository|@Transactional\n@Repository|' "$f"
  echo "Updated: $(basename "$f")"
done
