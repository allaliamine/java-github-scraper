package org.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.scraper.githubGists.processCode;

public class githubRepo {

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/Users/mac/Downloads/chromedriver-mac-x64/chromedriver");

        WebDriver driver = new ChromeDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try{

//            sign up to gitHub
            driver.get("https://github.com/login");

            WebElement usernameField = driver.findElement(By.id("login_field"));
            usernameField.sendKeys("USERNAME");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("PASSWORD");

            WebElement loginButton = driver.findElement(By.name("commit"));
            loginButton.click();

            Thread.sleep(10000);
//

            //load repositories page
//            driver.get("https://github.com/search?q=language%3AJava+stars%3A%3E500&type=repositories&p=42");

            List<String> links = new ArrayList<>();
            List<String> javaCodeUrls = new ArrayList<>();

            //navigate through all pages
//            goToNextPage(driver, js, ()->getRepoUrls(driver, links) );


            //click on java in each repo that are in links
//            for(String url: links){
//                selectJava(driver,js,url,javaCodeUrls);
//            }

            //extract urls to java files from each page of java code that have been extracted from "links"
//            for (String url : javaCodeUrls){
//                getRawFile(driver,url);
//            }


            readFile(driver);



        }catch(RuntimeException | InterruptedException e) {
            System.out.println("erreur" + e.getMessage());
        }
    }


    /*
        this method gets the links of repositories and stores them in a List
     */
    public static void getRepoUrls(WebDriver driver,List<String> links){

        List<WebElement> Repositories = driver.findElements(By.cssSelector("div.Box-sc-g0xbh4-0.bBwPjs.search-title > a"));

        for(WebElement repo: Repositories ){
            String RepoUrl = repo.getAttribute("href");
            links.add(RepoUrl);
            System.out.println(RepoUrl);
        }

    }

    /*
        method to go to next page and calls the getRepoUrls to store the urls of repositories of the
        actual page
     */
    public static void goToNextPage(WebDriver driver, JavascriptExecutor js, Runnable action) {

        //variable that store if there is a next page
        boolean nextPage = true;

        while(nextPage){

            action.run();

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
//                    Thread.sleep(5000);
                }else {
                    // update the nextPage variable if it's disabled
                    nextPage = false;
                }
            }else {
                nextPage = false;
            }
        }
    }


    /*
        this method finds a link with the text "Java",
        retrieves the URL from that link and then navigates to the Java URL
     */
    public static void selectJava(WebDriver driver, JavascriptExecutor js, String url, List<String> javaCodeUrls){
        try {
            driver.get(url);

            WebElement javaLink = driver.findElement(By.xpath("//a[span[contains(text(),'Java')]]"));

            String linkOfJava =javaLink.getAttribute("href");
//            System.out.println(linkOfJava);
            driver.get(linkOfJava);
            System.out.println("----------------------------------");
            goToNextPage(driver, js, ()->getUrlsFromJavaPage(driver,linkOfJava,javaCodeUrls));
            System.out.println("----------------------------------");


        }catch (Exception e){
            System.out.println("error"+ e.getMessage());
        }
    }


    /*
        this method is called in selectJava to get the links to java files and then stores them in a list
        to extract java comments and code after
     */
    public static void getUrlsFromJavaPage(WebDriver driver, String url ,List<String> javaCodeUrls){
        try {
            Thread.sleep(5000);
            FileWriter writer = new FileWriter("./extractedJavaFilesTrack.txt", true);

            List<WebElement> codePages = driver.findElements(By.cssSelector("td.blob-num a"));

            for (WebElement code : codePages) {

                String fileUrl = code.getAttribute("href");
                String regex = "https://github\\.com/[^#]+#L1";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(fileUrl);
                if (matcher.matches()) {

                    javaCodeUrls.add(fileUrl);
                    writer.write("\n" + fileUrl + "\n");
                    System.out.println(fileUrl);

                }

            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    /*
        this method takes the url of the java file then press on raw button, then it extracts the methods
        and comments using processCode method
     */
    public static void getRawFile(WebDriver driver, String rawUrl){

        try {

            try {
                driver.get(rawUrl);
                Thread.sleep(5000);
            } catch (InterruptedException | Error e) {
                System.out.println("Error loading URL: " + rawUrl + ". Skipping to next URL.");
                return;
            }

            try {
                //get the raw button
                WebElement rawButton = driver.findElement(By.cssSelector("a[data-testid='raw-button']"));

                // click on raw button
                String rawLink = rawButton.getAttribute("href");
                driver.get(rawLink);

                Thread.sleep(5000);

                // Extract the code from the <pre> tag
                List<WebElement> preTags = driver.findElements(By.tagName("pre"));

                if (preTags.isEmpty()) {
                        System.out.println("No <pre> tag found on the page: " + rawLink);
                    }else {
                        WebElement preTag = preTags.getFirst();
                        String codeText = preTag.getText();
                        if (codeText.isEmpty()) {
                            System.out.println("No code found in the <pre> tag at URL: " + rawLink);
                        }else {
                            try {
                                processCode(codeText);
                            } catch (Exception e) {
                                System.out.println("Error processing code at URL: " + rawLink);
                            }
                        }
                    }
            } catch (NoSuchElementException e) {
                System.out.println("Raw button not found at URL: " + rawUrl);
            }
        }catch (InterruptedException ex) {
                System.out.println("error "+ ex.getMessage());
        }

    }



    public static void readFile(WebDriver driver) {
        String filePath = "./extractedJavaFilesTrack.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {

                String lineUrl = line.trim();

                if (!lineUrl.isEmpty()) {
                    getRawFile(driver,lineUrl);
                    System.out.println(lineUrl);
                }
            }
        } catch (IOException e) {
            System.out.println("error"+ e.getMessage());
        }
    }

}