package org.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "/Users/mac/Downloads/chromedriver-mac-x64/chromedriver");

        WebDriver driver = new ChromeDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try{

            //sign up to gitHub
            driver.get("https://github.com/login");

            WebElement usernameField = driver.findElement(By.id("login_field"));
            usernameField.sendKeys("USERNAME");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("PASSWORD");

            WebElement loginButton = driver.findElement(By.name("commit"));
            loginButton.click();

            Thread.sleep(19000);

            //load java code pages
            driver.get("https://github.com/search?q=language%3AJava&type=code");

            List<String> links = new ArrayList<>();

            //variable that store if there is a next page
            boolean nextPage = true;

            while(nextPage){

                getUrls(driver, links);
                //scroll down
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                //get the next page button
                List<WebElement> nextButtons = driver.findElements(By.cssSelector("a[rel='next']"));

                if (nextButtons.size() > 0){

                    WebElement nextButton = nextButtons.getFirst();
                    String isAriaDisabled = nextButton.getAttribute("aria-disabled");

                    if (nextButton.isDisplayed() && ( isAriaDisabled == null || !isAriaDisabled.equals("true") ) ){
                        //click the button if it's not disabled
                        nextButton.click();
                        //wait to the next page to load
                        Thread.sleep(10000);
                    }else {
                        // update the nextPage variable if it's disabled
                        nextPage = false;
                    }
                }else {
                    nextPage = false;
                }
            }

            for(String url: links){
                extractCode(driver,url);
            }




        }catch(RuntimeException e){
            System.out.println("erreur"+e.getMessage());
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }



    //function that extract urls of java code page
    public static void getUrls(WebDriver driver,List<String> links ){

        List<WebElement> codePages = driver.findElements(By.cssSelector("td.blob-num a"));

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

    }


    public static void extractCode(WebDriver driver, String url) {
        try {
            driver.get(url);
            Thread.sleep(5000);

            WebElement rawButton = driver.findElement(By.cssSelector("a[data-testid='raw-button']"));
            rawButton.click();
            Thread.sleep(5000);

            String rawUrl = driver.getCurrentUrl();
            driver.get(rawUrl);

            // Extract the code from the <pre> tag
            List<WebElement> preTags = driver.findElements(By.tagName("pre"));

            if (preTags.isEmpty()) {
                System.out.println("No <pre> tag found on the page: " + rawUrl);
            } else {

                WebElement preTag = preTags.getFirst();
                String codeText = preTag.getText();

                if (codeText.isEmpty()) {
                    System.out.println("No code found in the <pre> tag at URL: " + rawUrl);
                } else {
                    System.out.println("Extracted Code:");
                    System.out.println(codeText);
                }
            }

        }catch (InterruptedException e) {
            System.out.println("erreur"+e.getMessage());
        }
    }


    //function to extract comments from code
    /* methodsPattern explanation:
        \\s* : zero or more spacing characters
        \\s+ : one or more spacing characters
        (public|private|protected)? : access to the method, "?" character makes it optional
        (static)? : matches the static key word (Optional)
        first \\w+ : any word character (alphanumeric characters and underscores) (return type)
        second \\w+ : the method/function name
        ([^)]*\) : opening and closing parenthesis
        (throws\s+[^\\s]*) : the throws key word followed by [^\\s]* (exception type)
    */
    public static void extractComments(String code){
        String singleLigne = "//.*";
        String multipleLigne = "/\\*.*?\\*/";
        String javaDocComments = "/\\*\\*.*?\\*/";

        String commentsPattern = singleLigne +"|"+multipleLigne+"|"+javaDocComments;

        String methodsPattern = "(public|private|protected)?\\s+(static)?\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(throws\\s+[^\\s]*)?\\s*\\{";

    }






}