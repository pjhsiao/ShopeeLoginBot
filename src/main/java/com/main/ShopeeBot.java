package com.main;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

/**
 * The bot be used for login the e-comm of Shopee
 */
public class ShopeeBot {

    private static final Logger logger =  LoggerFactory.getLogger(ShopeeBot.class);

    //fork-join thread pool　
    private static final ExecutorService ApForkJoinPool = ForkJoinPool.commonPool();

    //The Callable be used to get value from console
    private static final Callable<String> ConsoleValueCallable = new Callable<String>(){
        @Override
        public String call() throws Exception {
            logger.info("Please input value at here:");
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine();
        }
    };

    // Set ChromeDriver Path
    static {
        String chromeDriverPath = "";
        // To copy chromedriver.exe from Jar file to local temp folder
        try (InputStream inputStream = ShopeeBot.class.getResourceAsStream("/chromedriver.exe")){
            File tempFile = File.createTempFile("chromedriver", ".exe");
            tempFile.deleteOnExit();
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            chromeDriverPath = tempFile.getAbsolutePath();
            outputStream.flush();
            outputStream.close();

        }catch (Exception e){
            logger.error("occurred an error : {}", e.getMessage(), e);
        }

        //set absolute path to environment
        if (!chromeDriverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            logger.info("driverFullPath {}", System.getProperty("webdriver.chrome.driver"));
        }
    }

    public static void main(String[] args) {
        logger.info("======Bot process start======");
        //Create ChromeDriver new instance
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--remote-allow-origins=*");
        final WebDriver driver = new ChromeDriver(chromeOptions);
        driver.manage().window().maximize();

        try{
            // go to login page of shopee
            driver.get("https://shopee.tw/buyer/login");

            logger.info("====== login to go ======");
            login(driver);
            logger.info("====== login succeeded======");
        }catch (Exception e){
            logger.error("occurred an error : {}", e.getMessage(), e);
        }finally {
            //close thread-pool
            ApForkJoinPool.shutdown();
            logger.info("======Bot process end======");
        }
    }

    private static void login(WebDriver driver) throws InterruptedException, ExecutionException, TimeoutException {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        //to get username and pwd buttons
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("loginKey")));
        WebElement passwordInput = driver.findElement(By.name("password"));

        /**
         * To use fork-join mode to create another thread to get what user has inputted username and pwd on terminal.
         */

        logger.info("====== what username is, please input ? ======");
        Future<String> future = ApForkJoinPool.submit(ConsoleValueCallable);
        String username = future.get(300, TimeUnit.SECONDS);

        logger.info("====== what password is, please input ? ======");
        future = ApForkJoinPool.submit(ConsoleValueCallable);
        String password = future.get(300, TimeUnit.SECONDS);

        //send value of username and pwd
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);

        //to get login button
        Optional<WebElement> webElementOptional = Optional.empty();
        for(WebElement element:driver.findElements(By.tagName("button"))){
            if(element.getText().contains("登入")){
                webElementOptional =  Optional.of(element);
            }
        }
        WebElement loginButton = webElementOptional.get();
        //to wait for login button clickable when username and pwd has be inputted
        Thread.currentThread().sleep(300l);//wait disabled of button be canceled when username and pwd has be inputted
        loginButton.click();
    }

}
