package com.example.creditcalculatormobile.feature.applications.create

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.*
import com.example.creditcalculatormobile.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun CreateApplicationRoute(
    appContainer: AppContainer,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateApplicationViewModel = viewModel(
        factory = CreateApplicationViewModel.factory(
            applicationsRepository = appContainer.applicationsRepository,
            sessionManager = appContainer.sessionManager,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.createdApplicationId) {
        val applicationId = uiState.createdApplicationId
        if (!applicationId.isNullOrBlank()) {
            viewModel.consumeNavigation()
            onCreated(applicationId)
        }
    }

    CreateApplicationScreen(
        uiState = uiState,
        viewModel = viewModel,
        modifier = modifier,
    )
}

@Composable
fun CreateApplicationScreen(
    uiState: CreateApplicationUiState,
    viewModel: CreateApplicationViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun showDatePicker(currentDate: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val date = try {
            LocalDate.parse(currentDate)
        } catch (e: Exception) {
            LocalDate.now()
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(selected.format(DateTimeFormatter.ISO_LOCAL_DATE))
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "Оформление заявки",
                subtitle = "Заполните анкету для получения решения по кредиту.",
            )
        }

        // Блок 1. Параметры кредита
        item {
            InfoCard(title = "Параметры кредита") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = uiState.form.amount,
                        onValueChange = viewModel::updateAmount,
                        label = "Сумма кредита (₽)",
                        placeholder = "От 20 000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )
                    AppTextField(
                        value = uiState.form.termMonths,
                        onValueChange = viewModel::updateTermMonths,
                        label = "Срок кредита (мес.)",
                        placeholder = "От 6",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )
                }
            }
        }

        // Блок 2. Персональные данные
        item {
            InfoCard(title = "Персональные данные") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(value = uiState.form.lastName, onValueChange = viewModel::updateLastName, label = "Фамилия")
                    AppTextField(value = uiState.form.firstName, onValueChange = viewModel::updateFirstName, label = "Имя")
                    AppTextField(value = uiState.form.middleName, onValueChange = viewModel::updateMiddleName, label = "Отчество (при наличии)")

                    Box(modifier = Modifier.clickable { showDatePicker(uiState.form.birthDate, viewModel::updateBirthDate) }) {
                        AppTextField(
                            value = uiState.form.birthDate,
                            onValueChange = {},
                            label = "Дата рождения",
                            placeholder = "ГГГГ-ММ-ДД",
                            readOnly = true,
                            enabled = false
                        )
                    }
                }
            }
        }

        // Блок 3. Паспортные данные
        item {
            InfoCard(title = "Паспортные данные") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(
                            value = uiState.form.passportSeries,
                            onValueChange = viewModel::updatePassportSeries,
                            label = "Серия",
                            placeholder = "4 цифры",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        AppTextField(
                            value = uiState.form.passportNumber,
                            onValueChange = viewModel::updatePassportNumber,
                            label = "Номер",
                            placeholder = "6 цифр",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Дата выдачи паспорта
                    Box(modifier = Modifier.clickable { showDatePicker(uiState.form.passportIssueDate, viewModel::updatePassportIssueDate) }) {
                        AppTextField(
                            value = uiState.form.passportIssueDate,
                            onValueChange = {},
                            label = "Дата выдачи паспорта",
                            placeholder = "ГГГГ-ММ-ДД",
                            readOnly = true,
                            enabled = false
                        )
                    }

                    // Код подразделения
                    AppTextField(
                        value = uiState.form.passportIssueBranch,
                        onValueChange = viewModel::updatePassportIssueBranch,
                        label = "Код подразделения",
                        placeholder = "000-000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        // Блок 4. Семейное положение
        item {
            InfoCard(title = "Семейное положение") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnumDropdown(label = "Пол", selected = uiState.form.gender, options = GenderDto.entries, onSelected = viewModel::updateGender)
                    EnumDropdown(label = "Семейное положение", selected = uiState.form.maritalStatus, options = MaritalStatusDto.entries, onSelected = viewModel::updateMaritalStatus)
                    AppTextField(value = uiState.form.dependentAmount, onValueChange = viewModel::updateDependentAmount, label = "Кол-во иждивенцев", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        }

        // Блок 5. Занятость и доход
        item {
            InfoCard(title = "Занятость и доход") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnumDropdown(label = "Статус занятости", selected = uiState.form.employmentStatus, options = EmploymentStatusDto.entries, onSelected = viewModel::updateEmploymentStatus)

                    AppTextField(
                        value = uiState.form.employerInn,
                        onValueChange = viewModel::updateEmployerInn,
                        label = "ИНН работодателя",
                        placeholder = "10 или 12 цифр",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    AppTextField(value = uiState.form.salary, onValueChange = viewModel::updateSalary, label = "Ежемесячный доход", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    EnumDropdown(label = "Должность", selected = uiState.form.position, options = PositionDto.entries, onSelected = viewModel::updatePosition)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(value = uiState.form.workExperienceTotal, onValueChange = viewModel::updateWorkExperienceTotal, label = "Общий стаж (мес.)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        AppTextField(value = uiState.form.workExperienceCurrent, onValueChange = viewModel::updateWorkExperienceCurrent, label = "Тек. стаж (мес.)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        }

        // Блок 7. Подтверждение
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Checkbox(checked = uiState.form.confirmation, onCheckedChange = viewModel::updateConfirmation)
                    Text("Я подтверждаю корректность всех введенных данных", style = MaterialTheme.typography.bodyMedium)
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    ErrorState(message = uiState.errorMessage)
                }

                PrimaryButton(
                    text = "Создать заявку",
                    onClick = viewModel::submit,
                    loading = uiState.loading,
                    enabled = uiState.form.canSubmit && !uiState.loading,
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryButton(
                    text = "Очистить форму",
                    onClick = viewModel::clearForm,
                    enabled = !uiState.loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = remember(selected) {
        when (selected) {
            is GenderDto -> when (selected) {
                GenderDto.MALE -> "Мужской"
                GenderDto.FEMALE -> "Женский"
                GenderDto.NON_BINARY -> "Иной"
            }
            is MaritalStatusDto -> when (selected) {
                MaritalStatusDto.SINGLE -> "Холост / Незамужем"
                MaritalStatusDto.MARRIED -> "Женат / Замужем"
                MaritalStatusDto.DIVORCED -> "Разведен / Разведена"
                MaritalStatusDto.WIDOWED -> "Вдовец / Вдова"
            }
            is EmploymentStatusDto -> when (selected) {
                EmploymentStatusDto.EMPLOYED -> "Работает"
                EmploymentStatusDto.UNEMPLOYED -> "Безработный"
                EmploymentStatusDto.SELF_EMPLOYED -> "Самозанятый"
                EmploymentStatusDto.RETIRED -> "Пенсионер"
                EmploymentStatusDto.BUSINESS_OWNER -> "Владелец бизнеса"
                EmploymentStatusDto.STUDENT -> "Студент"
            }
            is PositionDto -> when (selected) {
                PositionDto.MID_MANAGER -> "Менеджер среднего звена"
                PositionDto.TOP_MANAGER -> "Топ-менеджер"
                PositionDto.JUNIOR_MANAGER -> "Младший менеджер"
                PositionDto.DEVELOPER -> "Разработчик"
                PositionDto.SALES -> "Продажи"
                PositionDto.ACCOUNTANT -> "Бухгалтер"
                PositionDto.HR -> "HR-специалист"
                PositionDto.OTHER -> "Другое"
            }
            else -> selected.name
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val itemText = when (option) {
                    is GenderDto -> when (option) {
                        GenderDto.MALE -> "Мужской"
                        GenderDto.FEMALE -> "Женский"
                        GenderDto.NON_BINARY -> "Иной"
                    }
                    is MaritalStatusDto -> when (option) {
                        MaritalStatusDto.SINGLE -> "Холост / Незамужем"
                        MaritalStatusDto.MARRIED -> "Женат / Замужем"
                        MaritalStatusDto.DIVORCED -> "Разведен / Разведена"
                        MaritalStatusDto.WIDOWED -> "Вдовец / Вдова"
                    }
                    is EmploymentStatusDto -> when (option) {
                        EmploymentStatusDto.EMPLOYED -> "Работает"
                        EmploymentStatusDto.UNEMPLOYED -> "Безработный"
                        EmploymentStatusDto.SELF_EMPLOYED -> "Самозанятый"
                        EmploymentStatusDto.RETIRED -> "Пенсионер"
                        EmploymentStatusDto.BUSINESS_OWNER -> "Владелец бизнеса"
                        EmploymentStatusDto.STUDENT -> "Студент"
                    }
                    is PositionDto -> when (option) {
                        PositionDto.MID_MANAGER -> "Менеджер среднего звена"
                        PositionDto.TOP_MANAGER -> "Топ-менеджер"
                        PositionDto.JUNIOR_MANAGER -> "Младший менеджер"
                        PositionDto.DEVELOPER -> "Разработчик"
                        PositionDto.SALES -> "Продажи"
                        PositionDto.ACCOUNTANT -> "Бухгалтер"
                        PositionDto.HR -> "HR-специалист"
                        PositionDto.OTHER -> "Другое"
                    }
                    else -> option.name
                }
                DropdownMenuItem(
                    text = { Text(itemText) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
