package org.scraper;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.opencsv.CSVWriter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class githubRepo {

    private static final String API_KEY = "YOUR_API_KEY";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/path/tp/your-chrome-driver/chromedriver-mac-x64/chromedriver");

        WebDriver driver = new ChromeDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try{

            driver.get("https://github.com/login");

            WebElement usernameField = driver.findElement(By.id("login_field"));
            usernameField.sendKeys("USERNAME");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("PASSWORD");

            WebElement loginButton = driver.findElement(By.name("commit"));
            loginButton.click();

            Thread.sleep(1000);

            //load repositories that have more than 500 stars and written in java
            driver.get("https://github.com/search?q=language%3AJava+stars%3A%3E500&type=repositories");

            //navigate through all Repository pages and store their urls in a file
            goToNextPage(driver, js, ()->getRepoUrls(driver) );

            /*
                click on java in each repo that are in the file that have the repos URLS
                and extract the urls of the files that contains java code
            */
            readFile(url -> selectJava(driver,js,url),"./extractedRepoUrls.txt");


            readFile(url -> getRawFile(driver,url),"./extractedJavaFiles.txt");


        }catch(RuntimeException | InterruptedException e) {
            System.out.println("error " + e.getMessage());
        }
    }


    /*
        this method gets the links of repositories and stores them in a List
     */
    public static void getRepoUrls(WebDriver driver){

        List<WebElement> Repositories = driver.findElements(By.cssSelector("div.Box-sc-g0xbh4-0.bBwPjs.search-title > a"));

        for(WebElement repo: Repositories ){

            String RepoUrl = repo.getAttribute("href");

            try(FileWriter writer = new FileWriter("./extractedRepoUrls.txt", true)){

                writer.write(RepoUrl + "\n");

            } catch (IOException e) {
                System.out.printf("error in getRepoUrls method : "+e.getMessage());
            }
        }
    }

    /*
        method to go to next page and calls the getRepoUrls to store the urls of repositories of the
        actual page
     */
    public static void goToNextPage(WebDriver driver, JavascriptExecutor js, Runnable action) throws InterruptedException {

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
                    Thread.sleep(5000);
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
        this method finds an <a> tag that contains a <span> tag with the text "Java",
        retrieves the URL from that tag and then navigates to the extracted URL
        then call the goToNextPage() with getUrlsFromJavaPage() method as an argument
     */
    public static void selectJava(WebDriver driver, JavascriptExecutor js, String url){
        try {
            driver.get(url);

            WebElement javaLink = driver.findElement(By.xpath("//a[span[contains(text(),'Java')]]"));

            String linkOfJava =javaLink.getAttribute("href");
            driver.get(linkOfJava);
            goToNextPage(driver, js, ()->getUrlsFromJavaPage(driver));

        }catch (Exception e){
            System.out.println("error in selectJava method : "+ e.getMessage());
        }
    }


    /*
        this method is called in selectJava to get the links to java files and then stores them in a file
        named: extractedJavaFiles.txt to extract java comments and code after from each link in the file
     */
    public static void getUrlsFromJavaPage(WebDriver driver){
        try (FileWriter writer = new FileWriter("./extractedJavaFiles.txt", true)){
            Thread.sleep(5000);

            List<WebElement> codePages = driver.findElements(By.cssSelector("td.blob-num a"));

            for (WebElement code : codePages) {

                String fileUrl = code.getAttribute("href");
                String regex = "https://github\\.com/[^#]+#L1";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(fileUrl);
                if (matcher.matches()) {
                    writer.write("\n"+ fileUrl + "\n");
                }

            }
        }catch (Exception e){
            System.out.println("error in getUrlsFromJavaPage method : " + e.getMessage());
        }
    }


    /*
        this method takes the url of the java file then press on raw button, then it extracts the methods
        and comments using processCode method
     */
    public static void getRawFile(WebDriver driver, String JavaFileUrl){

        try {

            try {
                driver.get(JavaFileUrl);
                Thread.sleep(5000);
            } catch (InterruptedException | Error e) {
                System.out.println("Error loading URL: " + JavaFileUrl + ". Skipping to next URL.");
                return;
            }

            try {
                //get the raw button
                WebElement rawButton = driver.findElement(By.cssSelector("a[data-testid='raw-button']"));

                // click on raw button
                String rawURL = rawButton.getAttribute("href");
                driver.get(rawURL);

                Thread.sleep(5000);

                // Extract the code from the <pre> tag
                List<WebElement> preTags = driver.findElements(By.tagName("pre"));

                if (preTags.isEmpty()) {
                        System.out.println("No <pre> tag found on the page: " + rawURL);
                    }else {
                        WebElement preTag = preTags.getFirst();
                        String codeText = preTag.getText();
                        if (codeText.isEmpty()) {
                            System.out.println("No code found in the <pre> tag at URL: " + rawURL);
                        }else {
                            try {
                                processCode(codeText);
                            } catch (Exception e) {
                                System.out.println("Error processing code at URL: " + rawURL);
                            }
                        }
                    }
            } catch (NoSuchElementException e) {
                System.out.println("Raw button not found at URL: " + JavaFileUrl);
            }
        }catch (InterruptedException ex) {
                System.out.println("error "+ ex.getMessage());
        }

    }


    /*
        method that read lines from a given file
        it is used to read
        -> file that have urls of repositories
        -> file that have urls of java files
     */
    public static void readFile(Consumer<String> action, String filePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {

                String lineUrl = line.trim();

                if (!lineUrl.isEmpty()) {
                    action.accept(lineUrl);
                }
            }
        } catch (IOException e) {
            System.out.println("error in reading file : "+ e.getMessage());
        }
    }


    /*
        method to extract methods and comments from java code
     */
    public static void processCode(String code) {

        String multipleLigne = "(?s)/\\*.*?\\*/";
        String singleLine = "//[^\\r\\n]*";
        String combainedComments = multipleLigne + "|" + singleLine + "\\s* (private|public|protected)";


        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow(() -> new RuntimeException("Failed to parse code"));

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            String methodName = method.getNameAsString();
            String methodDeclaration = method.toString(); // Get the complete method declaration and body including the comments
            String cleanedMethode = cleanCode(methodDeclaration); // get rid of the comments from the method


            Pattern commentPattern = Pattern.compile(combainedComments);
            Matcher commentMatcher = commentPattern.matcher(methodDeclaration);

            StringBuilder commentaire = new StringBuilder();

            while (commentMatcher.find()) {
                commentaire.append(commentMatcher.group()).append(" | "); // the "|" symbole is used as separator in case that a method have multiple comments
            }

            if(!commentaire.isEmpty()) {
                try {
                    commentaire = new StringBuilder(TranslteToEnglish(String.valueOf(commentaire)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Method: " + methodName);
                System.out.println("body: " + cleanedMethode);
                System.out.println("***********************");
                System.out.println("Comment: " + commentaire);
                System.out.println("***********************");

                writeData("/path/to/file/data.csv", new String[]{String.valueOf(commentaire), cleanedMethode});
            }

        });
    }


    /*
        method to clean the code from comments
     */
    private static String cleanCode(String code){
        String multipleLigne = "(?s)/\\*.*?\\*/";
        String singleLine = "//[^\\r\\n]*";
        String combainedComments = multipleLigne + "|" + singleLine;

        return code.replaceAll(combainedComments,"");
    }

    /*
        method that translate comments that are not in english to english using Google Translate API
     */
    public static String TranslteToEnglish(String text) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // URL encode the text to be translated
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = String.format("https://translation.googleapis.com/language/translate/v2?key=%s&q=%s&target=en", API_KEY, encodedText);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // Extract translated text from JSON response
            String responseBody = response.body().string();
            return parseTranslatedText(responseBody);
        }
    }

    /*
        method that gets the response of the TranslateToEnglish method
     */
    private static String parseTranslatedText(String jsonResponse) {
        // Parse the JSON response
        JSONObject jsonObject = new JSONObject(jsonResponse);
        return jsonObject
                .getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText");
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
            System.out.println("error while writing in the file : "+e.getMessage());
        }
    }

}