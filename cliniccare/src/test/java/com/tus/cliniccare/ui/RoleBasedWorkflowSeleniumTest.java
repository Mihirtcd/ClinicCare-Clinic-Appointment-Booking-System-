package com.tus.cliniccare.ui;

import com.tus.cliniccare.entity.Appointment;
import com.tus.cliniccare.entity.Doctor;
import com.tus.cliniccare.entity.ServiceEntity;
import com.tus.cliniccare.entity.TimeSlot;
import com.tus.cliniccare.entity.User;
import com.tus.cliniccare.entity.enums.AppointmentStatus;
import com.tus.cliniccare.entity.enums.Role;
import com.tus.cliniccare.entity.enums.TimeSlotStatus;
import com.tus.cliniccare.repository.AppointmentRepository;
import com.tus.cliniccare.repository.DoctorRepository;
import com.tus.cliniccare.repository.DoctorServiceRepository;
import com.tus.cliniccare.repository.ServiceRepository;
import com.tus.cliniccare.repository.TimeSlotRepository;
import com.tus.cliniccare.repository.UserRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:role_workflow_ui_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class RoleBasedWorkflowSeleniumTest {

    private static final String DEFAULT_PASSWORD = "Password@123";
    private static final DateTimeFormatter DATE_TIME_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorServiceRepository doctorServiceRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WebDriver driver;
    private WebDriverWait wait;

    private final String adminEmail = "admin.ui@cliniccare.com";
    private final String doctorEmail = "doctor.ui@cliniccare.com";
    private final String patientEmail = "patient.ui@cliniccare.com";
    private final String candidateDoctorEmail = "doctor.candidate.ui@cliniccare.com";

    private final String candidateDoctorFirstName = "Elena";
    private final String candidateDoctorLastName = "Morris";

    private Long doctorId;
    private Long candidateDoctorUserId;
    private Long pendingConfirmAppointmentId;
    private Long pendingRejectAppointmentId;
    private Long confirmedCompleteAppointmentId;
    private LocalDate bookingSlotDate;

    @BeforeEach
    void setUp() {
        setUpDriver();
        resetAndSeedDomainData();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void protectedPages_shouldRedirectToLoginWhenUserIsNotAuthenticated() {
        List<String> protectedPages = List.of(
                "/patient-dashboard.html",
                "/patient-booking.html",
                "/patient-appointments.html",
                "/doctor-dashboard.html",
                "/admin-dashboard.html",
                "/admin-users.html",
                "/admin-doctors.html",
                "/admin-services.html",
                "/admin-timeslots.html"
        );

        for (String page : protectedPages) {
            driver.get(baseUrl(page));
            wait.until(ExpectedConditions.urlContains("login.html"));
            assertTrue(driver.getCurrentUrl().contains("login.html"));
        }
    }

    @Test
    void login_asPatient_shouldRedirectToPatientDashboard() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        assertTrue(driver.getTitle().contains("Patient Dashboard"));
    }

    @Test
    void login_asDoctor_shouldRedirectToDoctorDashboard() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        assertTrue(driver.getTitle().contains("Doctor Dashboard"));
    }

    @Test
    void login_asAdmin_shouldRedirectToAdminDashboard() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        assertTrue(driver.getTitle().contains("Admin Dashboard"));
    }

    @Test
    void patientDashboard_shouldRenderMetricsAndRecentAppointments() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        driver.get(baseUrl("/patient-dashboard.html"));

        wait.until(d -> "1".equals(d.findElement(By.id("serviceCount")).getText()));
        wait.until(d -> "3".equals(d.findElement(By.id("appointmentCount")).getText()));
        wait.until(d -> "2".equals(d.findElement(By.id("pendingCount")).getText()));

        assertEquals("1", waitVisible(By.id("serviceCount")).getText());
        assertEquals("3", waitVisible(By.id("appointmentCount")).getText());
        assertEquals("2", waitVisible(By.id("pendingCount")).getText());
        waitForTableContains("appointmentsTable", "General Consultation");
    }

    @Test
    void doctorDashboard_shouldRenderMetricsAndFilterByStatus() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        driver.get(baseUrl("/doctor-dashboard.html"));

        wait.until(d -> "3".equals(d.findElement(By.id("totalCount")).getText()));
        wait.until(d -> "2".equals(d.findElement(By.id("pendingCount")).getText()));
        wait.until(d -> "1".equals(d.findElement(By.id("confirmedCount")).getText()));

        selectByValue("statusFilter", "CONFIRMED");

        wait.until(d -> "1".equals(d.findElement(By.id("totalCount")).getText()));
        wait.until(d -> "0".equals(d.findElement(By.id("pendingCount")).getText()));
        wait.until(d -> "1".equals(d.findElement(By.id("confirmedCount")).getText()));

        waitForTableContains("scheduleTable", "CONFIRMED");
        assertFalse(waitVisible(By.id("scheduleTable")).getText().contains("PENDING"));
    }

    @Test
    void adminDashboard_shouldRenderMetricsChartsAndUsersSnapshotSerialNumbers() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-dashboard.html"));

        wait.until(d -> "4".equals(d.findElement(By.id("totalUsers")).getText()));
        wait.until(d -> "1".equals(d.findElement(By.id("doctorCount")).getText()));
        wait.until(d -> "1".equals(d.findElement(By.id("serviceCount")).getText()));
        wait.until(d -> "3".equals(d.findElement(By.id("totalAppointments")).getText()));

        assertTrue(waitVisible(By.id("userRoleChart")).isDisplayed());
        assertTrue(waitVisible(By.id("appointmentStatusChart")).isDisplayed());

        wait.until(d -> !d.findElements(By.cssSelector("#usersTable tbody tr")).isEmpty());
        String firstSerial = waitVisible(By.cssSelector("#usersTable tbody tr td:first-child")).getText().trim();
        assertEquals("1", firstSerial);
    }

    @Test
    void roleMismatch_shouldRedirectPatientToPatientDashboardWhenOpeningAdminPage() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        driver.get(baseUrl("/admin-dashboard.html"));

        wait.until(ExpectedConditions.urlContains("patient-dashboard.html"));
        assertTrue(driver.getCurrentUrl().contains("patient-dashboard.html"));
    }

    @Test
    void roleMismatch_shouldRedirectDoctorToDoctorDashboardWhenOpeningAdminUsersPage() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        driver.get(baseUrl("/admin-users.html"));

        wait.until(ExpectedConditions.urlContains("doctor-dashboard.html"));
        assertTrue(driver.getCurrentUrl().contains("doctor-dashboard.html"));
    }

    @Test
    void patientBookingPage_shouldShowFriendlyServiceAndDoctorLabels() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        driver.get(baseUrl("/patient-booking.html"));

        waitForSelectOptions("serviceSelect", 2);
        String selectedServiceId = selectFirstNonEmptyOption("serviceSelect");
        assertTrue(selectedServiceId != null && !selectedServiceId.isBlank());

        wait.until(d -> d.findElement(By.id("doctorSelect")).isEnabled());
        waitForSelectOptions("doctorSelect", 2);

        Select serviceSelect = new Select(waitVisible(By.id("serviceSelect")));
        for (WebElement option : serviceSelect.getOptions()) {
            if (option.getAttribute("value") != null && !option.getAttribute("value").isBlank()) {
                assertFalse(option.getText().toUpperCase().contains("ID"));
            }
        }

        Select doctorSelect = new Select(waitVisible(By.id("doctorSelect")));
        for (WebElement option : doctorSelect.getOptions()) {
            if (option.getAttribute("value") != null && !option.getAttribute("value").isBlank()) {
                String text = option.getText();
                assertTrue(text.startsWith("Dr."));
                assertFalse(text.toUpperCase().contains("ID"));
            }
        }
    }

    @Test
    void patient_shouldBookAppointmentFromBookingPage() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        driver.get(baseUrl("/patient-booking.html"));

        waitForSelectOptions("serviceSelect", 2);
        selectFirstNonEmptyOption("serviceSelect");

        wait.until(d -> d.findElement(By.id("doctorSelect")).isEnabled());
        waitForSelectOptions("doctorSelect", 2);
        selectFirstNonEmptyOption("doctorSelect");

        setDateInput("slotDateFilter", bookingSlotDate);
        click(By.id("loadSlotsBtn"));

        By enabledBookButton = By.cssSelector("#slotsTable .book-btn:not([disabled])");
        wait.until(d -> !d.findElements(enabledBookButton).isEmpty());
        click(enabledBookButton);

        wait.until(ExpectedConditions.attributeContains(By.id("bookModal"), "class", "show"));
        type(By.id("patientNote"), "Booked from Selenium");
        click(By.cssSelector("#bookForm button[type='submit']"));

        wait.until(d -> appointmentRepository.count() >= 4);

        driver.get(baseUrl("/patient-appointments.html"));
        waitForTableContains("appointmentsTable", "Booked from Selenium");
        waitForTableContains("appointmentsTable", "PENDING");
    }

    @Test
    void patient_shouldCancelOwnPendingAppointment() {
        loginAs(patientEmail, DEFAULT_PASSWORD, "patient-dashboard.html");
        driver.get(baseUrl("/patient-appointments.html"));

        By cancelButton = By.cssSelector(".cancel-btn[data-id='" + pendingConfirmAppointmentId + "']");
        wait.until(d -> !d.findElements(cancelButton).isEmpty());
        click(cancelButton);

        wait.until(ExpectedConditions.attributeContains(By.id("cancelConfirmModal"), "class", "show"));
        click(By.id("confirmCancelBtn"));

        waitForAlertContains("Appointment cancelled.");
        waitForTableContains("appointmentsTable", "CANCELLED");
    }

    @Test
    void doctor_shouldConfirmAssignedPendingAppointment() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        driver.get(baseUrl("/doctor-dashboard.html"));

        By confirmButton = By.cssSelector(".confirm-btn[data-id='" + pendingConfirmAppointmentId + "']");
        wait.until(d -> !d.findElements(confirmButton).isEmpty());
        click(confirmButton);

        waitForAlertContains("Appointment confirmed.");
        waitForTableContains("scheduleTable", "CONFIRMED");
    }

    @Test
    void doctor_shouldRejectAssignedPendingAppointment() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        driver.get(baseUrl("/doctor-dashboard.html"));

        By rejectButton = By.cssSelector(".reject-btn[data-id='" + pendingRejectAppointmentId + "']");
        wait.until(d -> !d.findElements(rejectButton).isEmpty());
        click(rejectButton);

        wait.until(ExpectedConditions.attributeContains(By.id("rejectModal"), "class", "show"));
        type(By.id("rejectReason"), "Unavailable");
        click(By.cssSelector("#rejectForm button[type='submit']"));

        waitForAlertContains("Appointment rejected.");
        waitForTableContains("scheduleTable", "REJECTED");
    }

    @Test
    void doctor_shouldCompleteConfirmedPastAppointment() {
        loginAs(doctorEmail, DEFAULT_PASSWORD, "doctor-dashboard.html");
        driver.get(baseUrl("/doctor-dashboard.html"));

        By completeButton = By.cssSelector(".complete-btn[data-id='" + confirmedCompleteAppointmentId + "']");
        wait.until(d -> !d.findElements(completeButton).isEmpty());
        click(completeButton);

        waitForAlertContains("Appointment marked as completed.");
        waitForTableContains("scheduleTable", "COMPLETED");
    }

    @Test
    void adminDashboard_shouldRenderStatisticsAndCharts() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-dashboard.html"));

        wait.until(d -> {
            String totalUsersText = d.findElement(By.id("totalUsers")).getText();
            try {
                return Integer.parseInt(totalUsersText) >= 4;
            } catch (NumberFormatException ex) {
                return false;
            }
        });

        assertTrue(waitVisible(By.id("userRoleChart")).isDisplayed());
        assertTrue(waitVisible(By.id("appointmentStatusChart")).isDisplayed());
    }

    @Test
    void admin_shouldCreateAndDisableService() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-services.html"));

        String serviceName = "SeleniumService" + System.currentTimeMillis();
        click(By.id("createServiceBtn"));
        wait.until(ExpectedConditions.attributeContains(By.id("serviceModal"), "class", "show"));

        type(By.id("name"), serviceName);
        type(By.id("description"), "Created in Selenium UI test");
        type(By.id("durationMinutes"), "35");
        click(By.cssSelector("#serviceForm button[type='submit']"));

        waitForAlertContains("Service created successfully.");
        waitForTableContains("serviceTable", serviceName);

        By disableButton = By.xpath("//table[@id='serviceTable']//tbody//tr[td[contains(.,'" + serviceName + "')]]//button[contains(@class,'disable-btn')]");
        click(disableButton);

        waitForAlertContains("Service disabled successfully.");
        WebElement row = waitVisible(By.xpath("//table[@id='serviceTable']//tbody//tr[td[contains(.,'" + serviceName + "')]]"));
        assertTrue(row.getText().contains("DISABLED"));
    }

    @Test
    void admin_shouldCreateDoctorUser() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-users.html"));

        String doctorUserEmail = "new.doctor." + System.currentTimeMillis() + "@cliniccare.com";
        click(By.cssSelector("button[data-bs-target='#createUserModal']"));
        wait.until(ExpectedConditions.attributeContains(By.id("createUserModal"), "class", "show"));

        type(By.id("firstName"), "New");
        type(By.id("lastName"), "Doctor");
        type(By.id("email"), doctorUserEmail);
        type(By.id("phoneNumber"), "9111111111");
        type(By.id("password"), DEFAULT_PASSWORD);
        click(By.cssSelector("#createUserForm button[type='submit']"));

        waitForAlertContains("DOCTOR user created successfully.");

        selectByValue("roleFilter", "DOCTOR");
        click(By.id("filterBtn"));
        waitForTableContains("usersTable", doctorUserEmail);
    }

    @Test
    void admin_shouldCreateDoctorProfile() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-doctors.html"));

        click(By.id("openCreateDoctorBtn"));
        wait.until(ExpectedConditions.attributeContains(By.id("createDoctorModal"), "class", "show"));
        waitForSelectOptions("doctorUserSelect", 2);
        waitForSelectOptions("serviceIds", 1);

        selectOptionContainingText("doctorUserSelect", candidateDoctorEmail);
        type(By.id("specialization"), "Dermatology");

        Select services = new Select(waitVisible(By.id("serviceIds")));
        services.selectByIndex(0);

        click(By.cssSelector("#createDoctorForm button[type='submit']"));

        waitForAlertContains("Doctor profile created successfully.");
        waitForTableContains("doctorTable", candidateDoctorFirstName);
    }

    @Test
    void admin_shouldCreateTimeSlotForDoctor() {
        loginAs(adminEmail, DEFAULT_PASSWORD, "admin-dashboard.html");
        driver.get(baseUrl("/admin-timeslots.html"));

        waitForSelectOptions("doctorSelect", 2);
        selectByValue("doctorSelect", String.valueOf(doctorId));
        click(By.id("openCreateSlotBtn"));

        wait.until(ExpectedConditions.attributeContains(By.id("createSlotModal"), "class", "show"));
        selectByValue("slotDoctorSelect", String.valueOf(doctorId));

        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(30);
        setDateTimeLocalInput("startTime", start);
        setDateTimeLocalInput("endTime", end);

        click(By.cssSelector("#createSlotForm button[type='submit']"));

        waitForAlertContains("Time slot created successfully.");
        waitForTableContains("slotTable", "AVAILABLE");
    }

    private void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1440,1080");
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    private void resetAndSeedDomainData() {
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        doctorServiceRepository.deleteAll();
        doctorRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = createUser("Admin", "User", adminEmail, Role.ADMIN);
        User doctorUser = createUser("Rahul", "Shah", doctorEmail, Role.DOCTOR);
        User patient = createUser("Nina", "Patel", patientEmail, Role.PATIENT);
        User candidateDoctor = createUser(candidateDoctorFirstName, candidateDoctorLastName, candidateDoctorEmail, Role.DOCTOR);
        candidateDoctorUserId = candidateDoctor.getId();

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization("Cardiology");
        doctor = doctorRepository.save(doctor);
        doctorId = doctor.getId();

        ServiceEntity service = new ServiceEntity();
        service.setName("General Consultation");
        service.setDescription("General clinic consultation");
        service.setDurationMinutes(30);
        service.setIsEnabled(true);
        service = serviceRepository.save(service);

        com.tus.cliniccare.entity.DoctorService doctorService = new com.tus.cliniccare.entity.DoctorService();
        doctorService.setDoctor(doctor);
        doctorService.setService(service);
        doctorServiceRepository.save(doctorService);

        LocalDateTime base = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);

        TimeSlot bookingSlot = createSlot(doctor, base.plusHours(1), base.plusHours(1).plusMinutes(30), TimeSlotStatus.AVAILABLE);
        bookingSlotDate = bookingSlot.getStartTime().toLocalDate();

        TimeSlot confirmSlot = createSlot(doctor, base.plusHours(2), base.plusHours(2).plusMinutes(30), TimeSlotStatus.RESERVED);
        TimeSlot rejectSlot = createSlot(doctor, base.plusHours(3), base.plusHours(3).plusMinutes(30), TimeSlotStatus.RESERVED);
        TimeSlot completeSlot = createSlot(doctor, base.plusHours(4), base.plusHours(4).plusMinutes(30), TimeSlotStatus.RESERVED);

        pendingConfirmAppointmentId = createAppointment(
                patient, doctor, service, confirmSlot, AppointmentStatus.PENDING, "Please confirm"
        ).getId();

        pendingRejectAppointmentId = createAppointment(
                patient, doctor, service, rejectSlot, AppointmentStatus.PENDING, "Please reject"
        ).getId();

        confirmedCompleteAppointmentId = createAppointment(
                patient, doctor, service, completeSlot, AppointmentStatus.CONFIRMED, "Past confirmed appointment"
        ).getId();

        LocalDateTime pastStart = LocalDateTime.now().minusHours(2).withSecond(0).withNano(0);
        LocalDateTime pastEnd = LocalDateTime.now().minusHours(1).withSecond(0).withNano(0);
        jdbcTemplate.update(
                "update time_slots set start_time = ?, end_time = ? where id = ?",
                Timestamp.valueOf(pastStart),
                Timestamp.valueOf(pastEnd),
                completeSlot.getId()
        );
    }

    private User createUser(String firstName, String lastName, String email, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber("9000000000");
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    private TimeSlot createSlot(Doctor doctor, LocalDateTime start, LocalDateTime end, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setStatus(status);
        return timeSlotRepository.save(slot);
    }

    private Appointment createAppointment(
            User patient,
            Doctor doctor,
            ServiceEntity service,
            TimeSlot slot,
            AppointmentStatus status,
            String note
    ) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setTimeSlot(slot);
        appointment.setStatus(status);
        appointment.setPatientNote(note);
        return appointmentRepository.save(appointment);
    }

    private void loginAs(String email, String password, String expectedPageFragment) {
        driver.get(baseUrl("/login.html"));
        waitVisible(By.id("loginForm"));

        type(By.id("email"), email);
        type(By.id("password"), password);
        click(By.cssSelector("#loginForm button[type='submit']"));

        wait.until(ExpectedConditions.urlContains(expectedPageFragment));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private WebElement waitVisible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void click(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    private void type(By by, String text) {
        WebElement element = waitVisible(by);
        element.clear();
        element.sendKeys(text);
    }

    private void waitForSelectOptions(String selectId, int minimumOptions) {
        wait.until(d -> {
            Select select = new Select(d.findElement(By.id(selectId)));
            return select.getOptions().size() >= minimumOptions;
        });
    }

    private String selectFirstNonEmptyOption(String selectId) {
        Select select = new Select(waitVisible(By.id(selectId)));
        for (WebElement option : select.getOptions()) {
            String value = option.getAttribute("value");
            if (value != null && !value.isBlank()) {
                select.selectByValue(value);
                return value;
            }
        }
        return "";
    }

    private void selectByValue(String selectId, String value) {
        Select select = new Select(waitVisible(By.id(selectId)));
        select.selectByValue(value);
    }

    private void selectOptionContainingText(String selectId, String expectedText) {
        Select select = new Select(waitVisible(By.id(selectId)));
        for (WebElement option : select.getOptions()) {
            if (option.getText().contains(expectedText)) {
                select.selectByValue(option.getAttribute("value"));
                return;
            }
        }
        throw new IllegalStateException("Option containing text not found: " + expectedText);
    }

    private void setDateInput(String inputId, LocalDate date) {
        WebElement element = waitVisible(By.id(inputId));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                element,
                date.toString()
        );
    }

    private void setDateTimeLocalInput(String inputId, LocalDateTime dateTime) {
        WebElement element = waitVisible(By.id(inputId));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                element,
                DATE_TIME_LOCAL.format(dateTime)
        );
    }

    private void waitForAlertContains(String text) {
        wait.until(d -> d.findElement(By.id("alertBox")).getText().contains(text));
    }

    private void waitForTableContains(String tableId, String text) {
        wait.until(d -> d.findElement(By.id(tableId)).getText().contains(text));
    }
}
