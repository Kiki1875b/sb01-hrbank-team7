package team7.hrbank.unit.department;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import team7.hrbank.domain.department.dto.DepartmentCreateRequest;
import team7.hrbank.domain.department.dto.DepartmentMapper;
import team7.hrbank.domain.department.dto.DepartmentMapperImpl;
import team7.hrbank.domain.department.dto.DepartmentResponseDto;
import team7.hrbank.domain.department.entity.Department;
import team7.hrbank.domain.department.repository.DepartmentRepository;
import team7.hrbank.domain.department.service.DepartmentServiceImpl;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceTest.class);
    @Spy
    private DepartmentMapper departmentMapper = new DepartmentMapperImpl();

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks // 직접 생성하는 것이기 때문에 인터페이스말고 구현체로
    private DepartmentServiceImpl departmentService;

    @Test // 연관된 의존성 : departmentMapper,
    void create(){
        // given
        String beforeName = "기획 관리 부서";  // 일부러 띄어쓰기 적용되는지 테스트
        String beforeDescription = "부서 설명";
        DepartmentCreateRequest requestDTO = new DepartmentCreateRequest(beforeName, beforeDescription, LocalDate.now());
        // stub 세팅
        when(departmentRepository.findByName(anyString())).thenReturn(Optional.empty()); // 이름 검증은 항상 ture
        stub_repository_save();

        // when
        DepartmentResponseDto responseDTO = departmentService.create(requestDTO);

        // then 1 : 의존된 내부 메서드들이 호출됐는지 안됐는지만 검증 (mapper는 중요치 않아서 굳이 넣을 필요있나)
        verify(departmentRepository, timeout(1)).save(any(Department.class));
        verify(departmentMapper, timeout(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(any(Department.class));
        verify(departmentRepository, times(1)).findByName(anyString());

        // then 2 : 리턴값 검증
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isNotNull();
        assertThat(responseDTO.name()).isEqualTo(beforeName);
        assertThat(responseDTO.description()).isEqualTo(beforeDescription);
    }

    @Test  // 연관된 의존성 : departmentRepository
    @DisplayName("create 메서드에서 이름 체크 잘 되는지 테스트") // 테스트 결과 : 실패 Cuz validateName 메서드가 부서명에 띄어쓰기가 있으면 오류를 발생시킨다
    void validateNameSpaceForCreate(){
        // given
        String duplicateName = "중복 될 부서명";
        String description = "설명 입니다";
        lenient().when(departmentRepository.findByName(anyString())).thenReturn(Optional.of(getSavedDepartment(duplicateName, description)));
        // 	thenAnswer() : 동적으로 값을 생성할 때 사용
        stub_repository_save();

        // when, then  ---
        DepartmentCreateRequest emptyNameDto = new DepartmentCreateRequest(" ", "create를 위한 부서 설명", LocalDate.now());
        DepartmentCreateRequest duplicateNameDto = new DepartmentCreateRequest(duplicateName, "create를 위한 부서 설명", LocalDate.now());

        // 어떻게 할지 코멘트 듣고 고치기
        assertThatThrownBy(() -> departmentService.create(emptyNameDto))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("부서 이름에 공백은 포함될");
    }


    @Test  // 연관된 의존성 : departmentRepository
    @DisplayName("create 메서드에서 이름 체크 잘 되는지 테스트") // 테스트 결과 : 실패 Cuz validateName 메서드가 부서명에 띄어쓰기가 있으면 오류를 발생시킨다
    void validateDuplicatedNameForCreate(){
        // given
        String duplicateName = "중복부서명";
        String description = "설명 입니다";
        lenient().when(departmentRepository.findByName(anyString())).thenReturn(Optional.of(getSavedDepartment(duplicateName, description)));
        // 	thenAnswer() : 동적으로 값을 생성할 때 사용
        stub_repository_save();

        // when,
        DepartmentCreateRequest duplicateNameDto = new DepartmentCreateRequest(duplicateName, "create를 위한 부서 설명", LocalDate.now());

        //then
        assertThatThrownBy(() -> departmentService.create(duplicateNameDto))
                .isInstanceOf(InvalidParameterException.class)
                // .hasMessageContaining("부서 이름에 공백은 포함될 ");
                .hasMessageContaining("이미 존재하는 ");

    }




    private void stub_repository_save() {
        lenient().when(departmentRepository.save(any(Department.class))).thenAnswer(invocationOnMock -> {
            Department department = invocationOnMock.getArgument(0);
            ReflectionTestUtils.setField(department, "id", autoIncrementId.get());
            ReflectionTestUtils.setField(department, "createdAt", Instant.now());
            return department;
        });
    }

    private Department getSavedDepartment(String name, String description) {
        Department department = new Department(name, description, LocalDate.now());
        ReflectionTestUtils.setField(department, "id", autoIncrementId.get());
        ReflectionTestUtils.setField(department, "createdAt", Instant.now());
        return department;
    }


    private static final AtomicLong autoIncrementId = new AtomicLong(0L);

    @AfterAll
    static void afterTest() {
        autoIncrementId.set(0L);
    }
}
