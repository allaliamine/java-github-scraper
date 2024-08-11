package org.scraper;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

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



//        try{
//
//            //sign up to gitHub
//            driver.get("https://github.com/login");
//
//            WebElement usernameField = driver.findElement(By.id("login_field"));
//            usernameField.sendKeys("USERNAME");
//
//            WebElement passwordField = driver.findElement(By.id("password"));
//            passwordField.sendKeys("PASSWORD");
//
//            WebElement loginButton = driver.findElement(By.name("commit"));
//            loginButton.click();
//
//            Thread.sleep(19000);
//
//            //load java code pages
//            driver.get("https://gist.github.com/search?l=Java&q=java");
//
//            List<String> links = new ArrayList<>();
//
//            //variable that store if there is a next page
//            boolean nextPage = true;
//
//            while(nextPage){
//
//                getUrls(driver, links);
//                //scroll down
//                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
//
//                //get the next page button
//                List<WebElement> nextButtons = driver.findElements(By.cssSelector("a[rel='next']"));
//
//                if (nextButtons.size() > 0){
//
//                    WebElement nextButton = nextButtons.getFirst();
//                    String isAriaDisabled = nextButton.getAttribute("aria-disabled");
//
//                    if (nextButton.isDisplayed() && ( isAriaDisabled == null || !isAriaDisabled.equals("true") ) ){
//                        //click the button if it's not disabled
//                        nextButton.click();
//                        //wait to the next page to load
//                        Thread.sleep(5000);
//                    }else {
//                        // update the nextPage variable if it's disabled
//                        nextPage = false;
//                    }
//                }else {
//                    nextPage = false;
//                }
//            }
//
//            for(String url: links){
//                extractCode(driver,url);
//            }
//
//        }catch(RuntimeException e){
//            System.out.println("erreur"+e.getMessage());
//        }catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        extractCode(driver,"https://gist.github.com/fauzie811/b8a9678e0c522b391d5c7fd806c7bb16");


    }



    //function that extract urls of java code page
    public static void getUrls(WebDriver driver,List<String> links ){

        List<WebElement> codePages = driver.findElements(By.className("Link--muted"));

        for(WebElement code: codePages ){
            String fileUrl = code.getAttribute("href");
            if (!fileUrl.contains("forks") && !fileUrl.contains("comments") && !fileUrl.contains("stargazers")) {
                links.add(fileUrl);
                System.out.println(fileUrl);
            }

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
                        processCode(codeText);
                    }
                }
            }

        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /*
        methode to write data into csv file
     */
    public static void writeData(String filePath, String [] data){
        File file = new File(filePath);
        try {

            FileWriter dataFile = new FileWriter(file,true);

            CSVWriter writer = new CSVWriter(dataFile);

            writer.writeNext(data);

            writer.close();

        }catch(IOException e) {
            System.out.println("probleme lors de l'ecriture : "+e.getMessage());
        }
    }


    /*
        methode to extract methods and commetns from java code
     */
    private static void processCode(String code) {

        String multipleLigne = "(?s)/\\*.*?\\*/";
        String singleLine = "//[^\\r\\n]*";
        String combainedComments = multipleLigne + "|" + singleLine + "\\s* (private|public|protected)";


        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow(() -> new RuntimeException("Failed to parse code"));

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            String methodName = method.getNameAsString();
            String methodDeclaration = method.toString(); // Get the complete method declaration and body
            String cleanedMethode = cleanCode(methodDeclaration);


            Pattern commentPattern = Pattern.compile(combainedComments);
            Matcher commentMatcher = commentPattern.matcher(methodDeclaration);

            StringBuilder commentaire = new StringBuilder();


            while (commentMatcher.find()) {
                commentaire.append(commentMatcher.group()).append(" | ");
            }

            System.out.println("Method: " + methodName);
            System.out.println("body: " + cleanedMethode);
            System.out.println("***********************");
            System.out.println("Comment: " + commentaire);
            System.out.println("***********************");

        });

    }


    private static String cleanCode(String code){
        String multipleLigne = "(?s)/\\*.*?\\*/";
        String singleLine = "//[^\\r\\n]*";
        String combainedComments = multipleLigne + "|" + singleLine;

        return code.replaceAll(combainedComments,"");

    }

}