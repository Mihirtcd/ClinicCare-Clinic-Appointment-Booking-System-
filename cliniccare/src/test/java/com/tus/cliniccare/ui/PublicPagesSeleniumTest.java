package com.tus.cliniccare.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:ui_selenium_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "app.jwt.secret=Y2xpbmljY2FyZS1hY2FkZW1pYy1qd3Qtc2VjcmV0LWtleS1mb3ItcHJvamVjdC1kZW1vLXNpZ25pbmctMzJieXRlcw=="
        }
)
class PublicPagesSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;

    @Test
    void homepage_shouldRenderNavbarAndCallToAction() {
        setUpDriver();

        driver.get(baseUrl("/"));

        assertEquals("ClinicCare - Home", driver.getTitle());
        assertTrue(driver.findElement(By.id("landingNav")).isDisplayed());
        assertTrue(driver.findElement(By.cssSelector("a[href='login.html']")).isDisplayed());
        assertTrue(driver.findElement(By.cssSelector("a[href='register.html']")).isDisplayed());
    }

    @Test
    void loginPage_shouldRenderLoginFormFields() {
        setUpDriver();

        driver.get(baseUrl("/login.html"));

        assertEquals("ClinicCare - Login", driver.getTitle());
        assertTrue(driver.findElement(By.id("loginForm")).isDisplayed());
        assertTrue(driver.findElement(By.id("email")).isDisplayed());
        assertTrue(driver.findElement(By.id("password")).isDisplayed());
    }

    @Test
    void registerPage_shouldRenderRegistrationFormFields() {
        setUpDriver();

        driver.get(baseUrl("/register.html"));

        assertEquals("ClinicCare - Register", driver.getTitle());
        assertTrue(driver.findElement(By.id("registerForm")).isDisplayed());
        assertTrue(driver.findElement(By.id("firstName")).isDisplayed());
        assertTrue(driver.findElement(By.id("lastName")).isDisplayed());
        assertTrue(driver.findElement(By.id("email")).isDisplayed());
        assertTrue(driver.findElement(By.id("password")).isDisplayed());
        assertTrue(driver.findElement(By.id("confirmPassword")).isDisplayed());

        WebElement homeButton = driver.findElement(By.cssSelector("a[href='index.html']"));
        assertTrue(homeButton.isDisplayed());
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1400,1000");
        this.driver = new ChromeDriver(options);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
