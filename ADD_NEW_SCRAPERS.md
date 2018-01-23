###### Adding new scrapers on top of base scraper
Assuming you want to create an `xxx` kind of scraper:
- create package `ge.hamamlo.pentaho.di.trans.steps.scraper.xxx` in _specific-scrapers_ module.
- create class  `ScraperXXX` in the newly created package, implementing `Scraper` interface.
- specify the fields that will be produced by the new step, via `fields` method. Base step calls that method to find out which fields to create. `FieldDef` class makes this very straightforward.
- implement custom scraping logic in `ScraperXXX::scrapeUrl` method, you should make good use of _JSoup_ library while doing so, which provides stanrard DOM API to manipulate webpage content. Call `ScraperBase.ScraperOutput::yield` method to push results to output. 
- create class `ScraperXXXMeta` which extends `ScraperBaseMeta`.
- override default ctor in `ScraperXXXMeta`, after calling `super` ctor, register custom scraper using `setScraperClass` method: ```setScraperClass(ScraperXXX.class)```.
- annotate `ScraperXXXMeta` with `@Step` annotation, that enables PDI to detect custom step when scanning for plugins and picks up step information from annotation fields.
- populate `id`, `name`, `categoryDescription` and `image` fields of `@Step` with desired values.

After completing the steps, rebuild _specific-scrapers_ module using ```mvn clean package``` command and drop the generated jar file from _specific-scrapers_ module into PDI plugins location.

Please see `ge.hamamlo.pentaho.di.trans.steps.scraper.ec` package for concrete example of above described steps.
