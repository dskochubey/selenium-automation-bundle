package com.sysgears.seleniumbundle.core.pagemodel

import com.codeborne.selenide.*
import com.sysgears.seleniumbundle.core.conf.Config
import com.sysgears.seleniumbundle.core.data.DataLoader
import com.sysgears.seleniumbundle.core.pagemodel.annotations.StaticElement
import com.sysgears.seleniumbundle.core.uicomparison.UIComparison
import groovy.util.logging.Slf4j
import org.openqa.selenium.Keys

import java.lang.reflect.Field

import static com.codeborne.selenide.Selenide.$
import static com.codeborne.selenide.WebDriverRunner.url
import static org.testng.Assert.assertTrue

/**
 * Provides common methods for page model objects.
 */
@Slf4j
abstract class AbstractPage<T> implements UIComparison {

    /**
     * Instance of Config.
     */
    protected Config conf = Config.instance

    /**
     * Name of the os.
     */
    protected String os

    /**
     * Name of the browser.
     */
    protected String browser

    /**
     * URL of the page.
     */
    protected String url

    /**
     * Opens web page.
     *
     * @return page object
     */
    T open() {
        Selenide.open(url)
        assertUrl()
        (T) this
    }

    /**
     * Asserts whether current URL matches the page URL.
     */
    void assertUrl() {
        assertTrue(url().contains(url), "Given url is: [$url], real url is: [${url()}]")
    }

    /**
     * Clears a page text input.
     *
     * @param element web element to be cleared
     */
    void clearTextInput(SelenideElement element) {
        element.getValue()?.length()?.times {
            element.sendKeys(Keys.BACK_SPACE)
        }
    }

    /**
     * Clears a set of text inputs.
     *
     * @param list web element list to be cleared
     */
    void clearTextInputs(List<SelenideElement> list) {
        list.each {
            clearTextInput(it)
        }
    }

    /**
     * Sets CSS style visibility to hidden in order to hide an element.
     *
     * @param elements element which has to be hidden
     *
     * @return page object
     */
    T hideElement(SelenideElement... elements) {
        elements.each {
            Selenide.executeJavaScript("arguments[0].style.visibility='hidden'", it)
        }
        (T) this
    }

    /**
     * Sets CSS style visibility to hidden in order to hide a collection of elements.
     *
     * @param elementsCollections collection of elements which has to be hidden
     *
     * @return page object
     */
    T hideElement(ElementsCollection... elementsCollections) {
        elementsCollections.each { collection ->
            collection.each {
                hideElement(it)
            }
        }
        (T) this
    }

    /**
     * Hides elements on given page for UI comparison.
     *
     * @param page class of a page yo hide elements on
     *
     * @return page object
     */
    T hideElementsFromFile(Class page) {
        getElementsToHideFromFile(page).each {
            hideElement(it)
        }
        (T) this
    }

    /**
     * Hides elements of current page for UI comparison.
     *
     * @return page object
     */
    T hideElementsFromFile() {
        hideElementsFromFile(this.class)
        (T) this
    }

    /**
     * Checks if the static elements of the page object are loaded and exist in DOM.
     *
     * @return page object
     */
    T waitForPageToLoadElements() {
        Field[] fields = this.getClass().getDeclaredFields()

        fields.each { field ->
            StaticElement annotation = field.getAnnotation(StaticElement.class)
            if (annotation && field.getType() in [SelenideElement.class, ElementsCollection.class]) {
                field.setAccessible(true)

                try {
                    def element = field.get(this)
                    if (field.getType() == SelenideElement.class) {
                        log.trace("Checking if Selenide Element exists: $element")
                        (element as SelenideElement).should(Condition.exist)
                    } else {
                        log.trace("Checking if Elements Collection exists: $element")
                        (element as ElementsCollection).shouldHave(CollectionCondition.sizeGreaterThan(0))
                    }
                } catch (IllegalAccessException e) {
                    log.error("Unable to get element: ${field.name}", e)
                    throw new IllegalAccessException("Unable to get element: ${field.name}")
                }
            }
        }
        log.info("${this.getClass().getSimpleName()} has been loaded")
        (T) this
    }

    /**
     * Gets list of elements to hide on given page. Path to file with element locators saved in configuration file.
     *
     * @param classOfPage page on which the elements have to be hidden
     *
     * @return list of elements to hide
     */
    private List getElementsToHideFromFile(Class classOfPage) {
        def data = DataLoader.readMapFromYml(conf.ui.hiddenElements)
        def className = classOfPage.name - "com.sysgears.seleniumbundle.pagemodel."
        def selectorsList = (data as ConfigObject).flatten()."$className"

        if (!selectorsList) {
            log.error("No elements to hide on [$className].")
            throw new IllegalArgumentException("No elements to hide on [$className].")
        }

        selectorsList.collect { $(it as String) }
    }
}