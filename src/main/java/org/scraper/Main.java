package org.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "/Users/mac/Downloads/chromedriver-mac-x64/chromedriver");

        WebDriver driver = new ChromeDriver();

        try{

            driver.get("https://github.com/login");

            WebElement usernameField = driver.findElement(By.id("login_field"));
            usernameField.sendKeys("USERNAME");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("PASSWORD");

            WebElement loginButton = driver.findElement(By.name("commit"));
            loginButton.click();

            Thread.sleep(15000);

            driver.get("https://github.com/search?q=language%3AJava&type=code");

            List<WebElement> codePages = new ArrayList<>();
            List<String> links = new ArrayList<>();
            codePages = driver.findElements(By.cssSelector("td.blob-num a"));


            for(WebElement code: codePages ){
                String fileUrl = code.getAttribute("href");
                String regex = "https://github\\.com/[^#]+#L1";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(fileUrl);
                if (matcher.matches()) {
                    links.add(fileUrl);
                    System.out.println(fileUrl);
                }

            }

        }catch(RuntimeException e){
            System.out.println("erreur"+e.getMessage());
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        finally {
//            driver.quit();
//        }

    }
}