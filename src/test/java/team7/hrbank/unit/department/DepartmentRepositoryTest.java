package team7.hrbank.unit.department;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team7.hrbank.config.QuerydslConfig;
import team7.hrbank.domain.department.dto.DepartmentMapper;
import team7.hrbank.domain.department.dto.DepartmentMapperImpl;
import team7.hrbank.domain.department.dto.DepartmentWithEmployeeCountResponseDto;
import team7.hrbank.domain.department.dto.PageDepartmentsResponseDto;
import team7.hrbank.domain.department.entity.Department;
import team7.hrbank.domain.department.repository.DepartmentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;
import static team7.hrbank.unit.department.util.DepartmentRepositoryUtil.*;

@Import({QuerydslConfig.class, DepartmentMapperImpl.class})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
public class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("name 기준 정렬 제대로 했는지 테스트")
    void sortPagingTest() {
        // given
        String nameOrDescription = "부서";
        Integer idAfter = 0;  // 커서 (현재 가져온 페이지의 마지막 id)
        String cursor = null; // 커서 (이전 페이지 마지막 커서)
        Integer requestSize = 10;
        String sortedField = "name";  // 정렬 필드(name or establishmentDate)
        String sortDirection = "ASC ";

        // 저장될 엔티티 수
        int entitySettingSize = 12;
        int repeatCount = 5;
        String otherNameOrDescriptionSize = "기서";  // ㅎㅇ 기서 ㅎㅇ  기서 ㅎㅇ ㅎㅇ
        for (int i = 0; i < repeatCount; i++) {
            setting_entity_save_and_containing_name(entitySettingSize, nameOrDescription);
            setting_entity_save_and_containing_name(entitySettingSize, otherNameOrDescriptionSize);
        }

        //when
        List<DepartmentWithEmployeeCountResponseDto> contentDTOList = new ArrayList<>();
        PageDepartmentsResponseDto result;
        do {
            result = departmentRepository.findDepartments(nameOrDescription, idAfter, cursor, requestSize, sortedField, sortDirection);
            contentDTOList.addAll(result.content());
            cursor = result.nextCursor();
            idAfter = Math.toIntExact(result.nextIdAfter());
        } while (result.hasNext());

        List<String> nameList = contentDTOList.stream()
                .map((dto) -> {
                    String name = dto.name();
                    return name.replaceAll("[-]+", "  "); // 하이픈(-)을 2번 공백으로 대체
                }).toList();

        // then 1. 기본 값 테스트
        assertThat(contentDTOList).as("필터를 거치고 실제로 가져온 DTO가 예상과 같은지")
                .hasSize(entitySettingSize * repeatCount)
                .as("네임만 따로 추츨한 개수가 Content개수와 맞는지")
                .hasSize(nameList.size())
                .as("전부 필터링 조건에 맞는지")
                .allSatisfy(department -> assertThat(department.name()).contains(nameOrDescription));

        // then 2. 정렬 테스트
        // 대소문자 구분 필요 : ASCII는 대문자가 더 작음 (Postgre는 신경 안씀)
        Comparator<String> caseInsensitiveOrder = String.CASE_INSENSITIVE_ORDER;
        if (sortDirection.trim().equalsIgnoreCase("desc")) {
            assertThat(nameList).as("내림차순 정렬")
                    .isSortedAccordingTo(caseInsensitiveOrder.reversed());
        } else {
            assertThat(nameList).as("오름차순 정렬")
                    .isSortedAccordingTo(caseInsensitiveOrder);
        }
    }


    @Test
    @DisplayName("establishmentDate 기준 정렬 제대로 했는지 테스트")
    void sortPagingTest2() {
        // given
        String nameOrDescription = "부서";
        Integer idAfter = 0;  // 커서 (현재 가져온 페이지의 마지막 id)
        String cursor = null; // 커서 (이전 페이지 마지막 커서)
        Integer requestSize = 10;
        String sortedField = "establishmentDate"; // 정렬 필드(name or establishmentDate)
        String sortDirection = "asc ";

        // 저장될 엔티티 수
        int entitySettingSize = 12;
        int repeatCount = 5;
        String otherNameOrDescriptionSize = "기서";
        for (int i = 0; i < repeatCount; i++) {
            setting_entity_save_and_containing_name(entitySettingSize, nameOrDescription);
            setting_entity_save_and_containing_name(entitySettingSize, otherNameOrDescriptionSize);
        }

        //when
        List<DepartmentWithEmployeeCountResponseDto> contentDTOList = new ArrayList<>();
        PageDepartmentsResponseDto result;
        do {
            result = departmentRepository.findDepartments(nameOrDescription, idAfter, cursor, requestSize, sortedField, sortDirection);
            contentDTOList.addAll(result.content());
            cursor = result.nextCursor();
            idAfter = Math.toIntExact(result.nextIdAfter());
        } while (result.hasNext());

        // then 1. 기본 값 테스트
        assertThat(contentDTOList).as("필터를 거치고 실제로 가져온 DTO가 예상과 같은지")
                .hasSize(entitySettingSize * repeatCount)
                .as("전부 필터링 조건에 맞는지")
                .allSatisfy(department -> assertThat(department.name()).contains(nameOrDescription));

        // then 2. 정렬 테스트
        if (sortDirection.trim().equalsIgnoreCase("desc")) {
            assertThat(contentDTOList).as("Established 내림차순 정렬 확인 후 같을 경우 -> 2번 째 정렬 기준인 id를 내림차순 정렬 확인")
                    .isSortedAccordingTo(
                            Comparator.comparing(DepartmentWithEmployeeCountResponseDto::establishedDate, Comparator.reverseOrder())
                                    .thenComparing(DepartmentWithEmployeeCountResponseDto::id, Comparator.reverseOrder())
                    );

        } else {
            assertThat(contentDTOList).as("Established 오름차순 정렬 확인 후 같을 경우 -> 2번 째 정렬 기준인 id를 오름차순 정렬 확인")
                    .isSortedAccordingTo(
                            Comparator.comparing(DepartmentWithEmployeeCountResponseDto::establishedDate, Comparator.naturalOrder())
                                    .thenComparing(DepartmentWithEmployeeCountResponseDto::id));
        }
    }






    private void setting_entity_save_and_containing_name(int entityCountOfNumber, String containingWord) {
        // 1. name 추출
        Set<String> departmentName = get_department_name_common_Word(entityCountOfNumber, containingWord);
        // 2. description 및 establishedDate 추출
        List<String> descriptionList = get_department_description(entityCountOfNumber);
        List<LocalDate> establishedDateList = get_LocalDates(entityCountOfNumber);
        List<String> nameList = departmentName.stream().toList();
        // 3. entity 저장
        List<Department> departmentList = new ArrayList<>();
        for (int i = 0; i < entityCountOfNumber; i++) {
            departmentList.add(new Department(nameList.get(i), descriptionList.get(i), establishedDateList.get(i)));
        }
        departmentRepository.saveAllAndFlush(departmentList);
        em.clear();
    }


    private void setting_entity_save_and_containing_description(int entityCountOfNumber, String containingWord) {
        Set<String> departmentName = get_department_name(entityCountOfNumber);
        List<String> descriptionList = get_department_description_common_word(entityCountOfNumber, containingWord);
        List<LocalDate> establishedDateList = get_LocalDates(entityCountOfNumber);
        List<String> nameList = departmentName.stream().toList();

        // 4. entity 저장
        List<Department> departmentList = new ArrayList<>();
        for (int i = 0; i < entityCountOfNumber; i++) {
            departmentList.add(new Department(nameList.get(i), descriptionList.get(i), establishedDateList.get(i)));
        }
        departmentRepository.saveAllAndFlush(departmentList);
        em.clear();
    }
}
