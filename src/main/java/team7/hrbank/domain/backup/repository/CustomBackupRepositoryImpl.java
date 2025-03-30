package team7.hrbank.domain.backup.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team7.hrbank.domain.backup.dto.BackupListRequestDto;
import team7.hrbank.domain.backup.entity.Backup;
import team7.hrbank.domain.backup.entity.QBackup;

//@Repository
@RequiredArgsConstructor
public class CustomBackupRepositoryImpl implements CustomBackupRepository {

  private final JPAQueryFactory queryFactory;
  private final QBackup backup = QBackup.backup;

  /**
   * Custom repository implementation for querying backup records using QueryDSL.
   *
   * <p>This repository provides methods for retrieving backup records with dynamic filtering
   * and sorting criteria based on request parameters.</p>
   *
   * @see team7.hrbank.domain.backup.repository.CustomBackupRepository
   */
  @Override
  public List<Backup> findBackups(
      BackupListRequestDto dto,
      int size,
      String sortField,
      String sortDirection
  ) {

    BooleanBuilder where = new BooleanBuilder();

    if (dto.worker() != null) {
      where.and(backup.worker.containsIgnoreCase(dto.worker()));
    }

    if (dto.status() != null) {
      where.and(backup.status.eq(dto.status()));
    }

    if (dto.startedAtFrom() != null) {
        where.and(backup.startedAt.goe(dto.startedAtFrom()));
    }

    if (dto.startedAtTo() != null) {
        where.and(backup.startedAt.loe(dto.startedAtTo()));
    }

    if (dto.idAfter() != null) {

      if ("DESC".equalsIgnoreCase(sortDirection)) {
        where.and(backup.id.loe(dto.idAfter()));
      } else {
        where.and(backup.id.goe(dto.idAfter()));
      }// 테스트 후 gt 로 변경해야 할 수도
    }

    if (dto.cursor() != null) {

      if ("startedat".equalsIgnoreCase(sortField)) {
        where.and(
            "DESC".equalsIgnoreCase(sortDirection)
                ? backup.startedAt.lt(dto.cursor())
                : backup.startedAt.gt(dto.cursor())
        );
      } else {
        where.and(
            "DESC".equalsIgnoreCase(sortDirection)
                ? backup.endedAt.lt(dto.cursor())
                : backup.endedAt.gt(dto.cursor())
        );
      }
    }

    OrderSpecifier<?> specifier = getOrderSpecifier(sortField, sortDirection);

    return queryFactory
        .selectFrom(backup)
        .where(where)
        .orderBy(specifier)
        .limit(size + 1)
        .fetch();
  }

  private OrderSpecifier<?> getOrderSpecifier(String field, String direction) {
    switch (field) {
      case "startedAt":
        if ("DESC".equalsIgnoreCase(direction)) {
          return backup.startedAt.desc();
        } else {
          return backup.startedAt.asc();
        }
      case "endedAt":
        if ("DESC".equalsIgnoreCase(direction)) {
          return backup.endedAt.desc();
        } else {
          return backup.endedAt.asc();
        }
      case "status":
        return backup.status.asc();
      default:
        return backup.startedAt.desc();
    }
  }
}
