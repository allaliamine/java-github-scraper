package org.scraper;

import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            driver.get("https://gist.github.com/search?l=Java&q=java");

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

//        extractCode(driver,"https://gist.github.com/hectormethod/c5e4cd6e507acd905269465df6d5f543");


    }



    //function that extract urls of java code page
    public static void getUrls(WebDriver driver,List<String> links ){

        List<WebElement> codePages = driver.findElements(By.className("Link--muted"));

        for(WebElement code: codePages ){
            String fileUrl = code.getAttribute("href");

            links.add(fileUrl);
            System.out.println(fileUrl);

        }
    }


    public static void extractCode(WebDriver driver, String url) {
        try {
            driver.get(url);
            Thread.sleep(5000);

            List<String> rawLinks = new ArrayList<>();

            List<WebElement> rawButtons = driver.findElements(By.cssSelector("div.file-actions a.Button--secondary.Button--small.Button"));

            // Iterate through each raw button
            for (WebElement rawButton : rawButtons) {

                String href = rawButton.getAttribute("href");
                rawLinks.add(href);
            }

            for(String rawLink: rawLinks){

                driver.get(rawLink);
                Thread.sleep(5000);


                // Extract the code from the <pre> tag
                List<WebElement> preTags = driver.findElements(By.tagName("pre"));

                if (preTags.isEmpty()) {
                    System.out.println("No <pre> tag found on the page: " + rawLink);
                } else {
                    WebElement preTag = preTags.getFirst();
                    String codeText = preTag.getText();

                    if (codeText.isEmpty()) {
                        System.out.println("No code found in the <pre> tag at URL: " + rawLink);
                    } else {
                        extractComments(codeText);
                    }
                }
            }

        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
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
        String multipleLigne = "(?s)/\\*.*?\\*/";

        String singleLine = "//[^\\r\\n]*";


        String methodsPattern = "(public|private|protected)?\\s*(static)?\\s*(final)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(throws\\s+[^\\s]*)?\\s*\\{(?:[^{}]*|\\{(?:[^{}]*|\\{[^{}]*\\})*\\})*\\}";

        String combinedPattern = "(?s)(" + multipleLigne + "|" + singleLine + ")\\s*" + methodsPattern;

        Pattern pattern = Pattern.compile(combinedPattern);
        Matcher matcher = pattern.matcher(code);

        List<String> extractedCode = new ArrayList<>();

        while (matcher.find()) {
            // Extract the code with comment
            String foundCode = matcher.group().trim();
            // Add to list of comments
            extractedCode.add(foundCode);
        }

        for (String foundCode : extractedCode) {
            // Extract comment from the found code
            Pattern commentPattern = Pattern.compile(multipleLigne + "|" + singleLine + "\\s* (private|public|protected)");
            Matcher commentMatcher = commentPattern.matcher(foundCode);

//            System.out.println("Comment(s):");
//            System.out.println("******************************");

            StringBuilder commentaire = new StringBuilder();
            while (commentMatcher.find()) {
//                System.out.println(commentMatcher.group());
                commentaire.append(commentMatcher.group()).append(" | ");
            }

//            System.out.println("******************************");
//            System.out.println(commentaire);

            // Extract method from the found code
            Pattern methodPattern = Pattern.compile(methodsPattern);
            Matcher methodMatcher = methodPattern.matcher(foundCode);

//            System.out.println("Method(s):");
//            System.out.println("******************************");

            StringBuilder methode = new StringBuilder();
            while (methodMatcher.find()) {
                System.out.println(methodMatcher.group());
                methode.append(methodMatcher.group());
            }
//            System.out.println("******************************");

            writeData("/Users/mac/Downloads/data.csv", new String[]{String.valueOf(commentaire), String.valueOf(methode)});
        }
    }


    public static void writeData(String filePath, String [] data){
        File file = new File(filePath);
        try {
            // create FileWriter object with file as parameter
            FileWriter dataFile = new FileWriter(file,true);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(dataFile);
            // add data to csv
            writer.writeNext(data);

            writer.close();

        }catch(IOException e) {
            System.out.println("probleme lors de l'ecriture : "+e.getMessage());
        }
    }


}