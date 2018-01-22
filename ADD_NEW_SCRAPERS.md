###### Adding new scrapers on top of base scraper
Assuming you want to create an `xxx` kind of scraper:
- create package `ge.hamamlo.pentaho.di.trans.steps.scraper.xxx` in _specific-scrapers_ module.
- create class  `ScraperXXX` in the newly created package, implementing `Scraper` interface.
- implement custom scraping logic in `ScraperXXX::scrapeUrl` method.
- create class `ScraperXXXMeta` which extends `ScraperBaseMeta`.
- override default ctor in `ScraperXXXMeta`, after calling `super` ctor, register custom scraper using `setScraperClass` method: ```setScraperClass(ScraperXXX.class)```.
- annotate `ScraperXXXMeta` with `@Step` annotation, that enables PDI to detect custom step when scanning for plugins and picks up step information from annotation fields.
- populate `id`, `name`, `categoryDescription` and `image` fields of `@Step` with desired values.

After completing the steps, rebuild _specific-scrapers_ module and drop the jar file in PDI plugins location.
