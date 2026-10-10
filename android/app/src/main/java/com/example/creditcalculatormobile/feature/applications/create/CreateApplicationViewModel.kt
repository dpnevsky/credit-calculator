package com.example.creditcalculatormobile.feature.applications.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.core.session.SessionManager
import com.example.creditcalculatormobile.data.model.CreateApplicationRequestDto
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto
import com.example.creditcalculatormobile.data.model.EmploymentStatusDto
import com.example.creditcalculatormobile.data.model.GenderDto
import com.example.creditcalculatormobile.data.model.MaritalStatusDto
import com.example.creditcalculatormobile.data.model.PositionDto
import com.example.creditcalculatormobile.data.model.SubmitApplicationRequestDto
import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateApplicationFormState(
    val amount: String = "500000",
    val termMonths: String = "12",
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val birthDate: String = "",
    val passportSeries: String = "",
    val passportNumber: String = "",
    val passportIssueDate: String = "",
    val passportIssueBranch: String = "",
    val gender: GenderDto = GenderDto.MALE,
    val maritalStatus: MaritalStatusDto = MaritalStatusDto.SINGLE,
    val dependentAmount: String = "0",
    val employmentStatus: EmploymentStatusDto = EmploymentStatusDto.EMPLOYED,
    val employerInn: String = "",
    val salary: String = "100000",
    val position: PositionDto = PositionDto.OTHER,
    val workExperienceTotal: String = "24",
    val workExperienceCurrent: String = "6",
    val insuranceEnabled: Boolean = true,
    val salaryClient: Boolean = false,
    val accountNumber: String = "",
    val confirmation: Boolean = false,
) {
    val canSubmit: Boolean
        get() = confirmation && amount.isNotBlank() && termMonths.isNotBlank() &&
                firstName.isNotBlank() && lastName.isNotBlank() && birthDate.isNotBlank() &&
                passportSeries.length == 4 && passportNumber.length == 6 &&
                passportIssueDate.isNotBlank() && isPassportIssueBranchValid(passportIssueBranch)
}

fun isPassportIssueBranchValid(value: String): Boolean {
    return Regex("^\\d{3}-\\d{3}$").matches(value)
}

fun formatPassportIssueBranch(input: String): String {
    val digits = input.filter { it.isDigit() }.take(6)
    return if (digits.length <= 3) {
        digits
    } else {
        digits.substring(0, 3) + "-" + digits.substring(3)
    }
}

data class CreateApplicationUiState(
    val form: CreateApplicationFormState = CreateApplicationFormState(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val createdApplicationId: String? = null,
)

class CreateApplicationViewModel(
    private val applicationsRepository: ApplicationsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreateApplicationUiState(
            form = applyUserPrefill(
                base = CreateApplicationFormState(),
                user = sessionManager.currentUserOrNull(),
            ),
        ),
    )
    val uiState: StateFlow<CreateApplicationUiState> = _uiState.asStateFlow()

    fun updateAmount(value: String) = updateForm { copy(amount = sanitizeNumber(value)) }
    fun updateTermMonths(value: String) = updateForm { copy(termMonths = sanitizeDigits(value)) }
    fun updateFirstName(value: String) = updateForm { copy(firstName = value) }
    fun updateLastName(value: String) = updateForm { copy(lastName = value) }
    fun updateMiddleName(value: String) = updateForm { copy(middleName = value) }
    fun updateBirthDate(value: String) = updateForm { copy(birthDate = value.trim()) }
    fun updatePassportSeries(value: String) = updateForm { copy(passportSeries = sanitizeDigits(value, 4)) }
    fun updatePassportNumber(value: String) = updateForm { copy(passportNumber = sanitizeDigits(value, 6)) }

    fun updatePassportIssueDate(value: String) = updateForm { copy(passportIssueDate = value.trim()) }

    fun updatePassportIssueBranch(value: String) = updateForm {
        copy(passportIssueBranch = formatPassportIssueBranch(value))
    }

    fun updateGender(value: GenderDto) = updateForm { copy(gender = value) }
    fun updateMaritalStatus(value: MaritalStatusDto) = updateForm { copy(maritalStatus = value) }
    fun updateDependentAmount(value: String) = updateForm { copy(dependentAmount = sanitizeDigits(value)) }
    fun updateEmploymentStatus(value: EmploymentStatusDto) = updateForm { copy(employmentStatus = value) }

    fun updateEmployerInn(value: String) = updateForm {
        copy(employerInn = sanitizeDigits(value, 12))
    }

    fun updateSalary(value: String) = updateForm { copy(salary = sanitizeNumber(value)) }
    fun updatePosition(value: PositionDto) = updateForm { copy(position = value) }
    fun updateWorkExperienceTotal(value: String) = updateForm { copy(workExperienceTotal = sanitizeDigits(value)) }
    fun updateWorkExperienceCurrent(value: String) = updateForm { copy(workExperienceCurrent = sanitizeDigits(value)) }
    fun updateInsuranceEnabled(value: Boolean) = updateForm { copy(insuranceEnabled = value) }

    fun updateSalaryClient(value: Boolean) {
        val currentForm = _uiState.value.form
        if (value && currentForm.accountNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Сначала введите номер счета") }
            return
        }
        updateForm { copy(salaryClient = value) }
    }

    fun updateAccountNumber(value: String) {
        val digits = sanitizeDigits(value, 20)
        updateForm {
            copy(
                accountNumber = digits,
                salaryClient = if (digits.isBlank()) false else salaryClient
            )
        }
    }

    fun updateConfirmation(value: Boolean) = updateForm { copy(confirmation = value) }

    fun clearForm() {
        _uiState.value = _uiState.value.copy(
            form = applyUserPrefill(CreateApplicationFormState(), sessionManager.currentUserOrNull()),
            errorMessage = null,
            createdApplicationId = null
        )
    }

    fun submit() {
        if (_uiState.value.loading) return
        val form = _uiState.value.form
        val validationError = validate(form)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val createRequest = CreateApplicationRequestDto(
                amount = form.amount.toDouble(),
                termMonths = form.termMonths.toInt(),
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                middleName = form.middleName.trim().ifBlank { null },
                birthDate = form.birthDate.trim(),
                passportSeries = form.passportSeries.trim(),
                passportNumber = form.passportNumber.trim()
            )

            val createResult = applicationsRepository.createApplication(createRequest)
            if (createResult is ApiResult.Failure) {
                _uiState.update { it.copy(loading = false, errorMessage = createResult.error.message) }
                return@launch
            }

            val created = (createResult as ApiResult.Success).value
            val appId = created.applicationId
            if (created.status != "DRAFT") {
                _uiState.update { it.copy(loading = false, createdApplicationId = appId) }
                return@launch
            }

            val submitRequest = SubmitApplicationRequestDto(
                insuranceEnabled = form.insuranceEnabled,
                salaryClient = form.salaryClient,
                gender = form.gender,
                maritalStatus = form.maritalStatus,
                dependentAmount = form.dependentAmount.toIntOrNull() ?: 0,
                passportIssueDate = form.passportIssueDate,
                passportIssueBranch = form.passportIssueBranch,
                accountNumber = form.accountNumber.trim().ifBlank { null },
                employment = SubmitApplicationRequestDto.EmploymentRequestDto(
                    employmentStatus = form.employmentStatus,
                    employerInn = form.employerInn.trim().ifBlank { null },
                    salary = form.salary.toDouble(),
                    position = form.position,
                    workExperienceTotal = form.workExperienceTotal.toIntOrNull() ?: 0,
                    workExperienceCurrent = form.workExperienceCurrent.toIntOrNull() ?: 0
                )
            )

            val submitResult = applicationsRepository.submitApplication(appId, submitRequest)
            when (submitResult) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(loading = false, createdApplicationId = appId) }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(
                        loading = false,
                        errorMessage = "Заявка создана, но скоринг не пройден: ${submitResult.error.message}",
                        createdApplicationId = appId
                    ) }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(createdApplicationId = null) }
    }

    private fun updateForm(block: CreateApplicationFormState.() -> CreateApplicationFormState) {
        _uiState.update { it.copy(
            form = it.form.block(),
            errorMessage = null
        ) }
    }

    private fun validate(form: CreateApplicationFormState): String? {
        val amount = form.amount.toDoubleOrNull() ?: 0.0
        if (!amount.isFinite() || amount < 20000) return "Минимальная сумма кредита — 20 000 ₽"

        val term = form.termMonths.toIntOrNull() ?: 0
        if (term !in 6..360) return "Срок кредита должен быть от 6 до 360 месяцев"
        if (!form.confirmation) return "Подтвердите согласие на обработку данных"
        val birthDate = runCatching { LocalDate.parse(form.birthDate) }.getOrNull()
        val issueDate = runCatching { LocalDate.parse(form.passportIssueDate) }.getOrNull()
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) return "Укажите корректную дату рождения"
        if (issueDate == null || !issueDate.isBefore(LocalDate.now())) return "Укажите корректную дату выдачи паспорта"

        if (form.firstName.isBlank() || form.lastName.isBlank()) return "Пожалуйста, заполните Имя и Фамилию"

        if (form.passportSeries.length != 4 || form.passportNumber.length != 6) {
            return "Серия паспорта должна быть 4 цифры, номер — 6 цифр"
        }

        if (form.passportIssueDate.isBlank()) return "Укажите дату выдачи паспорта"
        if (!isPassportIssueBranchValid(form.passportIssueBranch)) {
            return "Код подразделения должен быть в формате 000-000"
        }

        if (form.employmentStatus != EmploymentStatusDto.UNEMPLOYED) {
            if (form.employerInn.length != 10 && form.employerInn.length != 12) {
                return "ИНН работодателя должен состоять из 10 или 12 цифр"
            }
        }

        if (form.salaryClient && form.accountNumber.isBlank()) return "Для зарплатных клиентов номер счета обязателен"

        val salary = form.salary.toDoubleOrNull() ?: return "Укажите корректный ежемесячный доход"
        if (!salary.isFinite() || salary < 0) return "Укажите корректный ежемесячный доход"
        if (form.dependentAmount.toIntOrNull() == null) return "Укажите корректное количество иждивенцев"
        val total = form.workExperienceTotal.toIntOrNull()
        val current = form.workExperienceCurrent.toIntOrNull()
        if (total == null || current == null || current > total) return "Проверьте общий стаж и стаж на текущем месте"

        return null
    }

    companion object {
        fun factory(applicationsRepository: ApplicationsRepository, sessionManager: SessionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateApplicationViewModel(applicationsRepository, sessionManager) as T
                }
            }

        private fun sanitizeDigits(v: String, limit: Int? = null): String {
            val digits = v.filter { it.isDigit() }
            return if (limit != null) digits.take(limit) else digits
        }

        private fun sanitizeNumber(v: String) = buildString {
            v.forEach { if (it.isDigit() || it == '.' || it == ',') append(if (it == ',') '.' else it) }
        }.let { s ->
            val dots = s.count { it == '.' }
            if (dots > 1) s.substringBeforeLast('.') else s
        }

        private fun applyUserPrefill(base: CreateApplicationFormState, user: CurrentUserProfileDto?): CreateApplicationFormState {
            if (user == null) return base
            return base.copy(
                firstName = user.firstName ?: base.firstName,
                lastName = user.lastName ?: base.lastName,
                middleName = user.middleName ?: base.middleName,
                birthDate = user.birthDate ?: base.birthDate
            )
        }
    }
}
