package team7.hrbank.domain.emplyee_statistic.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.TreeMap;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team7.hrbank.domain.change_log.dto.ChangeLogDashboardDto;
import team7.hrbank.domain.change_log.entity.ChangeLogType;
import team7.hrbank.domain.change_log.repository.ChangeLogRepository;
import team7.hrbank.domain.department.entity.Department;
import team7.hrbank.domain.department.repository.DepartmentRepository;
import team7.hrbank.domain.employee.dto.EmployeeDistributionDto;
import team7.hrbank.domain.employee.dto.EmployeeTrendDto;
import team7.hrbank.domain.employee.entity.Employee;
import team7.hrbank.domain.employee.entity.EmployeeStatus;
import team7.hrbank.domain.employee.repository.CustomEmployeeRepository;
import team7.hrbank.domain.employee.repository.EmployeeRepository;
import team7.hrbank.domain.emplyee_statistic.entity.EmployeeStatistic;
import team7.hrbank.domain.emplyee_statistic.repository.EmployeeStatisticRepository;
import team7.hrbank.domain.emplyee_statistic.entity.EmployeeStatisticType;


@Service
@RequiredArgsConstructor
public class EmployeeDashboardServiceImpl implements
    EmployeeDashboardService {

  private final EmployeeRepository employeeRepository;
  private final DepartmentRepository departmentRepository;
  private final CustomEmployeeRepository customEmployeeRepository;
  private final ChangeLogRepository changeLogRepository;

  private final EmployeeStatisticRepository statisticRepository;

  @Override
  public List<EmployeeTrendDto> getEmployeeTrendsV3(LocalDate from, LocalDate to, String unit) {
    if (from == null) {
      from = getFromIfNull(unit, to);
    }

    LocalDate start = from; // 계산을 위한 직전 unit
    LocalDate current = parseDate(start, unit, true);
    to = parseDate(to, unit, false);

    return switch (unit.toLowerCase()) {
      case "day" -> parseTrendDaily(from, to, EmployeeStatisticType.DAY);
      case "week" -> parseTrendDaily(from, to, EmployeeStatisticType.WEEK);
      case "month" -> parseTrendDaily(from, to, EmployeeStatisticType.MONTH);
      case "quarter" -> parseTrendDaily(from, to, EmployeeStatisticType.QUARTER);
      case "year" -> parseTrendDaily(from, to, EmployeeStatisticType.YEAR);
      default -> throw new IllegalArgumentException();
    };
  }

  private List<EmployeeTrendDto> parseTrendDaily(LocalDate from, LocalDate to, EmployeeStatisticType type) {
    List<EmployeeStatistic> statistics = statisticRepository.findByCaptureDateBetweenAndTypeOrderByCaptureDate(from,
        to,
        type);

    List<EmployeeTrendDto> returnDtoList = new ArrayList<>();
    int prev = 0;

    for (EmployeeStatistic stat : statistics) {
      int current = stat.getEmployeeCount();
      int diff = current - prev;
      double changeRate = calculateRate(prev, diff);
      prev = current;
      returnDtoList.add(
          new EmployeeTrendDto(stat.getCaptureDate(), current, diff, changeRate)
      );
    }

    return returnDtoList;
  }

  /**
   * DEPRECATED
   */
  @Override
  public List<EmployeeTrendDto> getEmployeeTrendsV2(LocalDate from, LocalDate to, String unit) {

    if (from == null) {
      from = getFromIfNull(unit, to);
    }

    LocalDate start = from; // 계산을 위한 직전 unit
    LocalDate current = parseDate(start, unit, true); //
    to = parseDate(to, unit, false);

    List<ChangeLogDashboardDto> changeHistory = changeLogRepository.findAllByTypeNotInOrderByCreatedAt(
        List.of(ChangeLogType.UPDATED));

    Map<LocalDate, Integer> changeByDay = new HashMap<>();
    Instant instant = current.atStartOfDay(ZoneId.systemDefault()).toInstant();

    long startCount =
        changeHistory.stream()
            .filter(history ->
                history.createdAt()
                    .isBefore(instant)
                    && history.type().equals(ChangeLogType.CREATED))
            .count()
            - changeHistory.stream()
            .filter(history ->
                history.createdAt()
                    .isBefore(instant)
                    && history.type().equals(ChangeLogType.DELETED))
            .count();

    for (ChangeLogDashboardDto dto : changeHistory) {
      LocalDate date = dto.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
      if (dto.type().equals(ChangeLogType.CREATED)) {
        changeByDay.put(date, changeByDay.getOrDefault(date, 0) + 1);
      } else if (dto.type().equals(ChangeLogType.DELETED)) {
        changeByDay.put(date, Math.max(changeByDay.getOrDefault(date, 0) - 1, 0));
      }
    }

    return switch (unit.toLowerCase()) {
      case "year" -> parseTrend(startCount, changeByDay, from, to, ChronoUnit.YEARS);
      case "quarter" -> parseTrend(startCount, changeByDay, from, to, IsoFields.QUARTER_YEARS);
      case "month" -> parseTrend(startCount, changeByDay, from, to, ChronoUnit.MONTHS);
      case "week" -> parseTrend(startCount, changeByDay, from, to, ChronoUnit.WEEKS);
      case "day" -> parseTrend(startCount, changeByDay, from, to, ChronoUnit.DAYS);
      default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
    };
  }

  @Override
  @Transactional
  public List<EmployeeDistributionDto> getEmployeeDistribution(String groupBy,
      EmployeeStatus status) {

    List<Employee> employees = employeeRepository.findByStatus(status);
    List<Department> departments = departmentRepository.findAll();
    int totalCount = employees.size();

    Map<String, Long> groupData = "department".equalsIgnoreCase(groupBy) ?
        employees.stream()
            .collect(Collectors.groupingBy(e -> e.getDepartment().getName(), Collectors.counting()))
        : employees.stream()
            .collect(Collectors.groupingBy(Employee::getPosition, Collectors.counting()));

    return groupData.entrySet().stream()
        .map(entry -> new EmployeeDistributionDto(
            entry.getKey(), entry.getValue().intValue(), entry.getValue() * 100.0 / totalCount
        )).collect(Collectors.toList());
  }

  @Override
  public Long getEmployeeCountByCriteria(EmployeeStatus status, LocalDate from, LocalDate to) {
    return customEmployeeRepository.getEmployeeCountByCriteria(status, from, to);
  }

  /**
   * DEPRECATED
   */

  private List<EmployeeTrendDto> parseTrend(long startCount, Map<LocalDate, Integer> dateMap,
      LocalDate from, LocalDate to, TemporalUnit unit) {

    List<EmployeeTrendDto> trends = new ArrayList<>();

    Map<LocalDate, Integer> prefixSum = new TreeMap<>();
    int sum = 0;

    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      sum += dateMap.getOrDefault(date, 0);
      prefixSum.put(date, sum);
    }

    int previousCount = (int) startCount;
    trends.add(new EmployeeTrendDto(from.minus(1, unit), previousCount, 0, 0));

    LocalDate currentFrom = from;

    while (!currentFrom.isAfter(to)) {
      LocalDate periodEnd = currentFrom.plus(1, unit);

      int currentChangeSum = prefixSum.getOrDefault(periodEnd, 0)
          - prefixSum.getOrDefault(currentFrom, 0);

      int currentTotal = previousCount + currentChangeSum;
      int diff = currentTotal - previousCount;

      trends.add(new EmployeeTrendDto(currentFrom.plus(1, unit), currentTotal, diff,
          calculateRate(previousCount, diff)));

      previousCount = currentTotal;
      currentFrom = currentFrom.plus(1, unit);
    }

    trends.remove(trends.size() - 1);
    return trends;
  }

  private double calculateRate(int previousCount, int diff) {
    if (previousCount == 0 && diff != 0) {
      return 100;
    }
    if (previousCount == 0) {
      return 0;
    }
    return (int) ((diff / (double) previousCount) * 100);
  }


  private LocalDate parseDate(LocalDate date, String unit, boolean isFrom) {
    if (isFrom) {
      if (unit.equalsIgnoreCase("year")) {
        return LocalDate.of(date.getYear(), 1, 1);
      } else if (unit.equalsIgnoreCase("month") || unit.equalsIgnoreCase("quarter")) {
        return LocalDate.of(date.getYear(), date.getMonth(), 1);
      }
    } else if (!isFrom) {
      if (unit.equalsIgnoreCase("year")) {
        return LocalDate.of(date.getYear(), 12, 31);
      } else if (unit.equalsIgnoreCase("month") || unit.equalsIgnoreCase("quarter")) {
        YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());
        return yearMonth.atEndOfMonth();
      } else if (unit.equalsIgnoreCase("week")) {
        return date;
      } else if (unit.equalsIgnoreCase("day")) {
        return date;
      }
    }

    return date;
  }

  private LocalDate getFromIfNull(String unit, LocalDate to) {
    switch (unit.toLowerCase()) {
      case "day":
        return to.minusDays(12);
      case "week":
        return to.minusWeeks(12);
      case "month":
        return to.minusMonths(12);
      case "quarter":
        return to.minusMonths(12 * 3);
      case "year":
        return to.minusYears(12);
      default:
        return to.minusMonths(12);
    }
  }
}
