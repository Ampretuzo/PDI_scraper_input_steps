###### Adding new scrapers on top of base scraper
Assuming you want to create an `xxx` kind of scraper:
- create package `ge.hamamlo.pentaho.di.trans.steps.scraper.xxx` in _specific-scrapers_ module.
- create class  `ScraperXXX` in the newly created package, implementing `Scraper` interface.
- specify the fields that will be produced by the new step, via `fields` method. Base step calls that method to find out which fields to create. `FieldDef` class makes this very straightforward.
- implement custom scraping logic in `ScraperXXX::scrapeUrl` method, you should make good use of _JSoup_ library while doing so, which provides standard DOM API to manipulate webpage content. Call `ScraperBase.ScraperOutput::yield` method whenever new scraping result is available - to push results to output. 
- create class `ScraperXXXMeta` which extends `ScraperBaseMeta`.
- override default ctor in `ScraperXXXMeta`, after calling `super` ctor, register custom scraper using `setScraperClass` method: ```setScraperClass(ScraperXXX.class)```.
- annotate `ScraperXXXMeta` with `@Step` annotation, that enables PDI to detect custom step when scanning for plugins and picks up step information from annotation fields.
- populate `id`, `name`, `categoryDescription` and `image` fields of `@Step` with desired values.

After completing the steps, rebuild _specific-scrapers_ module using ```mvn clean install -DskipTests=true``` command and drop the generated jar file from _specific-scrapers_ module into PDI plugins location.

Please see `ge.hamamlo.pentaho.di.trans.steps.scraper.ec` package for concrete example of above described steps.


###### Separation from Pentaho Plugin Architecture
The project is divided into two modules: _pdi-scraper-step-base_ and _specific-scrapers_.

The former provides connection with _Pentaho_ plugin architecture - it is called from Pentaho code itself. The reason why I separated it from actual scrapers is that it would be time consuming to re-write (copy) this code for each new type of scraper that would have to be implemented. It is quite simple and is configured in runtime by `Scraper` class.

_specific-scrapers_ module is where all the actual scraping logic is located. It does not directly depend on Pentaho. This separation makes it easier to launch scraping without having to worry about all the Pentaho plugin interfaces - just create an unit test for `ScraperXXX` class and call the most important method: `scrapeUrl`:
```java
@Test
public void scrapeUrl() throws Exception {
    // Initialize desired class using no-arg ctor
    SmergersScraper smergersScraper = new SmergersScraper();
    // Call the scrapeUrl method, you can debug this method from here
    smergersScraper.scrapeUrl(
            "https://www.smergers.com/businesses/businesses-for-sale/s0/c0/t2/?deal_size_gte=5000000&page=1&deal_size_lte=1000000000",
            null,   // it's safe to pass null if logging is not used in the class
            output -> { // just print the result to system.out
                if (output == null) {
                    System.out.println("done!");
                    return;
                }
                System.out.println("Result: " + Arrays.toString(output) );
            }
    );
}
```

This will spare **a lot** of time since you won't have to redeploy the plugin in _Kettle_ every time you make simple changes.

Simply run the test in debug mode with breakpoints enabled to see what it does in runtime. That's all you have to do to debug it - junit dependency is already included in the project. Please see concrete example in `SmergersScraperTest`.

_NOTE_: this is not a _real_ unit test, it is just a convenience to launch desired method.

###### Building and adding new dependency libraries

To build the project simply run: `mvn clean install -DskipTests=true`. Find distribution in `specific-scrapers/target/drop-in-plugins.zip` file, it should be ready to be dropped in Kettle plugins folder - it will contain all the dependent libraries.

To add new libraries to the project, you can add it as **provided** dependency in _specific-scrapers_ _pom.xml_. Make sure the new dependencies are _provided scope_, since the build is configured to pick them up and put them in libs folder automatically.

