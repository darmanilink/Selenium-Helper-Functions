import net.bytebuddy.pool.TypePool;
import org.openqa.selenium.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.server.handler.interactions.MouseMoveToLocation;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.PageFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class SeleniumUtils {
    //TODO: make a WORKING "wait until clickable" function, the built in one DOES NOT work properly

    static class Tuple<X,Y>{ //its a tuple, get it
        public final X x;
        public final Y y;

        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

    private static ArrayList<Tuple<WebDriverWait, Integer>> WDWCache = new ArrayList<>();

    public static final int TIMEOUT = 30;
    public static final int PAGE_LOAD_TIMEOUT = 20;
    public static final int PAGE_LOAD_TIMEOUT_HALF = PAGE_LOAD_TIMEOUT / 2;
    public static final int PAGE_LOAD_TIMEOUT_SHORT = PAGE_LOAD_TIMEOUT /4;
    public static final int PAGE_WAIT_DEFAULT = 2;

    public static final int MS_OFFSET_CONST = 1000;

    public static void printf(String text, Object... args){ //I'm done typing System.out.print("aadasd" + "aasdas "+ adadasdas" +" asdas"...) every time
        text = text.replace("%i","%d");
        text = String.format(text, args);
        System.out.print(text);
    }

    public static WebDriverWait RequestWaiter(int timeout){
        for (Tuple<WebDriverWait, Integer> wdw : WDWCache){
            if(wdw.y.equals(timeout)){
                if(wdw.x instanceof WebDriverWait) //It may be a tuple and it should be correct but is it? (typeof x not guarenteed to be wdw since we're using generics)
                    return wdw.x;
                else {
                    WDWCache.remove(wdw); //invalid object, remove
                    break; //found cached item was invalid, allocate new wdw to replace it
                }
            }
        }
        WebDriverWait wdw = new WebDriverWait(SeleniumDriver.getDriver(), timeout);//not cached, create new one
        WDWCache.add(new Tuple<WebDriverWait, Integer>(wdw, timeout)); //cache it along with the timeout as an identifier
        return wdw;
    }

    private static void ConditionalWait(WebDriverWait waiter, ExpectedCondition ec){ //easier than Try{}catch(Ex e){}... every single wait
        try {
            waiter.until(ec);
        }catch(Exception ex){
            
        }
    }
    public static void ScrollTo(int y){
        JavascriptExecutor jse = (JavascriptExecutor)SeleniumDriver.getDriver();
        jse.executeScript(String.format("scroll(0, %d);", y));
    }
    public static void ScrollTo(int x, int y){
        JavascriptExecutor jse = (JavascriptExecutor)SeleniumDriver.getDriver();
        jse.executeScript(String.format("scroll(%d, %d);", x,y));
    }

    public static void ScrollBy(int y){
        JavascriptExecutor jse = (JavascriptExecutor)SeleniumDriver.getDriver();
        Long posY = (Long) jse.executeScript("return window.pageYOffset;"); //get offset
        jse.executeScript(String.format("scroll(0, %d);", posY + y)); //offset + desired_distance =  target
    }
    public static void ScrollBy(int x, int y){
        JavascriptExecutor jse = (JavascriptExecutor)SeleniumDriver.getDriver();
        Long posX = (Long) jse.executeScript("return window.pageXOffset;");
        Long posY = (Long) jse.executeScript("return window.pageYOffset;"); //get offset
        jse.executeScript(String.format("scroll(%d, %d);", posX + x, posY + y)); //offset + desired_distance =  target
    }

    public static void WaitUntilGone(WebElement Elem){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.invisibilityOf(Elem));
    }

    public static void WaitUntilGone(String XPATH){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.invisibilityOfElementLocated(By.xpath(XPATH)));
    }

    public static void WaitUntilGone(WebElement Elem, int timeout){
        ConditionalWait(RequestWaiter(timeout),ExpectedConditions.invisibilityOf(Elem));
    }
    public static void WaitUntilGone(String XPATH, int timeout){
        ConditionalWait(RequestWaiter(timeout), ExpectedConditions.invisibilityOfElementLocated(By.xpath((XPATH))));
    }

    public static void ClickElement(WebElement el){
        int WaitFor = PAGE_LOAD_TIMEOUT_HALF;
        final int WaitFor_OffsetMS = WaitFor * MS_OFFSET_CONST;
        long start_time = System.currentTimeMillis();
        for(long i = start_time; i < start_time + WaitFor_OffsetMS; i = System.currentTimeMillis()) {
            try {
                WaitUntilFound(el, PAGE_WAIT_DEFAULT);     //throws exception if timeout
                el.click(); //throws exception if element not found
            }
            catch(Exception e){
                continue;
            }
            return;
        }
        throw new IllegalStateException("Element failed to be clicked.");
    }

    public static void ClickElement(WebElement el, int WaitFor){
        final int WaitFor_OffsetMS = WaitFor * MS_OFFSET_CONST;
        long start_time = System.currentTimeMillis();
        for(long i = start_time; i < start_time + WaitFor_OffsetMS; i = System.currentTimeMillis()) {
            try {
                WaitUntilFound(el,PAGE_WAIT_DEFAULT);
                el.click();
            }
            catch(Exception e){
                continue;
            }
            return;
        }
        throw new IllegalStateException("Element failed to be clicked.");
    }

    public static void ClickElement(String XPATH, int WaitFor){
        final int WaitFor_OffsetMS = WaitFor * MS_OFFSET_CONST;
        long start_time = System.currentTimeMillis();
        for(long i = start_time; i < start_time + WaitFor_OffsetMS; i = System.currentTimeMillis()) {
            try {
                WebElement el = SeleniumDriver.getDriver().findElement(By.xpath((XPATH)));
                WaitUntilFound(el, PAGE_WAIT_DEFAULT);
                el.click(); //throws exception if element not found
            }
            catch(Exception e){
                continue; //exception thrown therefore we loop
            }
            return;
        }
        throw new IllegalStateException("Element failed to be clicked.");
    }

    public static void ClickElement(String XPATH){
        WebElement el = SeleniumDriver.getDriver().findElement(By.xpath((XPATH)));
        int WaitFor = PAGE_LOAD_TIMEOUT_HALF;
        final int WaitFor_OffsetMS = WaitFor * MS_OFFSET_CONST;
        long start_time = System.currentTimeMillis();
        for(long i = start_time; i < start_time + WaitFor_OffsetMS; i = System.currentTimeMillis()) {
            try {
                WaitUntilFound(el, PAGE_WAIT_DEFAULT);     //throws exception if timeout
                el.click(); //throws exception if element not found
            }
            catch(Exception e){
                continue; //exception thrown therefore we loop
            }
            return;
        }
        throw new IllegalStateException("Element failed to be clicked.");
    }

    public static void InputElement(WebElement wel, String input){
        wel.sendKeys(input);
    }

    public static void InputElement(String XPATH, String input){
        SeleniumDriver.getDriver().findElement(By.xpath(XPATH)).sendKeys(input);
    }

    //TODO: needs to be tested
    public static void ClickItemAtIndex(int index, String element){ //make the part of the xpath that increments for every element in the carousel REPLACE
        element = element.replace("REPLACE", "div[%d]".replace("%d", Integer.toString(index)));
        SeleniumDriver.getDriver().findElement(By.xpath(element)).click();

    }

    //TODO: needs to be tested
    public static WebElement GetItemAtIndex(int index, String element){
        element = element.replace("REPLACE", "div[%d]".replace("%d", Integer.toString(index)));
        return SeleniumDriver.getDriver().findElement(By.xpath(element));
    }

    public static void WaitUntilClickable(String XPATH){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.elementToBeClickable(By.xpath(XPATH)))  ;
    }

    public static void WaitUntilClickable(WebElement el){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.elementToBeClickable(el));
    }

    public static void WaitUntilFound(String XPATH){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH)));
    }

    public static void WaitUntilFound(WebElement el){
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.visibilityOf(el));
    }

    public static void WaitUntilFound(String XPATH, int timeout){
        ConditionalWait(RequestWaiter(timeout),ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH)));
    }

    public static void WaitUntilFound(WebElement el, int timeout){
        ConditionalWait(RequestWaiter(timeout), ExpectedConditions.visibilityOf(el));
    }

    public static boolean VerifyDictionaryIsValid(String[] dictionary){
        boolean IsElementSubstringOfAnotherElement = false;
        for(int i = 0; i < dictionary.length; i++){ //iterating over every single combonation
            for(int j = i; j < dictionary.length; j++)
                if(dictionary[i].toLowerCase().contains(dictionary[j].toLowerCase()) || dictionary[j].toLowerCase().contains(dictionary[i].toLowerCase())) // d[i] ⫅ d[j] | d[j] ⫅ d[i],  since (app ⫅ apple) but !(apple ⫅ app)
                    IsElementSubstringOfAnotherElement = true;
        }
        return !IsElementSubstringOfAnotherElement; //If this is true then the dictionary is invalid so not it
    }

    public static String MatchPartialString(String input, String[] dictionary){
        boolean DictionaryIsValid = VerifyDictionaryIsValid(dictionary);
        if(!DictionaryIsValid) //dictonary is considered valid if no other element can be found using the full name of an element
            for (String s : dictionary)
                if(s.toLowerCase().equals(input.toLowerCase()))
                    return s; //input may not be properly cased as this was created so you would not have to worry about that
                else
                    return "bad input";

        boolean success = false;
        for(int i = 0; i <  dictionary.length; i++){
            if(input.toLowerCase().equals(dictionary[i].toLowerCase().substring(0, input.length()))){
                input = dictionary[i]; //switch statement may be cased properly but input might not
                success = true;
                break;
            }
        }
        return success ? input : "bad input";
    }

    public static int MatchPartialString(String input, String[] dictionary, boolean UsingInt){ //overloaded to return an int instead of a string if you want to not switch case strings
        if(!UsingInt)
            return -1;

        boolean DictionaryIsValid = VerifyDictionaryIsValid(dictionary);
        if(!DictionaryIsValid) {
            for (int i = 0; i < dictionary.length; i++) {
                if (dictionary[i].toLowerCase().equals(input.toLowerCase()))
                    return i;
            }
            return -1;
        }

        for(int i = 0; i <  dictionary.length; i++){
            if(input.toLowerCase().equals(dictionary[i].toLowerCase().substring(0, input.length()))){
                return i; //we found the index of what we're looking for
            }
        }
        return -1;  //not found
    }

    public static void WaitUntilFound_Or(WebElement... w1){
        ArrayList<ExpectedCondition>arr_conds = new ArrayList<ExpectedCondition>();
        for(WebElement w : w1)
            arr_conds.add(ExpectedConditions.visibilityOf(w));
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT),ExpectedConditions.or(arr_conds.toArray(new ExpectedCondition[arr_conds.size()])));
    }

    public static void WaitUntilFound_And(WebElement... w1){
        ArrayList<ExpectedCondition>arr_conds = new ArrayList<ExpectedCondition>();
        for(WebElement w : w1)
            arr_conds.add(ExpectedConditions.visibilityOf(w));
        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT), ExpectedConditions.and(arr_conds.toArray(new ExpectedCondition[arr_conds.size()])));
    }

    public static void WaitUntilFound_Xor(WebElement... w1){ //or except will throw an exception if any other element is found
        ArrayList<ExpectedCondition>arr_conds = new ArrayList<ExpectedCondition>();
        for(WebElement w : w1)
            arr_conds.add(ExpectedConditions.visibilityOf(w));

        ConditionalWait(RequestWaiter(PAGE_LOAD_TIMEOUT_SHORT),ExpectedConditions.or(arr_conds.toArray(new ExpectedCondition[arr_conds.size()])));

        boolean ElementAlreadyFound = false;
        for(WebElement w : w1) {
            if(w.isDisplayed() && w.isEnabled()) { //element is considered found if this condition has been met
                if(ElementAlreadyFound) {  //has an element already been found?
                    throw new IllegalStateException("found another entry where 1 should have been present"); //crash and burn/raise false
                }
                ElementAlreadyFound = true; //gets raised on your first time going through as any next time leads to a brick wall
            }
        }
    }
}



